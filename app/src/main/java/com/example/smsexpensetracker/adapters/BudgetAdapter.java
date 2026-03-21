package com.example.smsexpensetracker.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smsexpensetracker.databinding.ItemBudgetBinding;

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

    /** Update spent amounts without rebuilding the adapter — preserves user edits */
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

        h.binding.budgetCategory.setText(cat);

        // Spent label
        if (spentAmt > 0) {
            h.binding.budgetSpent.setText(String.format("₹%.0f spent", spentAmt));
            h.binding.budgetSpent.setVisibility(View.VISIBLE);
        } else {
            h.binding.budgetSpent.setVisibility(View.GONE);
        }

        // Progress bar
        if (budget > 0) {
            int pct = (int) Math.min(100, (spentAmt / budget) * 100);
            h.binding.budgetProgress.setProgress(pct, true);
            int color;
            if      (pct >= 100) color = 0xFFF87171;
            else if (pct >= 75)  color = 0xFFFBBF24;
            else                  color = 0xFF34D399;
            h.binding.budgetProgress.setIndicatorColor(color);
            h.binding.budgetProgress.setVisibility(View.VISIBLE);
            h.binding.budgetPct.setText(pct + "%");
            h.binding.budgetPct.setVisibility(View.VISIBLE);
        } else {
            h.binding.budgetProgress.setVisibility(View.GONE);
            h.binding.budgetPct.setVisibility(View.GONE);
        }

        // Detach old TextWatcher before changing text, then attach new one
        if (h.watcher != null) {
            h.binding.budgetInput.removeTextChangedListener(h.watcher);
        }

        h.binding.budgetInput.setText(budget > 0 ? String.valueOf((int) budget) : "");

        h.watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    editedBudgets.put(cat, Double.parseDouble(s.toString()));
                } catch (NumberFormatException e) {
                    editedBudgets.put(cat, 0.0);
                }
            }
        };
        h.binding.budgetInput.addTextChangedListener(h.watcher);
    }

    @Override
    public int getItemCount() { return categories.length; }

    public Map<String,Double> getBudgets() { return editedBudgets; }

    static class VH extends RecyclerView.ViewHolder {
        final ItemBudgetBinding binding;
        TextWatcher watcher; // typed as TextWatcher so removeTextChangedListener works

        VH(ItemBudgetBinding b) {
            super(b.getRoot());
            this.binding = b;
        }
    }
}
