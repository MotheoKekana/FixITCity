package com.example.fixitcity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StatusHistoryAdapter extends RecyclerView.Adapter<StatusHistoryAdapter.ViewHolder> {

    private final List<StatusHistoryItem> items;

    public StatusHistoryAdapter(List<StatusHistoryItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_status_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StatusHistoryItem item = items.get(position);

        // "Pending → Assigned" or just "Pending" for the very first entry
        String label;
        if (item.oldStatus == null || item.oldStatus.isEmpty()) {
            label = item.newStatus != null ? item.newStatus : "—";
        } else {
            label = item.oldStatus + " → " + item.newStatus;
        }
        holder.tvStatus.setText(label);

        // Format the timestamp nicely
        String date = item.changedAt;
        if (date != null && date.length() >= 16) {
            try {
                String d = date.substring(0, 10);       // "2026-04-15"
                String t = date.substring(11, 16);      // "14:32"
                String[] parts = d.split("-");
                String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                        "Jul","Aug","Sep","Oct","Nov","Dec"};
                int monthIdx = Integer.parseInt(parts[1]) - 1;
                date = Integer.parseInt(parts[2]) + " " + months[monthIdx] + " " +
                        parts[0] + " · " + t;
            } catch (Exception ignored) {
                // Fall back to the raw string if parsing fails
            }
        }
        holder.tvDate.setText(date != null ? date : "—");

        // Optional notes line
        if (item.notes != null && !item.notes.isEmpty()) {
            holder.tvNotes.setText(item.notes);
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvDate, tvNotes;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvNotes = itemView.findViewById(R.id.tv_notes);
        }
    }
}