package com.example.smsexpensetracker.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smsexpensetracker.R;
import com.example.smsexpensetracker.adapters.TransactionAdapter;
import com.example.smsexpensetracker.databinding.ActivityMainBinding;
import com.example.smsexpensetracker.databinding.ItemCategoryInsightBinding;
import com.example.smsexpensetracker.models.Transaction;
import com.example.smsexpensetracker.utils.ExportHelper;
import com.example.smsexpensetracker.utils.Prefs;
import com.example.smsexpensetracker.viewmodel.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private MainViewModel vm;
    private TransactionAdapter adapter;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── First-launch guard ─────────────────────────────────────────────
        if (Prefs.isFirstLaunch(this)) {
            Intent i = new Intent(this, SetupActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }

        // ── Normal launch ──────────────────────────────────────────────────
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        // RecyclerView
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this::onLongPressTransaction);
        binding.rvTransactions.setAdapter(adapter);

        // FAB
        binding.fabExport.setOnClickListener(v -> exportCsv());

        // ViewModel
        vm = new ViewModelProvider(this).get(MainViewModel.class);

        vm.transactions.observe(this, list -> {
            boolean wasEmpty = adapter.getCurrentList().isEmpty();
            adapter.submitList(list);
            if (wasEmpty && list != null && !list.isEmpty()) {
                LayoutAnimationController anim = AnimationUtils.loadLayoutAnimation(
                        this, R.anim.layout_animation_fall_down);
                binding.rvTransactions.setLayoutAnimation(anim);
                binding.rvTransactions.scheduleLayoutAnimation();
            }
            binding.emptyState.setVisibility(
                    (list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
            
            // Update the "Recent Transactions" label based on filter
            String currentFilter = vm.getFilterCategory();
            if (currentFilter != null) {
                binding.tvLabelTransactions.setText(currentFilter + " Transactions");
            } else {
                binding.tvLabelTransactions.setText("Recent Transactions");
            }
        });

        vm.summary.observe(this, s -> {
            binding.tvToday.setText(fmt(s.today));
            binding.tvWeek.setText(fmt(s.week));
            binding.tvMonth.setText(fmt(s.month));
            binding.tvYear.setText(fmt(s.year));
        });

        // Observe Category Breakdown for Insights Dashboard
        vm.breakdown.observe(this, cb -> {
            renderInsights(cb);
        });

        vm.syncing.observe(this, loading ->
            binding.mainProgress.setVisibility(loading ? View.VISIBLE : View.GONE)
        );

        vm.syncResult.observe(this, count -> {
            if (count != null && count > 0) {
                Snackbar.make(binding.getRoot(),
                        count + " new transaction" + (count == 1 ? "" : "s") + " found",
                        Snackbar.LENGTH_SHORT).show();
                vm.syncResult.setValue(null);
            }
        });

        syncIfPermitted();
    }

    private void renderInsights(MainViewModel.CategoryBreakdown cb) {
        binding.insightsContainer.removeAllViews();
        if (cb == null || cb.totals.isEmpty()) {
            binding.insightsContainer.addView(binding.tvNoInsights);
            binding.tvNoInsights.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoInsights.setVisibility(View.GONE);
            LayoutInflater inflater = getLayoutInflater();
            String activeFilter = vm.getFilterCategory();

            for (Map.Entry<String, Double> entry : cb.totals.entrySet()) {
                String cat = entry.getKey();
                ItemCategoryInsightBinding b = ItemCategoryInsightBinding.inflate(inflater, binding.insightsContainer, false);
                b.tvCategoryName.setText(cat);
                b.tvCategoryAmount.setText(fmt(entry.getValue()));

                // Highlight if active
                if (cat.equals(activeFilter)) {
                    b.getRoot().setStrokeColor(Color.parseColor("#9D5CFF")); // Electric Violet
                    b.getRoot().setStrokeWidth(4);
                } else {
                    b.getRoot().setStrokeColor(Color.parseColor("#3F3F4D")); // Default outline
                    b.getRoot().setStrokeWidth(2);
                }

                b.getRoot().setOnClickListener(v -> {
                    if (cat.equals(vm.getFilterCategory())) {
                        vm.setFilterCategory(null); // Clear filter
                    } else {
                        vm.setFilterCategory(cat); // Set filter
                    }
                    renderInsights(cb); // Re-render to update highlights
                });

                binding.insightsContainer.addView(b.getRoot());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null) vm.refreshSummary();
    }

    private void syncIfPermitted() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            vm.sync();
        }
    }

    private void onLongPressTransaction(Transaction t) {
        String[] cats = {"Food","Transport","Shopping","Entertainment",
                         "Health","Utilities","Fuel","ATM","Transfer","EMI","Investment","Other"};
        int current = -1;
        for (int i = 0; i < cats.length; i++) {
            if (cats[i].equals(t.category)) { current = i; break; }
        }
        new MaterialAlertDialogBuilder(this)
            .setTitle("Re-categorise")
            .setSingleChoiceItems(cats, current, (d, i) -> {
                vm.updateCategory(t, cats[i]);
                d.dismiss();
                Snackbar.make(binding.getRoot(), "Category → " + cats[i], Snackbar.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
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

        if (id == R.id.menu_refresh) {
            syncIfPermitted();
            return true;
        }
        if (id == R.id.menu_add_senders) {
            Intent i = new Intent(this, SetupActivity.class);
            i.putExtra("manage_mode", true);
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        }
        if (id == R.id.menu_remove_sender) {
            showRemoveSenderDialog();
            return true;
        }
        if (id == R.id.menu_budgets) {
            startActivity(new Intent(this, BudgetActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        }
        if (id == R.id.menu_export) {
            exportCsv();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRemoveSenderDialog() {
        Set<String> senders = Prefs.getSenders(this);
        if (senders.isEmpty()) {
            Snackbar.make(binding.getRoot(), "No senders configured.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String[] arr      = senders.toArray(new String[0]);
        boolean[] checked = new boolean[arr.length];

        new MaterialAlertDialogBuilder(this)
            .setTitle("Remove Senders")
            .setMultiChoiceItems(arr, checked, (d, i, isChecked) -> checked[i] = isChecked)
            .setPositiveButton("Remove", (d, w) -> {
                Set<String> current = Prefs.getSenders(this);
                int removed = 0;
                for (int i = 0; i < arr.length; i++) {
                    if (checked[i]) { current.remove(arr[i]); removed++; }
                }
                if (removed > 0) {
                    Prefs.saveSenders(this, current);
                    Snackbar.make(binding.getRoot(),
                            removed + " sender" + (removed == 1 ? "" : "s") + " removed.",
                            Snackbar.LENGTH_SHORT).show();
                }
                if (Prefs.isFirstLaunch(this)) {
                    Intent i = new Intent(this, SetupActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void exportCsv() {
        vm.getAllForExport(list -> {
            if (list == null || list.isEmpty()) {
                runOnUiThread(() ->
                    Snackbar.make(binding.getRoot(),
                            "No transactions to export.", Snackbar.LENGTH_SHORT).show()
                );
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

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
