package com.example.fixitcity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FaultReportAdapter extends RecyclerView.Adapter<FaultReportAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FaultReport report);
    }

    private Context context;
    private List<FaultReport> reports;
    private OnItemClickListener listener;

    public FaultReportAdapter(Context context, List<FaultReport> reports) {
        this.context = context;
        this.reports = reports;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_track_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FaultReport report = reports.get(position);

        String ref = report.getReferenceNumber();
        holder.tvReference.setText(ref != null ? ref : "#----");

        // Status text: "⏳ Pending sync" for queued rows, normal status otherwise
        if (report.isPendingSync()) {
            holder.tvStatus.setText("⏳ Pending sync");
        } else {
            String status = report.getStatus();
            holder.tvStatus.setText(status != null ? status : "Pending");
        }

        ReportStatusColors.apply(holder.tvStatus,
                report.isPendingSync() ? "Pending sync" : report.getStatus());

        String created = report.getCreatedAt();
        if (created != null && created.length() >= 10) {
            holder.tvDate.setText(created.substring(0, 10));
        } else {
            holder.tvDate.setText("—");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(report);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReference, tvStatus, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReference = itemView.findViewById(R.id.tv_reference);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDate = itemView.findViewById(R.id.tv_date);
        }
    }
}