package com.example.smsexpensetracker.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.smsexpensetracker.models.Transaction;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ExportHelper {

    public static void export(Context ctx, List<Transaction> list) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File dir = new File(ctx.getCacheDir(), "exports");
                dir.mkdirs();
                String name = "expenses_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date()) + ".csv";
                File file = new File(dir, name);

                FileWriter fw = new FileWriter(file);
                fw.append("Date,Sender,Type,Amount,Category,Merchant\n");

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
                for (Transaction t : list) {
                    fw.append(sdf.format(new Date(t.dateMillis))).append(",")
                      .append(csv(t.sender)).append(",")
                      .append(csv(t.type)).append(",")
                      .append(String.valueOf(t.amount)).append(",")
                      .append(csv(t.category)).append(",")
                      .append(csv(t.merchant)).append("\n");
                }
                fw.flush();
                fw.close();

                Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/csv");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.putExtra(Intent.EXTRA_SUBJECT, "Expense Report");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(Intent.createChooser(intent, "Export via…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(ctx, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private static String csv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
