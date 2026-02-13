package com.example.smsexpensetracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
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

    private SmsReader smsReader;
    private ListView senderListView;
    private ArrayAdapter<String> adapter;
    private List<String> allSenders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender_selection);

        smsReader = new SmsReader(getContentResolver());
        senderListView = findViewById(R.id.senderListView);
        Button saveButton = findViewById(R.id.saveButton);
        SearchView searchView = findViewById(R.id.searchView);

        // Get the full list of potential senders
        allSenders = smsReader.getAllUniqueSenders();

        // Set up the adapter for the ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, allSenders);
        senderListView.setAdapter(adapter);

        // Load previously saved selections
        loadAndCheckSavedSenders();

        // Set up the search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });

        // Set up the save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSelectedSenders();
            }
        });
    }

    private void loadAndCheckSavedSenders() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> savedSenders = prefs.getStringSet(KEY_SELECTED_SENDERS, new HashSet<String>());

        for (int i = 0; i < allSenders.size(); i++) {
            if (savedSenders.contains(allSenders.get(i))) {
                senderListView.setItemChecked(i, true);
            }
        }
    }

    private void saveSelectedSenders() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> selectedSenders = new HashSet<>();
        SparseBooleanArray checkedItems = senderListView.getCheckedItemPositions();

        for (int i = 0; i < adapter.getCount(); i++) {
            if (checkedItems.get(i)) {
                selectedSenders.add(adapter.getItem(i));
            }
        }

        editor.putStringSet(KEY_SELECTED_SENDERS, selectedSenders);
        editor.apply();

        Toast.makeText(this, "Senders saved!", Toast.LENGTH_SHORT).show();
        finish(); // Close the activity and return to MainActivity
    }
}
