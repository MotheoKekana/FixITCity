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

public class RecentReportAdapter
        extends RecyclerView.Adapter<RecentReportAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FaultReport report);
    }

    private final List<FaultReport> items;
    private final OnItemClickListener listener;

    public RecentReportAdapter(List<FaultReport> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_report, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FaultReport r = items.get(position);

        holder.tvTitle.setText(r.getTitle() != null ? r.getTitle() : "Report");
        holder.tvReference.setText(
                r.getReferenceNumber() != null ? r.getReferenceNumber() : "—");
        holder.tvDate.setText(NotificationDates.format(r.getCreatedAt()));

        int score = r.getPriorityScore() != null ? r.getPriorityScore() : 0;
        holder.tvPriority.setText("Score " + score + "/100");

        String status = r.getStatus() != null ? r.getStatus() : "Pending";
        holder.tvStatus.setText(status);
        ReportStatusColors.apply(holder.tvStatus, status);

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
