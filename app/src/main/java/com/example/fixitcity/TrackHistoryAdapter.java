package com.example.fixitcity;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrackHistoryAdapter
        extends RecyclerView.Adapter<TrackHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FaultReport report);
    }

    private final List<FaultReport> items;
    private final OnItemClickListener listener;

    public TrackHistoryAdapter(List<FaultReport> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FaultReport r = items.get(position);

        // Title
        holder.tvTitle.setText(r.getTitle() != null ? r.getTitle() : "Report");

        // Reference
        holder.tvReference.setText(
                r.getReferenceNumber() != null ? r.getReferenceNumber() : "—");

        // Date
        holder.tvDate.setText("📅 " + (r.getCreatedAt() != null ? r.getCreatedAt() : "—"));

        // Priority
        int score = r.getPriorityScore() != null ? r.getPriorityScore() : 0;
        holder.tvPriority.setText("Score " + score + "/100");

        // Status badge text + colour
        String status = r.getStatus() != null ? r.getStatus() : "Pending";
        holder.tvStatus.setText(status);
        holder.tvStatus.setBackgroundTintList(
                ColorStateList.valueOf(colorForStatus(status)));

        // Icon tint (optional — keeps default icon but colours per status)
        holder.ivIcon.setBackgroundTintList(
                ColorStateList.valueOf(colorForStatus(status)));

        // Row click → open details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int colorForStatus(String status) {
        switch (status) {
            case "Pending":      return 0xFFFFA500;  // orange
            case "Under Review": return 0xFF2196F3;  // blue
            case "Accepted":     return 0xFF4CAF50;  // green
            case "Rejected":     return 0xFFF44336;  // red
            case "In Progress":  return 0xFF9C27B0;  // purple
            case "Fixed":        return 0xFF4CAF50;  // green
            default:             return 0xFF9E9E9E;  // grey
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvStatus, tvReference, tvDate, tvPriority;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon      = itemView.findViewById(R.id.iv_icon);
            tvTitle     = itemView.findViewById(R.id.tv_title);
            tvStatus    = itemView.findViewById(R.id.tv_status);
            tvReference = itemView.findViewById(R.id.tv_reference);
            tvDate      = itemView.findViewById(R.id.tv_date);
            tvPriority  = itemView.findViewById(R.id.tv_priority);
        }
    }
}