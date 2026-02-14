package com.example.smsexpensetracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SenderAdapter extends ArrayAdapter<SenderInfo> {

    private final List<SenderInfo> originalList;
    private List<SenderInfo> filteredList;
    private final LayoutInflater inflater;
    private final Set<String> currentSelections;

    public SenderAdapter(Context context, List<SenderInfo> list, Set<String> selections) {
        super(context, R.layout.sender_list_item, list);
        this.originalList = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
        this.inflater = LayoutInflater.from(context);
        this.currentSelections = selections; // The reliable set of selected addresses
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Nullable
    @Override
    public SenderInfo getItem(int position) {
        return filteredList.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.sender_list_item, parent, false);
            holder = new ViewHolder();
            holder.senderName = convertView.findViewById(R.id.senderName);
            holder.latestMessage = convertView.findViewById(R.id.latestMessage);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SenderInfo info = getItem(position);
        if (info != null) {
            holder.senderName.setText(info.getAddress());
            holder.latestMessage.setText(info.getLatestMessage());
            // **THE FIX:** The checkmark is now based on our reliable Set, not the ListView's state.
            holder.senderName.setChecked(currentSelections.contains(info.getAddress()));
        }

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<SenderInfo> filteredItems = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filteredItems.addAll(originalList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (SenderInfo item : originalList) {
                        if (item.getAddress().toLowerCase().contains(filterPattern)) {
                            filteredItems.add(item);
                        }
                    }
                }
                results.values = filteredItems;
                results.count = filteredItems.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, @NonNull FilterResults results) {
                filteredList.clear();
                if (results.values != null) {
                    filteredList.addAll((List) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }

    private static class ViewHolder {
        CheckedTextView senderName;
        TextView latestMessage;
    }
}
