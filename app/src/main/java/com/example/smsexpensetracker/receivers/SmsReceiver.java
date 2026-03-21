package com.example.smsexpensetracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.example.smsexpensetracker.db.AppDatabase;
import com.example.smsexpensetracker.models.Transaction;
import com.example.smsexpensetracker.utils.Prefs;
import com.example.smsexpensetracker.utils.SmsParser;
import com.example.smsexpensetracker.utils.Tagger;

import java.util.Set;
import java.util.concurrent.Executors;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;

        Bundle b = intent.getExtras();
        if (b == null) return;

        Object[] pdus = (Object[]) b.get("pdus");
        String format = b.getString("format");
        if (pdus == null) return;

        StringBuilder body = new StringBuilder();
        String sender = null;
        long ts = System.currentTimeMillis();

        for (Object pdu : pdus) {
            SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu, format);
            if (msg != null) {
                if (sender == null) sender = msg.getDisplayOriginatingAddress();
                body.append(msg.getMessageBody());
                ts = msg.getTimestampMillis();
            }
        }

        if (sender == null || body.length() == 0) return;

        // Only process if sender is tracked
        Set<String> tracked = Prefs.getSenders(ctx);
        boolean found = false;
        for (String s : tracked) {
            if (s.equalsIgnoreCase(sender)) { found = true; break; }
        }
        if (!found) return;

        SmsParser.Result r = SmsParser.parse(body.toString());
        if (r.amount <= 0) return;

        String category = Tagger.tag(body.toString(), r.merchant);
        String smsId = "live_" + sender + "_" + ts;

        Transaction t = new Transaction(smsId, sender, body.toString(),
                r.amount, r.type, category, r.merchant, ts);

        final Context appCtx = ctx.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() ->
            AppDatabase.get(appCtx).dao().insert(t)
        );
    }
}
