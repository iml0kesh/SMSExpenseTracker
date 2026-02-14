package com.example.smsexpensetracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BankSelectionActivity extends AppCompatActivity {

    private final String[] popularBanks = new String[]{
            "ICICI", "HDFC", "SBI", "AXIS", "KOTAK", "Paytm", "PhonePe", "CRED",
            "AMEX", "CITI", "PNB", "BOB", "CANARA", "UNION", "IDBI", "INDUSIND"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_selection);

        ListView bankListView = findViewById(R.id.bankListView);
        Button nextButton = findViewById(R.id.nextButton);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice, popularBanks);
        bankListView.setAdapter(adapter);

        loadAndCheckSavedBanks(bankListView);

        nextButton.setOnClickListener(v -> handleNextButtonClick(bankListView));
    }

    private void handleNextButtonClick(ListView bankListView) {
        SharedPreferences prefs = getSharedPreferences(SenderSelectionActivity.PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> previouslySavedSenders = prefs.getStringSet(SenderSelectionActivity.KEY_SELECTED_SENDERS, new HashSet<>());

        ArrayList<String> currentlyCheckedBanks = new ArrayList<>();
        SparseBooleanArray checkedPositions = bankListView.getCheckedItemPositions();
        for (int i = 0; i < popularBanks.length; i++) {
            if (checkedPositions.get(i)) {
                currentlyCheckedBanks.add(popularBanks[i]);
            }
        }

        Set<String> sendersToKeep = new HashSet<>();
        for (String sender : previouslySavedSenders) {
            for (String bank : currentlyCheckedBanks) {
                if (sender.toUpperCase().contains(bank.toUpperCase())) {
                    sendersToKeep.add(sender);
                    break;
                }
            }
        }

        ArrayList<String> banksToConfigure = new ArrayList<>();
        for (String bank : currentlyCheckedBanks) {
            boolean isAlreadyConfigured = false;
            for (String sender : sendersToKeep) {
                if (sender.toUpperCase().contains(bank.toUpperCase())) {
                    isAlreadyConfigured = true;
                    break;
                }
            }
            if (!isAlreadyConfigured) {
                banksToConfigure.add(bank);
            }
        }

        prefs.edit().putStringSet(SenderSelectionActivity.KEY_SELECTED_SENDERS, sendersToKeep).apply();

        if (banksToConfigure.isEmpty()) {
            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            // **THE FIX:** Corrected the typo in the class name.
            Intent intent = new Intent(this, SenderSelectionActivity.class);
            intent.putStringArrayListExtra("SELECTED_BANKS", banksToConfigure);
            startActivity(intent);
        }
    }

    private void loadAndCheckSavedBanks(ListView bankListView) {
        SharedPreferences prefs = getSharedPreferences(SenderSelectionActivity.PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> savedSenders = prefs.getStringSet(SenderSelectionActivity.KEY_SELECTED_SENDERS, new HashSet<>());

        if (savedSenders.isEmpty()) return;

        for (int i = 0; i < popularBanks.length; i++) {
            String bank = popularBanks[i];
            for (String savedSender : savedSenders) {
                if (savedSender.toUpperCase().contains(bank.toUpperCase())) {
                    bankListView.setItemChecked(i, true);
                    break;
                }
            }
        }
    }
}
