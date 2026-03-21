package com.example.smsexpensetracker.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smsexpensetracker.R;
import com.example.smsexpensetracker.adapters.BudgetAdapter;
import com.example.smsexpensetracker.databinding.ActivityBudgetBinding;
import com.example.smsexpensetracker.utils.Prefs;
import com.example.smsexpensetracker.viewmodel.MainViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

public class BudgetActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {
        "Food","Transport","Shopping","Entertainment",
        "Health","Utilities","Fuel","ATM","Transfer","EMI","Investment","Other"
    };

    private BudgetAdapter adapter;
    private MainViewModel vm;
    private ActivityBudgetBinding binding;
    // Load budgets once, don't rebuild adapter on every observe
    private final Map<String, Double> loadedBudgets = new HashMap<>();
    private boolean adapterInitialised = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBudgetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        vm = new ViewModelProvider(this).get(MainViewModel.class);
        binding.budgetRecycler.setLayoutManager(new LinearLayoutManager(this));

        // Load saved budgets once
        for (String cat : CATEGORIES) {
            loadedBudgets.put(cat, Prefs.getBudget(this, cat));
        }

        // Observe breakdown — only init adapter the FIRST time
        vm.breakdown.observe(this, breakdown -> {
            Map<String, Double> spent = (breakdown != null) ? breakdown.totals : new HashMap<>();
            if (!adapterInitialised) {
                adapter = new BudgetAdapter(CATEGORIES, loadedBudgets, spent);
                binding.budgetRecycler.setAdapter(adapter);
                adapterInitialised = true;
            } else {
                // Update spent amounts without recreating — preserve user edits
                adapter.updateSpent(spent);
            }
        });

        vm.refreshSummary();

        binding.btnSaveBudgets.setOnClickListener(v -> {
            if (adapter == null) return;
            Map<String, Double> updated = adapter.getBudgets();
            for (Map.Entry<String, Double> e : updated.entrySet()) {
                Prefs.setBudget(this, e.getKey(), e.getValue());
            }
            Snackbar.make(binding.getRoot(), "Budgets saved!", Snackbar.LENGTH_SHORT).show();
            // Small delay so snackbar is visible, then finish
            binding.getRoot().postDelayed(this::finish, 800);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
