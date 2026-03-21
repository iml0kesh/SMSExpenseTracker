package com.example.smsexpensetracker.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smsexpensetracker.R;
import com.example.smsexpensetracker.adapters.SenderPickerAdapter;
import com.example.smsexpensetracker.databinding.ActivitySetupBinding;
import com.example.smsexpensetracker.utils.Prefs;
import com.example.smsexpensetracker.utils.SmsIngestor;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * SetupActivity is only ever reached in two cases:
 *   1. First launch — MainActivity redirects here (no senders saved)
 *   2. Manage mode — user taps "Add Senders" from the menu
 *
 * It is NOT the launcher. MainActivity is the launcher and handles
 * the first-launch check itself, so there is zero flicker.
 */
public class SetupActivity extends AppCompatActivity {

    private static final int PERM_CODE = 100;

    private ActivitySetupBinding binding;
    private SenderPickerAdapter adapter;
    private final Set<String> preSelected = new HashSet<>();
    private boolean showingAll   = false;
    private boolean isManageMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isManageMode = getIntent().getBooleanExtra("manage_mode", false);

        // Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            if (isManageMode) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
        }
        binding.toolbarTitle.setText(isManageMode ? "Manage Senders" : "Select Bank Senders");

        binding.setupRecycler.setLayoutManager(new LinearLayoutManager(this));

        // Pre-check senders already saved (for manage mode)
        preSelected.addAll(Prefs.getSenders(this));

        // Search
        binding.setupSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (adapter != null) adapter.filter(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });

        // Show-all toggle
        binding.setupSwitchAll.setOnCheckedChangeListener((btn, checked) -> {
            showingAll = checked;
            if (adapter != null) binding.setupSearch.setText("");
            loadSenders();
        });

        binding.setupBtnManual.setOnClickListener(v -> showManualEntryDialog());
        binding.setupSaveBtn.setOnClickListener(v -> saveAndProceed());

        checkPermission();
    }

    // ── Permission ────────────────────────────────────────────────────────

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            loadSenders();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_SMS,
                                 android.Manifest.permission.RECEIVE_SMS}, PERM_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERM_CODE && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadSenders();
        } else {
            binding.setupStatus.setText(
                    "SMS permission required. Grant it in Settings → Apps → Permissions.");
            binding.setupBtnManual.setEnabled(true);
            binding.setupSaveBtn.setEnabled(true);
            binding.setupProgress.setVisibility(View.GONE);
        }
    }

    // ── Load senders ──────────────────────────────────────────────────────

    private void loadSenders() {
        binding.setupStatus.setText("Scanning your SMS inbox…");
        binding.setupProgress.setVisibility(View.VISIBLE);
        binding.setupSaveBtn.setEnabled(false);
        binding.setupSearch.setEnabled(false);
        binding.setupSwitchAll.setEnabled(false);
        binding.setupBtnManual.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<String[]> senders = showingAll
                    ? SmsIngestor.getAllSenders(getContentResolver())
                    : SmsIngestor.getFinancialSenders(getContentResolver());

            runOnUiThread(() -> {
                binding.setupProgress.setVisibility(View.GONE);
                binding.setupSearch.setEnabled(true);
                binding.setupSwitchAll.setEnabled(true);
                binding.setupBtnManual.setEnabled(true);
                binding.setupSaveBtn.setEnabled(true);

                if (senders.isEmpty() && !showingAll) {
                    binding.setupStatus.setText(
                            "No bank SMS detected. Toggle \"All\" or add manually ↓");
                } else if (senders.isEmpty()) {
                    binding.setupStatus.setText("No SMS found in inbox.");
                } else {
                    binding.setupStatus.setText("Found " + senders.size()
                            + " sender" + (senders.size() == 1 ? "" : "s")
                            + " — select to track:");
                }

                if (adapter == null) {
                    adapter = new SenderPickerAdapter(senders, preSelected);
                    binding.setupRecycler.setAdapter(adapter);
                } else {
                    adapter.replaceSenders(senders);
                }
            });
        });
    }

    // ── Manual entry ──────────────────────────────────────────────────────

    private void showManualEntryDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("e.g. JD-HDFCBK");
        input.setSingleLine(true);
        input.setPadding(48, 32, 48, 16);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Add Sender Manually")
            .setMessage("Enter the exact sender ID shown in your bank SMS.")
            .setView(input)
            .setPositiveButton("Add", (d, w) -> {
                String val = input.getText() != null
                        ? input.getText().toString().trim() : "";
                if (val.isEmpty()) {
                    Toast.makeText(this, "Enter a sender ID.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (adapter == null) {
                    adapter = new SenderPickerAdapter(new ArrayList<>(), preSelected);
                    binding.setupRecycler.setAdapter(adapter);
                    binding.setupSaveBtn.setEnabled(true);
                }
                adapter.addManualSender(val, "Added manually");
                Toast.makeText(this, "\"" + val + "\" added ✓", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ── Save & navigate ───────────────────────────────────────────────────

    private void saveAndProceed() {
        Set<String> picks = adapter != null ? adapter.getSelected() : preSelected;
        if (picks.isEmpty()) {
            Toast.makeText(this, "Select at least one sender.", Toast.LENGTH_SHORT).show();
            return;
        }
        Prefs.saveSenders(this, picks);

        if (isManageMode) {
            // Slide back to MainActivity
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } else {
            // First-launch: fade into MainActivity (no slide — feels like arriving, not going back)
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
    }

    // ── Back handling ─────────────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        if (isManageMode) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        } else {
            // First launch — nowhere to go back to, keep in background
            moveTaskToBack(true);
        }
    }
}
