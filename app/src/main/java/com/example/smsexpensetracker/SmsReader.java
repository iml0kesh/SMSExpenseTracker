package com.example.smsexpensetracker;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsReader {

    private final ContentResolver contentResolver;

    private final List<String> keywords = Arrays.asList(
            "BANK", "BNK", "PAY", "WALLET", "CREDIT", "DEBIT", "UPI", "CARD", "TXN",
            "ICICI", "HDFC", "SBI", "AXIS", "KOTAK", "PAYTM", "PHONEPE", "CRED", "AMEX", "CITI", "PNB"
    );

    public SmsReader(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public List<SmsComponent> readSmsFromSenders(String[] senders) {
        List<SmsComponent> smsList = new ArrayList<>();
        if (senders == null || senders.length == 0) return smsList;

        StringBuilder selection = new StringBuilder();
        for (int i = 0; i < senders.length; i++) {
            selection.append(Telephony.Sms.ADDRESS + " = ?");
            if (i < senders.length - 1) selection.append(" OR ");
        }

        try (Cursor cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, selection.toString(), senders, Telephony.Sms.DATE + " DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long dateLong = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    smsList.add(new SmsComponent(address, body, new Date(dateLong), 0.0, "UNPROCESSED"));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SmsReader", "Error reading SMS from cursor", e);
        }
        return smsList;
    }

    public List<SenderInfo> getAllUniqueSendersWithLatestMessage() {
        Map<String, String> senderMap = new HashMap<>();

        // First, get all potential financial senders
        try (Cursor cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, new String[]{Telephony.Sms.ADDRESS}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    if (address != null && address.matches(".*[a-zA-Z]+.*")) {
                        String upperAddress = address.toUpperCase();
                        for (String keyword : keywords) {
                            if (upperAddress.contains(keyword)) {
                                if (!senderMap.containsKey(address)) {
                                    senderMap.put(address, ""); 
                                }
                                break; 
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SmsReader", "Error getting unique senders", e);
        }

        // Now, for each unique sender, get their most recent message
        for (String senderAddress : senderMap.keySet()) {
            try (Cursor messageCursor = contentResolver.query(Telephony.Sms.CONTENT_URI, 
                                                             new String[]{Telephony.Sms.BODY}, 
                                                             Telephony.Sms.ADDRESS + " = ?", 
                                                             new String[]{senderAddress}, 
                                                             Telephony.Sms.DATE + " DESC LIMIT 1")) {
                if (messageCursor != null && messageCursor.moveToFirst()) {
                    String body = messageCursor.getString(messageCursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    senderMap.put(senderAddress, body);
                }
            }
        }
        
        List<SenderInfo> resultList = new ArrayList<>();
        for(Map.Entry<String, String> entry : senderMap.entrySet()){
            resultList.add(new SenderInfo(entry.getKey(), entry.getValue()));
        }

        Collections.sort(resultList, (o1, o2) -> o1.getAddress().compareTo(o2.getAddress()));
        return resultList;
    }
}
