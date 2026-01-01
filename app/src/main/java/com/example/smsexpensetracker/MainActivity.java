package com.example.smsexpensetracker;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    TextView tvTotalDebit, tvTotalCredit, tvBalance;

    private ArrayList<String> smsList = new ArrayList<>();

    private ArrayList<SmsComponent> smsComponents = new ArrayList<>();
    private ArrayList<Double> allAmount = new ArrayList<>();

    private ArrayList<Date> AmountDate = new ArrayList<>();
    private ListView listViewOfSMS;
    private ListView listViewOfAmount;
    private static final int READ_SMS_PERMISSION_CODE = 1;

    private static double totalDebitAmount = 0.0;
    private  static double totalCreditAmount = 0.0;

    SmsProcessor processor = new SmsProcessor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔹 Initialize views FIRST
        tvTotalDebit = findViewById(R.id.tvTotalDebit);
        tvTotalCredit = findViewById(R.id.tvTotalCredit);
        tvBalance = findViewById(R.id.tvBalance);

        listViewOfSMS = findViewById(R.id.listViewOfSMS);

        ArrayAdapter<SmsComponent> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsComponents);
        listViewOfSMS.setAdapter(adapter);

        // Permission check
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.READ_SMS},
                    READ_SMS_PERMISSION_CODE
            );
        } else {
            readSms(new String[]{"AD-ICICIT", "JD-ICICIT", "JM-ICICIT", "JX-ICICIT"});
            updateTotals();
        }
    }

    private void updateTotals() {
        double totalDebit = processor.getTotalDebitAmount();
        double totalCredit = processor.getTotalCreditAmount();
        double balance = totalCredit - totalDebit;

        tvTotalDebit.setText("Total Spent: ₹" + totalDebit);
        tvTotalCredit.setText("Total Received: ₹" + totalCredit);
        tvBalance.setText("Balance: ₹" + balance);
    }



    // This function will get the messages only from a specific number.
    @SuppressLint("SetTextI18n")
//    private void readSms(String[] possibleSenders) {
//
//        smsComponents.clear();
//        processor.resetTotals();
//
//        ContentResolver contentResolver = getContentResolver();
//
//        double totalDebit = processor.getTotalDebitAmount();
//        double totalCredit = processor.getTotalCreditAmount();
//        double balance = totalCredit - totalDebit;
//
//        StringBuilder selectionBuilder = new StringBuilder();
//        String[] selectionArgs = new String[possibleSenders.length];
//
//        selectionBuilder.append(Telephony.Sms.ADDRESS + " IN (");
//        for(int i=0; i<possibleSenders.length; i++){
//            selectionBuilder.append("?");
//            selectionArgs[i] = possibleSenders[i];
//            if(i < possibleSenders.length - 1) {
//                selectionBuilder.append(", ");
//            }
//        }
//
//        selectionBuilder.append(")");
//
//        String selection = selectionBuilder.toString();
//
//        Cursor cursor = contentResolver.query(
//                Telephony.Sms.CONTENT_URI,
//                null,
//                selection,
//                selectionArgs,
//                null);
//
//        if (cursor != null && cursor.moveToFirst()) {
//            do {
//                // This will get the sender.
//                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
//
//                // This will get what the sender has sent.
//                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
//
//                Date smsDate = new Date(cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)));
////                AmountDate.add(smsDate);
//
//                SmsProcessor.ProcessResult result = processor.processSms(body);
//                String Type = result.type == SmsProcessor.TransactionType.DEBIT ? "DEBIT" : "CREDIT";
//
//                SmsComponent smsComponent = new SmsComponent(address, body, smsDate, result.amount, Type);
//                smsComponents.add(smsComponent);
//
////                smsList.add("Sender: " + address + "\nMessage: " + body + "\nAmount: " + processSms(body));
//
//            } while (cursor.moveToNext());
//        }
//
//        tvTotalDebit.setText("Total Spent: ₹" + totalDebit);
//        tvTotalCredit.setText("Total Received: ₹" + totalCredit);
//        tvBalance.setText("Balance: ₹" + balance);
//
//        if (cursor != null) {
//            cursor.close();
//        }
//        updateTotals();
//    }

    private void readSms(String[] possibleSenders) {
        ContentResolver contentResolver = getContentResolver();

        // 1. Clear existing data to avoid duplicates
        smsComponents.clear();

        String selection = Telephony.Sms.ADDRESS + " LIKE ?"; // <--- Defined here
        String[] selectionArgs = new String[]{"%ICICI%"};
        String sortOrder = Telephony.Sms.DATE + " DESC"; // <--- Get newest first

        // 3. Query the provider
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                selection,      // Pass the variable
                selectionArgs,  // Pass the variable
                sortOrder       // Added sort order here
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                long dateLong = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                Date smsDate = new Date(dateLong);

                SmsProcessor.ProcessResult result = processor.processSms(body);
                String type = (result.type == SmsProcessor.TransactionType.DEBIT) ? "DEBIT" : "CREDIT";

                SmsComponent smsComponent = new SmsComponent(address, body, smsDate, result.amount, type);
                smsComponents.add(smsComponent);

            } while (cursor.moveToNext());
            cursor.close();
        }

        // 4. Update UI
        updateTotals();
        if (listViewOfSMS.getAdapter() != null) {
            ((ArrayAdapter<?>) listViewOfSMS.getAdapter()).notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == READ_SMS_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            readSms(new String[]{"AD-ICICIT", "JD-ICICIT", "JM-ICICIT", "JX-ICICIT", "AD-ICICIT-S", "AX-ICICIT-S", ""});
            updateTotals();

            ((ArrayAdapter<?>) listViewOfSMS.getAdapter()).notifyDataSetChanged();
        }
    }

}