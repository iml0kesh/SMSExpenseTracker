package com.example.smsexpensetracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smsexpensetracker.databinding.ItemSenderPickBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SenderPickerAdapter extends RecyclerView.Adapter<SenderPickerAdapter.VH> {

    private final List<String[]> allSenders;   // full unfiltered list
    private List<String[]> displayed;           // filtered list shown in RecyclerView
    private final Set<String> selected;

    public SenderPickerAdapter(List<String[]> senders, Set<String> preSelected) {
        this.allSenders = new ArrayList<>(senders);
        this.displayed  = new ArrayList<>(senders);
        this.selected   = new HashSet<>(preSelected);
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            displayed = new ArrayList<>(allSenders);
        } else {
            String q = query.toLowerCase().trim();
            List<String[]> filtered = new ArrayList<>();
            for (String[] item : allSenders) {
                if (item[0].toLowerCase().contains(q) ||
                    (item.length > 1 && item[1].toLowerCase().contains(q))) {
                    filtered.add(item);
                }
            }
            displayed = filtered;
        }
        notifyDataSetChanged();
    }

    public void addManualSender(String address, String preview) {
        for (String[] item : allSenders) {
            if (item[0].equalsIgnoreCase(address)) {
                selected.add(item[0]);
                notifyDataSetChanged();
                return;
            }
        }
        String[] newItem = {address, preview != null ? preview : "Manually added"};
        allSenders.add(0, newItem);
        displayed.add(0, newItem);
        selected.add(address);
        notifyDataSetChanged();
    }

    public void replaceSenders(List<String[]> newList) {
        allSenders.clear();
        allSenders.addAll(newList);
        displayed = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        ItemSenderPickBinding binding = ItemSenderPickBinding.inflate(LayoutInflater.from(p.getContext()), p, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        String[] item    = displayed.get(pos);
        String   addr    = item[0];
        String   preview = item.length > 1 ? item[1] : "";

        h.binding.senderAddress.setText(addr);
        h.binding.senderPreview.setText(preview.length() > 90 ? preview.substring(0, 90) + "…" : preview);
        h.binding.senderCheckbox.setChecked(selected.contains(addr));

        View.OnClickListener toggle = v -> {
            if (selected.contains(addr)) selected.remove(addr);
            else selected.add(addr);
            h.binding.senderCheckbox.setChecked(selected.contains(addr));
        };
        h.itemView.setOnClickListener(toggle);
        h.binding.senderCheckbox.setOnClickListener(toggle);
    }

    @Override public int getItemCount() { return displayed.size(); }

    public Set<String> getSelected() { return selected; }
    public int getTotalCount()       { return allSenders.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ItemSenderPickBinding binding;

        VH(ItemSenderPickBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
