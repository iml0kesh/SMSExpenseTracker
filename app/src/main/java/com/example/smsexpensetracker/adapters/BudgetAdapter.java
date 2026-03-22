package com.example.smsexpensetracker.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smsexpensetracker.databinding.ItemBudgetBinding;
import com.google.android.material.color.MaterialColors;

import java.util.HashMap;
import java.util.Map;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.VH> {

    private final String[]           categories;
    private final Map<String,Double> editedBudgets = new HashMap<>();
    private Map<String,Double>       spent;

    public BudgetAdapter(String[] categories, Map<String,Double> budgets, Map<String,Double> spent) {
        this.categories = categories;
        this.spent      = new HashMap<>(spent);
        editedBudgets.putAll(budgets);
    }

    public void updateSpent(Map<String,Double> newSpent) {
        this.spent = new HashMap<>(newSpent);
        notifyItemRangeChanged(0, categories.length);
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemBudgetBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        String cat      = categories[pos];
        double budget   = editedBudgets.getOrDefault(cat, 0.0);
        double spentAmt = spent.getOrDefault(cat, 0.0);
        Context ctx     = h.itemView.getContext();

        h.binding.budgetCategory.setText(cat);

        if (spentAmt > 0) {
            h.binding.budgetSpent.setText(String.format("₹%.0f spent", spentAmt));
            h.binding.budgetSpent.setVisibility(View.VISIBLE);
        } else {
            h.binding.budgetSpent.setVisibility(View.GONE);
        }

        if (budget > 0) {
            int pct = (int) Math.min(100, (spentAmt / budget) * 100);
            h.binding.budgetProgress.setProgress(pct, true);

            // ── Progress color via MaterialColors — theme-aware ───────────
            int color;
            if (pct >= 100) {
                // colorError = over budget
                color = MaterialColors.getColor(ctx,
                        com.google.android.material.R.attr.colorError,
                        0xFFF87171);
            } else if (pct >= 75) {
                // colorTertiary = warning (amber in Midnight Blue theme)
                color = MaterialColors.getColor(ctx,
                        com.google.android.material.R.attr.colorTertiary,
                        0xFFF59E0B);
            } else {
                // colorSecondary = on track (emerald)
                color = MaterialColors.getColor(ctx,
                        com.google.android.material.R.attr.colorSecondary,
                        0xFF10B981);
            }
            h.binding.budgetProgress.setIndicatorColor(color);
            h.binding.budgetProgress.setVisibility(View.VISIBLE);
            h.binding.budgetPct.setText(pct + "%");
            h.binding.budgetPct.setVisibility(View.VISIBLE);
        } else {
            h.binding.budgetProgress.setVisibility(View.GONE);
            h.binding.budgetPct.setVisibility(View.GONE);
        }

        // Input — detach old watcher before setText to prevent feedback loop
        if (h.watcher != null) {
            h.binding.budgetInput.removeTextChangedListener(h.watcher);
        }
        h.binding.budgetInput.setText(budget > 0 ? String.valueOf((int) budget) : "");
        h.watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                try   { editedBudgets.put(cat, Double.parseDouble(s.toString())); }
                catch (NumberFormatException e) { editedBudgets.put(cat, 0.0); }
            }
        };
        h.binding.budgetInput.addTextChangedListener(h.watcher);
    }

    @Override public int getItemCount() { return categories.length; }

    public Map<String,Double> getBudgets() { return editedBudgets; }

    static class VH extends RecyclerView.ViewHolder {
        final ItemBudgetBinding binding;
        TextWatcher watcher;
        VH(ItemBudgetBinding b) { super(b.getRoot()); binding = b; }
    }
}
