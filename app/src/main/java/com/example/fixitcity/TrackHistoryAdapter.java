package com.example.fixitcity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitcity.data.NotificationDates;

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
        String readableDate = NotificationDates.format(r.getCreatedAt());
        holder.tvDate.setText(readableDate);
        holder.tvDate.setContentDescription((r.isPendingSync() ? "Saved on " : "Submitted on ")
                + readableDate);

        // Priority
        if (r.isPendingSync()) {
            holder.tvPriority.setText("Score after sync");
        } else if (r.getPriorityScore() != null) {
            holder.tvPriority.setText("Score " + r.getPriorityScore() + "/100");
        } else {
            holder.tvPriority.setText("Calculating score");
        }

        // Status badge text + colour
        String status = r.getStatus() != null ? r.getStatus() : "Pending";
        holder.tvStatus.setText(status);
        ReportStatusColors.apply(holder.tvStatus, status);

        // Icon tint (optional — keeps default icon but colours per status)
        ReportStatusColors.applyIcon(holder.ivIcon, status);

        // Row click → open details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
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
