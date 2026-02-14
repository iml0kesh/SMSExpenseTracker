package com.example.smsexpensetracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SenderSelectionActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "SmsTrackerPrefs";
    public static final String KEY_SELECTED_SENDERS = "SelectedSenders";

    private SenderAdapter adapter;
    // The single source of truth for ALL senders selected across the entire app
    private Set<String> masterSelections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender_selection);

        SmsReader smsReader = new SmsReader(getContentResolver());
        ListView senderListView = findViewById(R.id.senderListView);
        Button saveButton = findViewById(R.id.saveButton);
        SearchView searchView = findViewById(R.id.searchView);

        // 1. Get the list of new banks the user wants to configure.
        ArrayList<String> banksToConfigure = getIntent().getStringArrayListExtra("SELECTED_BANKS");

        // 2. Load the master list of all currently saved senders.
        loadMasterSelections();

        // 3. Get all potential senders from the user's phone.
        List<SenderInfo> allSendersOnPhone = smsReader.getAllUniqueSendersWithLatestMessage();

        // 4. Filter the list to show only senders for the new banks.
        List<SenderInfo> displayedSenders = new ArrayList<>();
        if (banksToConfigure != null) {
            for (SenderInfo sender : allSendersOnPhone) {
                for (String bank : banksToConfigure) {
                    if (sender.getAddress().toUpperCase().contains(bank.toUpperCase())) {
                        displayedSenders.add(sender);
                        break;
                    }
                }
            }
        }

        // 5. Give the adapter the list to display and the master selection set.
        adapter = new SenderAdapter(this, displayedSenders, masterSelections);
        senderListView.setAdapter(adapter);

        // 6. When an item is clicked, update the master set directly.
        senderListView.setOnItemClickListener((parent, view, position, id) -> {
            SenderInfo info = adapter.getItem(position);
            if (info != null) {
                if (masterSelections.contains(info.getAddress())) {
                    masterSelections.remove(info.getAddress());
                } else {
                    masterSelections.add(info.getAddress());
                }
                adapter.notifyDataSetChanged();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });

        saveButton.setOnClickListener(v -> saveAndFinish());
    }

    private void loadMasterSelections() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        masterSelections = new HashSet<>(prefs.getStringSet(KEY_SELECTED_SENDERS, new HashSet<>()));
    }

    private void saveAndFinish() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_SELECTED_SENDERS, masterSelections).apply();

        Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
