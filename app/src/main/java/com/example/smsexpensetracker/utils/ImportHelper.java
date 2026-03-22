package com.example.smsexpensetracker.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.smsexpensetracker.db.AppDatabase;
import com.example.smsexpensetracker.db.TransactionDao;
import com.example.smsexpensetracker.models.Transaction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ImportHelper {

    public interface ImportCallback {
        void onResult(int count);
    }

    public static void importCsv(Context ctx, Uri uri, ImportCallback callback) {
        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            performImport(ctx, is, callback);
        } catch (Exception e) {
            Log.e("ImportHelper", "Failed to open stream", e);
            callback.onResult(-1);
        }
    }

    public static void importFromAssets(Context ctx, String fileName, ImportCallback callback) {
        try {
            InputStream is = ctx.getAssets().open(fileName);
            performImport(ctx, is, callback);
        } catch (Exception e) {
            Log.e("ImportHelper", "Failed to open asset", e);
            callback.onResult(-1);
        }
    }

    private static void performImport(Context ctx, InputStream is, ImportCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int count = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                String header = reader.readLine(); // Skip header
                String line;
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
                TransactionDao dao = AppDatabase.get(ctx).dao();
                List<Transaction> batch = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    try {
                        String[] parts = parseCsvLine(line);
                        if (parts.length < 6) continue;

                        long date = sdf.parse(parts[0]).getTime();
                        String sender = parts[1];
                        String type = parts[2];
                        double amount = Double.parseDouble(parts[3]);
                        String category = parts[4];
                        String merchant = parts[5];

                        // Generate a unique SMS ID for imported records
                        String smsId = "imp_" + date + "_" + (int)(amount * 100);
                        
                        if (dao.existsBySmsId(smsId) == 0) {
                            batch.add(new Transaction(smsId, sender, "Imported Data",
                                    amount, type, category, merchant, date));
                            count++;
                        }
                    } catch (Exception e) {
                        Log.e("ImportHelper", "Error parsing line: " + line, e);
                    }
                }

                if (!batch.isEmpty()) {
                    dao.insertAll(batch);
                }

            } catch (Exception e) {
                Log.e("ImportHelper", "Import failed", e);
                count = -1;
            }

            int finalCount = count;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onResult(finalCount));
        });
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }
}
