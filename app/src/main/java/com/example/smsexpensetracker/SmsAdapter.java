package com.example.smsexpensetracker;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class SmsAdapter extends ArrayAdapter<SmsComponent> {
    public SmsAdapter(Context context, ArrayList<SmsComponent> objects) {
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_transaction, parent, false);
        }

        SmsComponent sms = getItem(position);

        TextView tvSender = convertView.findViewById(R.id.tvSender);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvAmount = convertView.findViewById(R.id.tvAmount);
        TextView tvIcon = convertView.findViewById(R.id.tvIcon);

        // Set Data
        tvSender.setText(sms.getSMSSender());
        tvDate.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a").format(sms.getSMSDate()));

        // Color coding like Google Pay
        if (sms.getAmountType().equalsIgnoreCase("DEBIT")) {
            tvAmount.setText("- ₹" + sms.getSMSAmount());
            tvAmount.setTextColor(Color.parseColor("#D32F2F")); // Red
        } else {
            tvAmount.setText("+ ₹" + sms.getSMSAmount());
            tvAmount.setTextColor(Color.parseColor("#388E3C")); // Green
        }

        // Set Icon to first letter of sender
        tvIcon.setText(sms.getSMSSender().substring(0, 1));

        return convertView;
    }
}