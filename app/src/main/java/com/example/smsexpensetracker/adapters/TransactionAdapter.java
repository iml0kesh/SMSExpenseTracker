package com.example.smsexpensetracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smsexpensetracker.R;
import com.example.smsexpensetracker.databinding.ItemTransactionBinding;
import com.example.smsexpensetracker.models.Transaction;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.VH> {

    public interface InteractionListener {
        void onLongPress(Transaction t);
        default void onClick(Transaction t) {}
    }

    private final InteractionListener listener;
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    private static final DiffUtil.ItemCallback<Transaction> DIFF =
            new DiffUtil.ItemCallback<Transaction>() {
                public boolean areItemsTheSame(@NonNull Transaction o, @NonNull Transaction n) {
                    return o.id == n.id;
                }
                public boolean areContentsTheSame(@NonNull Transaction o, @NonNull Transaction n) {
                    return o.amount == n.amount
                        && o.type.equals(n.type)
                        && o.category.equals(n.category)
                        && o.dateMillis == n.dateMillis;
                }
            };

    public TransactionAdapter(InteractionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Transaction t = getItem(pos);
        if (t == null) return;

        Context ctx = h.itemView.getContext();

        // Sender name + icon letter
        String name = decodeSender(t.sender);
        h.b.tvIcon.setText(name.substring(0, 1).toUpperCase(Locale.getDefault()));
        h.b.tvSender.setText(name);
        h.b.tvDate.setText(SDF.format(new Date(t.dateMillis)));
        h.b.tvCategory.setText(t.category != null ? t.category : "Other");

        // Merchant line — hide if null
        if (t.merchant != null && !t.merchant.isEmpty()) {
            h.b.tvMerchant.setText(t.merchant);
            h.b.tvMerchant.setVisibility(View.VISIBLE);
        } else {
            h.b.tvMerchant.setVisibility(View.GONE);
        }

        // ── Amount color ──────────────────────────────────────────────────
        // MaterialColors.getColor() reads from the current theme at runtime,
        // so this works correctly on every device and every theme change.
        // No hardcoded hex — theme owns the colors.
        switch (t.type) {
            case "DEBIT":
                h.b.tvAmount.setText(
                        String.format(Locale.getDefault(), "-₹%.0f", t.amount));
                // colorError = coral red = debits
                h.b.tvAmount.setTextColor(
                        MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorError,
                                ctx.getColor(android.R.color.holo_red_light)));
                break;

            case "CREDIT":
                h.b.tvAmount.setText(
                        String.format(Locale.getDefault(), "+₹%.0f", t.amount));
                // colorSecondary = emerald = credits
                h.b.tvAmount.setTextColor(
                        MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSecondary,
                                ctx.getColor(android.R.color.holo_green_light)));
                break;

            default:
                h.b.tvAmount.setText(
                        String.format(Locale.getDefault(), "₹%.0f", t.amount));
                // colorOnSurfaceVariant = muted = unknown
                h.b.tvAmount.setTextColor(
                        MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant,
                                ctx.getColor(android.R.color.darker_gray)));
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(t);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress(t);
            return true;
        });
    }

    // ── TRAI sender decoder ───────────────────────────────────────────────
    public static String decodeSender(String sender) {
        if (sender == null || sender.isEmpty()) return "Unknown";
        String entity = (sender.length() >= 4 && sender.charAt(2) == '-')
                ? sender.substring(3).toUpperCase()
                : sender.toUpperCase();

        if (entity.contains("HDFCBK") || entity.contains("HDFCBL") || entity.contains("HDFC"))    return "HDFC Bank";
        if (entity.contains("ICICIB") || entity.contains("ICICI"))                                  return "ICICI Bank";
        if (entity.contains("SBIINB") || entity.contains("SBIPSG") || entity.contains("SBI"))      return "State Bank";
        if (entity.contains("AXISBK") || entity.contains("AXISBM") || entity.contains("AXIS"))     return "Axis Bank";
        if (entity.contains("KOTAKB") || entity.contains("KOTAK"))                                  return "Kotak Bank";
        if (entity.contains("PNBSMS") || entity.contains("PNJBNK") || entity.contains("PNB"))      return "PNB";
        if (entity.contains("BOBTXN") || entity.contains("BARODB") || entity.contains("BOB"))      return "Bank of Baroda";
        if (entity.contains("CANBNK") || entity.contains("CANARA"))                                return "Canara Bank";
        if (entity.contains("UNIONB") || entity.contains("UCOBNK"))                                return "Union Bank";
        if (entity.contains("INDBNK") || entity.contains("INDUS"))                                 return "IndusInd Bank";
        if (entity.contains("YESBNK") || entity.contains("YESBK"))                                 return "Yes Bank";
        if (entity.contains("IDBIBK") || entity.contains("IDBI"))                                  return "IDBI Bank";
        if (entity.contains("FEDBK")  || entity.contains("FEDERAL"))                               return "Federal Bank";
        if (entity.contains("RBLBNK") || entity.contains("RATBNK"))                                return "RBL Bank";
        if (entity.contains("AMEXIN") || entity.contains("AMEX"))                                  return "Amex";
        if (entity.contains("CITIBK") || entity.contains("CITI"))                                  return "Citibank";
        if (entity.contains("SCBNKI") || entity.contains("SCBANK"))                                return "Standard Chartered";
        if (entity.contains("HSBCIN") || entity.contains("HSBC"))                                  return "HSBC";
        if (entity.contains("PAYTMB") || entity.contains("PAYTM"))                                 return "Paytm";
        if (entity.contains("PHPEBN") || entity.contains("PHONEPE") || entity.contains("PHPBNK"))  return "PhonePe";
        if (entity.contains("GPAYBN") || entity.contains("GOOGLEPAY") || entity.contains("GPAY"))  return "Google Pay";
        if (entity.contains("CREDBN") || entity.contains("CRED"))                                  return "CRED";
        if (entity.contains("SBICARD") || entity.contains("SBICRD"))                               return "SBI Card";
        if (entity.contains("HDFCCC") || entity.contains("HDFCCD"))                                return "HDFC Credit Card";
        if (entity.contains("BAJAJF") || entity.contains("BAJFIN"))                                return "Bajaj Finserv";
        if (entity.contains("AUBNKL") || entity.contains("AUSMFB"))                                return "AU Small Finance";
        if (entity.contains("EQTSBN") || entity.contains("EQUITAS"))                               return "Equitas Bank";
        if (entity.contains("UJJIVN") || entity.contains("UJJIVAN"))                               return "Ujjivan Bank";

        return entity.isEmpty() ? sender : entity;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemTransactionBinding b;
        VH(ItemTransactionBinding b) { super(b.getRoot()); this.b = b; }
    }
}
