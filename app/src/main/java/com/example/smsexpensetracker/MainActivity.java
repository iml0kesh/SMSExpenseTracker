package com.example.smsexpensetracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    TextView tvToday, tvWeek, tvMonth, tvYear;
    private final ArrayList<SmsComponent> smsComponents = new ArrayList<>();
    private ListView listViewOfSMS;
    private static final int READ_SMS_PERMISSION_CODE = 1;

    private final SmsProcessor processor = new SmsProcessor();
    private SmsReader smsReader;
    private final ExpenseCalculator expenseCalculator = new ExpenseCalculator();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // **THE WELCOME GATE**
        // Check if the user has completed the setup. If not, redirect them.
        SharedPreferences prefs = getSharedPreferences(SenderSelectionActivity.PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getStringSet(SenderSelectionActivity.KEY_SELECTED_SENDERS, new HashSet<>()).isEmpty()) {
            Intent intent = new Intent(this, BankSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Stop the rest of the onCreate method from executing
        }

        tvToday = findViewById(R.id.tvToday);
        tvWeek = findViewById(R.id.tvWeek);
        tvMonth = findViewById(R.id.tvMonth);
        tvYear = findViewById(R.id.tvYear);
        listViewOfSMS = findViewById(R.id.listViewOfSMS);

        SmsAdapter adapter = new SmsAdapter(this, smsComponents);
        listViewOfSMS.setAdapter(adapter);
        smsReader = new SmsReader(getContentResolver());

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            loadAndProcessSms();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            loadAndProcessSms();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_select_senders) {
            startActivity(new Intent(this, BankSelectionActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAndProcessSms() {
        smsComponents.clear();
        processor.resetTotals();

        SharedPreferences prefs = getSharedPreferences(SenderSelectionActivity.PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> selectedSendersSet = prefs.getStringSet(SenderSelectionActivity.KEY_SELECTED_SENDERS, new HashSet<>());
        String[] selectedSenders = selectedSendersSet.toArray(new String[0]);


        List<SmsComponent> rawSmsList = smsReader.readSmsFromSenders(selectedSenders);

        for (SmsComponent rawSms : rawSmsList) {
            SmsProcessor.ProcessResult result = processor.processSms(rawSms.getSMSBody());
            String type = (result.type == SmsProcessor.TransactionType.DEBIT) ? "DEBIT" : "CREDIT";

            SmsComponent processedSms = new SmsComponent(
                    rawSms.getSMSSender(),
                    rawSms.getSMSBody(),
                    rawSms.getSMSDate(),
                    result.amount,
                    type
            );
            smsComponents.add(processedSms);
        }

        updateUiWithTotals();
        if (listViewOfSMS.getAdapter() != null) {
            ((ArrayAdapter<?>) listViewOfSMS.getAdapter()).notifyDataSetChanged();
        }
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void updateUiWithTotals() {
        ExpenseCalculator.Totals totals = expenseCalculator.calculate(smsComponents);

        tvToday.setText("₹" + String.format("%.2f", totals.spentToday));
        tvWeek.setText("₹" + String.format("%.2f", totals.spentWeek));
        tvMonth.setText("₹" + String.format("%.2f", totals.spentMonth));
        tvYear.setText("₹" + String.format("%.2f", totals.spentYear));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAndProcessSms();
        }
    }
}
