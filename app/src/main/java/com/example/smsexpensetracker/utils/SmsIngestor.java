package com.example.smsexpensetracker.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import com.example.smsexpensetracker.db.TransactionDao;
import com.example.smsexpensetracker.models.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmsIngestor {

    private static final String TAG = "SmsIngestor";

    /**
     * TRAI TCCCPR 2018 — entity codes assigned to Principal Entities.
     * Used to detect financial senders from sender ID (after stripping XY- prefix).
     * This list covers entity codes, not just display names.
     */
    private static final String[] FINANCIAL_ENTITY_CODES = {
        // Major banks — entity codes
        "HDFCBK","HDFCBL","ICICIB","ICICIT","SBIINB","SBIPSG","AXISBK","KOTAKB",
        "PNBSMS","PNJBNK","BOBTXN","BARODB","CANBNK","UNIONB","IDBIBK",
        "INDBNK","YESBNK","YESBK","FEDBK","RBLBNK","RATBNK","AUBNKL",
        "AUSMFB","EQTSBN","UJJIVN","SCBNKI","HSBCIN","AMEXIN","CITIBK",
        "BAJAJF","BAJFIN","SBICARD","SBICRD","HDFCCC",
        // Payment apps
        "PAYTMB","PAYTM","PHPEBN","PHPBNK","GPAYBN","CREDBN",
        // Generic financial keywords that appear in entity codes
        "BANK","BNK","NEFT","IMPS","RTGS","DEBIT","CREDIT",
        "TRANS","UPI","CARD","WALLET","FINCR","FINCO",
        // Broad fallback keywords (catches non-standard headers)
        "PAY","TXN","STMT","ALRT","INFOM"
    };

    // ── Ingest ────────────────────────────────────────────────────────────

    public static int ingest(ContentResolver cr, TransactionDao dao, Set<String> senders) {
        if (senders == null || senders.isEmpty()) return 0;

        String[] args = senders.toArray(new String[0]);
        StringBuilder sel = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sel.append(Telephony.Sms.ADDRESS).append("=?");
            if (i < args.length - 1) sel.append(" OR ");
        }

        List<Transaction> batch = new ArrayList<>();
        try (Cursor c = cr.query(
                Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms._ID, Telephony.Sms.ADDRESS,
                             Telephony.Sms.BODY, Telephony.Sms.DATE},
                sel.toString(), args, Telephony.Sms.DATE + " DESC"
        )) {
            if (c == null) return 0;
            while (c.moveToNext()) {
                String smsId = c.getString(0);
                String addr  = c.getString(1);
                String body  = c.getString(2);
                long   date  = c.getLong(3);
                if (dao.existsBySmsId(smsId) > 0) continue;
                SmsParser.Result r = SmsParser.parse(body);
                if (r.amount <= 0) continue;
                batch.add(new Transaction(smsId, addr, body,
                        r.amount, r.type, Tagger.tag(body, r.merchant), r.merchant, date));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error ingesting SMS", e);
        }
        if (!batch.isEmpty()) dao.insertAll(batch);
        Log.d(TAG, "Ingested " + batch.size() + " new transactions");
        return batch.size();
    }

    // ── Sender discovery ──────────────────────────────────────────────────

    /** Only senders that match known financial entity codes (TRAI-aware) */
    public static List<String[]> getFinancialSenders(ContentResolver cr) {
        return getSendersInternal(cr, true);
    }

    /** Every unique alphanumeric sender in the inbox */
    public static List<String[]> getAllSenders(ContentResolver cr) {
        return getSendersInternal(cr, false);
    }

    private static List<String[]> getSendersInternal(ContentResolver cr, boolean financialOnly) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        try (Cursor c = cr.query(
                Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY},
                null, null, Telephony.Sms.DATE + " DESC"
        )) {
            if (c == null) return new ArrayList<>();
            while (c.moveToNext()) {
                String addr = c.getString(0);
                String body = c.getString(1);
                if (addr == null || addr.trim().isEmpty()) continue;
                // Only alphanumeric senders (TRAI short codes, not personal numbers)
                if (!addr.matches(".*[a-zA-Z].*")) continue;
                if (financialOnly && !isFinancialSender(addr)) continue;
                if (!map.containsKey(addr)) map.put(addr, body != null ? body : "");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading senders", e);
        }
        List<String[]> result = new ArrayList<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            result.add(new String[]{e.getKey(), e.getValue()});
        }
        return result;
    }

    /**
     * Checks if a sender is a financial sender using TRAI entity code matching.
     * Strips the XY- carrier prefix first, then checks the entity code.
     */
    private static boolean isFinancialSender(String sender) {
        // Extract entity code (strip XY- prefix if present)
        String entity = sender.toUpperCase();
        if (entity.length() >= 4 && entity.charAt(2) == '-') {
            entity = entity.substring(3);
        }
        for (String code : FINANCIAL_ENTITY_CODES) {
            if (entity.contains(code)) return true;
        }
        return false;
    }
}
