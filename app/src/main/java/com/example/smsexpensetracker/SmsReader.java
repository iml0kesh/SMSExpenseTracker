package com.example.smsexpensetracker;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmsReader {

    private final ContentResolver contentResolver;

    // A more refined list of keywords to identify transactional messages.
    private final List<String> keywords = Arrays.asList(
            // General financial terms
            "BANK", "BNK", "PAY", "WALLET", "CREDIT", "DEBIT", "UPI", "CARD", "TXN",
            // Specific bank and service names
            "ICICI", "HDFC", "SBI", "AXIS", "KOTAK", "PAYTM", "PHONEPE", "CRED", "AMEX", "CITI", "PNB"
    );

    public SmsReader(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public List<SmsComponent> readSmsFromSenders(String[] senders) {
        List<SmsComponent> smsList = new ArrayList<>();

        if (senders == null || senders.length == 0) {
            return smsList;
        }

        // Use exact matching for accuracy
        StringBuilder selection = new StringBuilder();
        for (int i = 0; i < senders.length; i++) {
            selection.append(Telephony.Sms.ADDRESS + " = ?");
            if (i < senders.length - 1) {
                selection.append(" OR ");
            }
        }

        String sortOrder = Telephony.Sms.DATE + " DESC";

        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                selection.toString(),
                senders, // Use the senders array directly as selectionArgs
                sortOrder
        );

        if (cursor != null && cursor.moveToFirst()) {
            try {
                do {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long dateLong = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    Date smsDate = new Date(dateLong);

                    smsList.add(new SmsComponent(address, body, smsDate, 0.0, "UNPROCESSED"));

                } while (cursor.moveToNext());
            } catch (Exception e) {
                Log.e("SmsReader", "Error reading SMS from cursor", e);
            } finally {
                cursor.close();
            }
        }

        return smsList;
    }

    /**
     * Reads all SMS and returns a list of unique sender IDs that likely belong to financial institutions.
     *
     * @return A sorted list of potential bank/financial sender IDs.
     */
    public List<String> getAllUniqueSenders() {
        Set<String> senderSet = new HashSet<>();

        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            try {
                do {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));

                    if (address != null && address.matches(".*[a-zA-Z]+.*")) {
                        String upperAddress = address.toUpperCase();
                        for (String keyword : keywords) {
                            if (upperAddress.contains(keyword)) {
                                senderSet.add(address);
                                break; // Found a match, no need to check other keywords for this sender
                            }
                        }
                    }
                } while (cursor.moveToNext());
            } catch (Exception e) {
                Log.e("SmsReader", "Error reading unique senders", e);
            } finally {
                cursor.close();
            }
        }

        List<String> sortedSenders = new ArrayList<>(senderSet);
        Collections.sort(sortedSenders);

        return sortedSenders;
    }
}
