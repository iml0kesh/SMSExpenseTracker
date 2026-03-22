package com.example.smsexpensetracker.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.smsexpensetracker.utils.ImportHelper;
import com.example.smsexpensetracker.utils.Prefs;
import com.example.smsexpensetracker.utils.SmsParser;
import com.example.smsexpensetracker.viewmodel.MainViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private MainViewModel vm;
    private TransactionAdapter adapter;
    private ActivityMainBinding binding;

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) performImport(uri); }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // First-launch guard — runs before setContentView, zero flicker
        if (Prefs.isFirstLaunch(this)) {
            Intent i = new Intent(this, SetupActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // RecyclerView
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this::showTransactionDetails);
        binding.rvTransactions.setAdapter(adapter);

        // Bottom bar buttons
        binding.fabExport.setOnClickListener(v -> exportCsv());
        binding.btnSettings.setOnClickListener(v -> showSettingsSheet());
        binding.btnLoadMore.setOnClickListener(v -> vm.loadMore());

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
            String currentFilter = vm.getFilterCategory();
            binding.tvLabelTransactions.setText(
                    currentFilter != null ? currentFilter + " Transactions" : "Transactions");
        });

        vm.hasMore.observe(this, more -> {
            binding.btnLoadMore.setVisibility(more ? View.VISIBLE : View.GONE);
            binding.tvNoMore.setVisibility(
                    !more && !adapter.getCurrentList().isEmpty() ? View.VISIBLE : View.GONE);
        });

        vm.summary.observe(this, s -> {
            binding.tvToday.setText(fmt(s.today));
            binding.tvWeek.setText(fmt(s.week));
            binding.tvMonth.setText(fmt(s.month));
            binding.tvYear.setText(fmt(s.year));
        });

        vm.breakdown.observe(this, this::renderInsights);

        vm.syncing.observe(this, loading ->
                binding.mainProgress.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.syncResult.observe(this, count -> {
            if (count != null && count > 0) {
                Snackbar.make(binding.getRoot(),
                        count + " new transaction" + (count == 1 ? "" : "s") + " found",
                        Snackbar.LENGTH_SHORT).show();
                vm.syncResult.setValue(null);
            }
        });

        setupBankFilters();
        syncIfPermitted();
    }

    // ── Settings bottom sheet (replaces top menu) ─────────────────────────

    private void showSettingsSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.sheet_settings, null);
        sheet.setContentView(view);

        view.findViewById(R.id.sheetRefresh).setOnClickListener(v -> {
            sheet.dismiss();
            syncIfPermitted();
        });
        view.findViewById(R.id.sheetAddSenders).setOnClickListener(v -> {
            sheet.dismiss();
            Intent i = new Intent(this, SetupActivity.class);
            i.putExtra("manage_mode", true);
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        view.findViewById(R.id.sheetRemoveSenders).setOnClickListener(v -> {
            sheet.dismiss();
            showRemoveSenderDialog();
        });
        view.findViewById(R.id.sheetBudgets).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, BudgetActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        view.findViewById(R.id.sheetImport).setOnClickListener(v -> {
            sheet.dismiss();
            importLauncher.launch(new String[]{"text/csv", "text/comma-separated-values", "application/csv"});
        });
        view.findViewById(R.id.sheetExport).setOnClickListener(v -> {
            sheet.dismiss();
            exportCsv();
        });

        sheet.show();
    }

    // ── Transaction detail bottom sheet ───────────────────────────────────

    private void showTransactionDetails(Transaction t) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_transaction_details, null);
        dialog.setContentView(view);

        TextView tvAmount  = view.findViewById(R.id.detailAmount);
        TextView tvType    = view.findViewById(R.id.detailType);
        TextView tvSender  = view.findViewById(R.id.detailSender);
        TextView tvMerchant = view.findViewById(R.id.detailMerchant);
        TextView tvDate    = view.findViewById(R.id.detailDate);
        TextView tvSmsBody = view.findViewById(R.id.detailSmsBody);
        ChipGroup chipGroup = view.findViewById(R.id.categoryChipGroup);

        tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", t.amount));

        if ("DEBIT".equals(t.type)) {
            tvType.setText("DEBITED");
            tvType.setTextColor(Color.parseColor("#F87171"));
        } else if ("CREDIT".equals(t.type)) {
            tvType.setText("RECEIVED");
            tvType.setTextColor(Color.parseColor("#10B981"));
        } else {
            tvType.setText("TRANSACTION");
            tvType.setTextColor(Color.parseColor("#8B9CB6"));
        }

        tvSender.setText(TransactionAdapter.decodeSender(t.sender));
        tvMerchant.setText(t.merchant != null ? t.merchant : "—");
        tvDate.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                Locale.getDefault()).format(new Date(t.dateMillis)));
        tvSmsBody.setText(t.body);

        String[] cats = {"Food","Transport","Shopping","Entertainment",
                         "Health","Utilities","Fuel","ATM","Transfer","EMI","Investment","Other"};
        for (String cat : cats) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChecked(cat.equals(t.category));
            chip.setOnClickListener(v -> {
                vm.updateCategory(t, cat);
                dialog.dismiss();
                Snackbar.make(binding.getRoot(), "Category → " + cat, Snackbar.LENGTH_SHORT).show();
            });
            chipGroup.addView(chip);
        }
        dialog.show();
    }

    // ── Bank filter chips ─────────────────────────────────────────────────

    private void setupBankFilters() {
        Set<String> allSenderIds = Prefs.getSenders(this);
        Set<String> bankNames = new HashSet<>();
        for (String id : allSenderIds) bankNames.add(SmsParser.getBankName(id));

        List<String> sortedBanks = new ArrayList<>(bankNames);
        Collections.sort(sortedBanks);

        binding.bankChipGroup.removeAllViews();
        for (String bank : sortedBanks) {
            Chip chip = new Chip(this);
            chip.setText(bank);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((v, isChecked) -> vm.toggleBankFilter(bank));
            binding.bankChipGroup.addView(chip);
        }
    }

    // ── Insights row ──────────────────────────────────────────────────────

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
                ItemCategoryInsightBinding b = ItemCategoryInsightBinding.inflate(
                        inflater, binding.insightsContainer, false);
                b.tvCategoryName.setText(cat);
                b.tvCategoryAmount.setText(fmt(entry.getValue()));
                if (cat.equals(activeFilter)) {
                    b.getRoot().setStrokeColor(Color.parseColor("#0EA5E9"));
                    b.getRoot().setStrokeWidth(3);
                } else {
                    b.getRoot().setStrokeColor(Color.parseColor("#2A3A52"));
                    b.getRoot().setStrokeWidth(1);
                }
                b.getRoot().setOnClickListener(v -> {
                    vm.setFilterCategory(cat.equals(vm.getFilterCategory()) ? null : cat);
                    renderInsights(cb);
                });
                binding.insightsContainer.addView(b.getRoot());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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

    private void performImport(Uri uri) {
        binding.mainProgress.setVisibility(View.VISIBLE);
        ImportHelper.importCsv(this, uri, count -> {
            binding.mainProgress.setVisibility(View.GONE);
            Toast.makeText(this,
                    count >= 0 ? "Imported " + count + " transactions" : "Import failed",
                    Toast.LENGTH_SHORT).show();
            if (count >= 0) vm.refreshSummary();
        });
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
                    setupBankFilters();
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
                runOnUiThread(() -> Snackbar.make(binding.getRoot(),
                        "No transactions to export.", Snackbar.LENGTH_SHORT).show());
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

    @Override public void onBackPressed() { moveTaskToBack(true); }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
