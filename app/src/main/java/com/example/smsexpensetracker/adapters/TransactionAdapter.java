package com.example.smsexpensetracker.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smsexpensetracker.R;
import com.example.smsexpensetracker.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {

    public interface OnLongPress { void onLongPress(Transaction t); }

    private List<Transaction> data;
    private final OnLongPress listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public TransactionAdapter(List<Transaction> data, OnLongPress listener) {
        this.data     = data;
        this.listener = listener;
    }

    public void setData(List<Transaction> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_transaction, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Transaction t = data.get(pos);

        // Icon letter
        String senderDisplay = friendlyName(t.sender);
        h.tvIcon.setText(senderDisplay.substring(0, 1).toUpperCase());
        h.tvSender.setText(senderDisplay);
        h.tvDate.setText(sdf.format(new Date(t.dateMillis)));
        h.tvCategory.setText(t.category != null ? t.category : "Other");

        // Amount with color
        if ("DEBIT".equals(t.type)) {
            h.tvAmount.setText(String.format(Locale.getDefault(), "-₹%.0f", t.amount));
            h.tvAmount.setTextColor(Color.parseColor("#D32F2F"));
        } else if ("CREDIT".equals(t.type)) {
            h.tvAmount.setText(String.format(Locale.getDefault(), "+₹%.0f", t.amount));
            h.tvAmount.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            h.tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", t.amount));
            h.tvAmount.setTextColor(Color.parseColor("#757575"));
        }

        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress(t);
            return true;
        });
    }

    @Override public int getItemCount() { return data != null ? data.size() : 0; }

    private String friendlyName(String s) {
        if (s == null) return "?";
        String u = s.toUpperCase();
        if (u.contains("HDFC"))     return "HDFC Bank";
        if (u.contains("ICICI"))    return "ICICI Bank";
        if (u.contains("SBI"))      return "State Bank";
        if (u.contains("AXIS"))     return "Axis Bank";
        if (u.contains("KOTAK"))    return "Kotak Bank";
        if (u.contains("PAYTM"))    return "Paytm";
        if (u.contains("PHONEPE"))  return "PhonePe";
        if (u.contains("PNB"))      return "PNB";
        if (u.contains("BOB"))      return "Bank of Baroda";
        if (u.contains("INDUSIND")) return "IndusInd";
        if (u.contains("YESBANK"))  return "Yes Bank";
        if (u.contains("AMEX"))     return "Amex";
        if (u.contains("CITI"))     return "Citibank";
        return s;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvSender, tvDate, tvAmount, tvCategory;
        VH(View v) {
            super(v);
            tvIcon     = v.findViewById(R.id.tvIcon);
            tvSender   = v.findViewById(R.id.tvSender);
            tvDate     = v.findViewById(R.id.tvDate);
            tvAmount   = v.findViewById(R.id.tvAmount);
            tvCategory = v.findViewById(R.id.tvCategory);
        }
    }
}
