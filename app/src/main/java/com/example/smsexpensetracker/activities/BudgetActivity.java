package com.example.smsexpensetracker.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smsexpensetracker.R;
import com.example.smsexpensetracker.adapters.BudgetAdapter;
import com.example.smsexpensetracker.utils.Prefs;
import com.example.smsexpensetracker.viewmodel.MainViewModel;

import java.util.HashMap;
import java.util.Map;

public class BudgetActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {
        "Food", "Transport", "Shopping", "Entertainment",
        "Health", "Utilities", "Fuel", "ATM", "Transfer", "EMI", "Investment", "Other"
    };

    private BudgetAdapter adapter;
    private MainViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        vm = new ViewModelProvider(this).get(MainViewModel.class);

        RecyclerView rv = findViewById(R.id.budgetRecycler);
        rv.setLayoutManager(new LinearLayoutManager(this));

        Button btnSave = findViewById(R.id.btnSaveBudgets);

        // Load existing budgets
        Map<String, Double> budgets = new HashMap<>();
        for (String cat : CATEGORIES) {
            double b = Prefs.getBudget(this, cat);
            budgets.put(cat, b);
        }

        // Observe monthly spending breakdown
        vm.breakdown.observe(this, breakdown -> {
            Map<String, Double> spent = breakdown != null ? breakdown.totals : new HashMap<>();
            adapter = new BudgetAdapter(CATEGORIES, budgets, spent);
            rv.setAdapter(adapter);
        });

        vm.refreshSummary();

        btnSave.setOnClickListener(v -> {
            if (adapter == null) return;
            Map<String, Double> updated = adapter.getBudgets();
            for (Map.Entry<String, Double> e : updated.entrySet()) {
                Prefs.setBudget(this, e.getKey(), e.getValue());
            }
            Toast.makeText(this, "Budgets saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
