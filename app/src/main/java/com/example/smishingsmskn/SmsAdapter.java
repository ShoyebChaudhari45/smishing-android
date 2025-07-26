package com.example.smishingsmskn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.ViewHolder> {

    private List<SmsResult> smsList;

    public SmsAdapter(List<SmsResult> smsList) {
        this.smsList = smsList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, resultText;

        public ViewHolder(View view) {
            super(view);
            messageText = view.findViewById(R.id.smsMessage);
            resultText = view.findViewById(R.id.smsStatus);
        }
    }

    @NonNull
    @Override
    public SmsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sms_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmsResult sms = smsList.get(position);
        holder.messageText.setText(sms.getMessage());
        holder.resultText.setText(sms.getResult());

        String resultText = sms.getResult().toLowerCase();

        if (resultText.contains("spam") && resultText.startsWith("spam")) {
            // Spam
            holder.resultText.setBackgroundResource(R.drawable.bg_spam);
            holder.resultText.setTextColor(0xFFD32F2F); // Dark red
        } else {
            // Not spam
            holder.resultText.setBackgroundResource(R.drawable.bg_not_spam);
            holder.resultText.setTextColor(0xFF388E3C); // Dark green
        }
    }


    @Override
    public int getItemCount() {
        return smsList.size();
    }
    public void setSmsList(List<SmsResult> newList) {
        this.smsList = newList;
        notifyDataSetChanged();
    }

}
