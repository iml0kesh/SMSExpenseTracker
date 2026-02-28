package com.example.smsexpensetracker.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smsexpensetracker.R;
import com.example.smsexpensetracker.adapters.TransactionAdapter;
import com.example.smsexpensetracker.models.Transaction;
import com.example.smsexpensetracker.utils.ExportHelper;
import com.example.smsexpensetracker.utils.Prefs;
import com.example.smsexpensetracker.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private MainViewModel vm;
    private TransactionAdapter adapter;
    private TextView tvToday, tvWeek, tvMonth, tvYear;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Redirect to setup if no senders configured
        if (Prefs.isFirstLaunch(this)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Bind views
        tvToday   = findViewById(R.id.tvToday);
        tvWeek    = findViewById(R.id.tvWeek);
        tvMonth   = findViewById(R.id.tvMonth);
        tvYear    = findViewById(R.id.tvYear);
        progressBar = findViewById(R.id.mainProgress);

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rvTransactions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(new ArrayList<>(), this::onLongPressTransaction);
        rv.setAdapter(adapter);

        // ViewModel
        vm = new ViewModelProvider(this).get(MainViewModel.class);

        vm.transactions.observe(this, list -> {
            adapter.setData(list);
        });

        vm.summary.observe(this, s -> {
            tvToday.setText(fmt(s.today));
            tvWeek.setText(fmt(s.week));
            tvMonth.setText(fmt(s.month));
            tvYear.setText(fmt(s.year));
        });

        vm.syncing.observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        // Initial sync
        syncIfPermitted();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh summary every time we come back (e.g. after budget screen)
        vm.refreshSummary();
    }

    private void syncIfPermitted() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            vm.sync();
        }
    }

    // Long press a transaction → re-categorise dialog
    private void onLongPressTransaction(Transaction t) {
        String[] cats = {"Food","Transport","Shopping","Entertainment",
                         "Health","Utilities","Fuel","ATM","Transfer","EMI","Investment","Other"};
        new AlertDialog.Builder(this)
            .setTitle("Set Category for this transaction")
            .setItems(cats, (d, i) -> {
                vm.updateCategory(t, cats[i]);
                Toast.makeText(this, "Category updated to " + cats[i], Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    // ── Menu ──────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_add_senders) {
            startActivity(new Intent(this, SetupActivity.class));
            return true;
        }
        if (id == R.id.menu_remove_sender) {
            showRemoveSenderDialog();
            return true;
        }
        if (id == R.id.menu_budgets) {
            startActivity(new Intent(this, BudgetActivity.class));
            return true;
        }
        if (id == R.id.menu_export) {
            exportCsv();
            return true;
        }
        if (id == R.id.menu_refresh) {
            syncIfPermitted();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRemoveSenderDialog() {
        Set<String> senders = Prefs.getSenders(this);
        if (senders.isEmpty()) {
            Toast.makeText(this, "No senders configured.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] arr = senders.toArray(new String[0]);
        boolean[] checked = new boolean[arr.length];

        new AlertDialog.Builder(this)
            .setTitle("Remove Senders")
            .setMultiChoiceItems(arr, checked, (d, i, isChecked) -> checked[i] = isChecked)
            .setPositiveButton("Remove", (d, w) -> {
                Set<String> current = Prefs.getSenders(this);
                for (int i = 0; i < arr.length; i++) {
                    if (checked[i]) current.remove(arr[i]);
                }
                Prefs.saveSenders(this, current);
                Toast.makeText(this, "Senders removed.", Toast.LENGTH_SHORT).show();
                if (Prefs.isFirstLaunch(this)) {
                    startActivity(new Intent(this, SetupActivity.class));
                    finish();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void exportCsv() {
        vm.getAllForExport(list -> {
            if (list == null || list.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "No transactions to export.", Toast.LENGTH_SHORT).show());
                return;
            }
            ExportHelper.export(this, list);
        });
    }

    private String fmt(double amount) {
        if (amount >= 100000) return String.format("₹%.1fL", amount / 100000);
        if (amount >= 1000)   return String.format("₹%.1fK", amount / 1000);
        return String.format("₹%.0f", amount);
    }
}
