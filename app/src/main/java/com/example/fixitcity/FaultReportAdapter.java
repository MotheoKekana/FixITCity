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

    private Context context;
    private List<FaultReport> reports;

    public FaultReportAdapter(Context context, List<FaultReport> reports) {
        this.context = context;
        this.reports = reports;
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

        String status = report.getStatus();
        holder.tvStatus.setText(status != null ? status : "Pending");

        String created = report.getCreatedAt();
        if (created != null && created.length() >= 10) {
            // "2024-10-15T12:34:56" → "2024-10-15"
            holder.tvDate.setText(created.substring(0, 10));
        } else {
            holder.tvDate.setText("—");
        }
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