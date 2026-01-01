package com.example.smsexpensetracker;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    // 1. Declare variables at the top (don't use findViewById here!)
    TextView tvTotalDebit, tvTotalCredit, tvBalance;
    TextView tvToday, tvWeek, tvMonth, tvYear; // New grouping views
    private final ArrayList<SmsComponent> smsComponents = new ArrayList<>();
    private ListView listViewOfSMS;
    private static final int READ_SMS_PERMISSION_CODE = 1;

    SmsProcessor processor = new SmsProcessor();
    FloatingActionButton btnScrollBottom; // Just declare it


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 2. Initialize views AFTER setContentView
//        tvTotalDebit = findViewById(R.id.tvTotalDebit);
//        tvTotalCredit = findViewById(R.id.tvTotalCredit);
        // tvBalance = findViewById(R.id.tvBalance);

        // Make sure these IDs exist in your XML
        tvToday = findViewById(R.id.tvToday);
        tvWeek = findViewById(R.id.tvWeek);
        tvMonth = findViewById(R.id.tvMonth);
        tvYear = findViewById(R.id.tvYear);

        listViewOfSMS = findViewById(R.id.listViewOfSMS);
//        btnScrollBottom = findViewById(R.id.btnScrollBottom);

        SmsAdapter adapter = new SmsAdapter(this, smsComponents);
        listViewOfSMS.setAdapter(adapter);


        // Permission check
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            // Now it's safe to call readSms because tvToday etc. are not null anymore
            readSms();
        }

        // 3. Set the listener inside onCreate
//        btnScrollBottom.setOnClickListener(v -> {
//            if (listViewOfSMS.getAdapter() != null) {
//                int lastItem = listViewOfSMS.getAdapter().getCount() - 1;
//                if (lastItem >= 0) {
//                    listViewOfSMS.smoothScrollToPosition(lastItem);
//                }
//            }
//        });
    }

    @SuppressLint("DefaultLocale")
    private void updateTotals() {
        // Grand Totals
        double totalDebit = processor.getTotalDebitAmount();
        double totalCredit = processor.getTotalCreditAmount();
        // double balance = totalCredit - totalDebit;

//        tvTotalDebit.setText(String.format("%.2f", totalDebit));
//        tvTotalCredit.setText(String.format("%.2f", totalCredit));
        // tvBalance.setText("Balance: ₹" + balance);

        // Calculate Grouped Totals (Today, Week, Month)
        calculateGroupedTotals();
    }

    private void readSms() {
        ContentResolver contentResolver = getContentResolver();
        smsComponents.clear();
        processor.resetTotals(); // Assuming your processor has a way to reset

        // Use OR to look for either ICICI or HDFC in the sender address
        String selection = Telephony.Sms.ADDRESS + " LIKE ? OR " + Telephony.Sms.ADDRESS + " LIKE ?";

        // The % wildcards ensure it matches names like AD-ICICIT or HDFCBK
        String[] selectionArgs = new String[]{"%ICICI%", "%HDFC%"};
        String sortOrder = Telephony.Sms.DATE + " DESC";

        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                sortOrder
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

        updateTotals();
        if (listViewOfSMS.getAdapter() != null) {
            ((ArrayAdapter<?>) listViewOfSMS.getAdapter()).notifyDataSetChanged();
        }
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void calculateGroupedTotals() {
        double spentToday = 0, spentWeek = 0, spentMonth = 0, spentYear = 0;
        Calendar now = Calendar.getInstance();

        for (SmsComponent sms : smsComponents) {
            if (sms.getAmountType().equalsIgnoreCase("DEBIT")) {
                Calendar smsCal = Calendar.getInstance();
                smsCal.setTime(sms.getSMSDate());

                if (smsCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                    spentYear += sms.getSMSAmount(); // Total for current year

                    if (smsCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)) {
                        spentMonth += sms.getSMSAmount(); // Total for current month

                        if (smsCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)) {
                            spentWeek += sms.getSMSAmount(); // Total for current week
                        }

                        if (smsCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                            spentToday += sms.getSMSAmount(); // Total for today
                        }
                    }
                }
            }
        }

        tvToday.setText("₹" + String.format("%.2f", spentToday));
        tvWeek.setText("₹" + String.format("%.2f", spentWeek));
        tvMonth.setText("₹" + String.format("%.2f", spentMonth));
        tvYear.setText("₹" + String.format("%.2f", spentYear));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            readSms();
        }
    }
}