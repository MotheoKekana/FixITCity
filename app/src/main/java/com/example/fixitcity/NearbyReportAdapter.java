package com.example.fixitcity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NearbyReportAdapter extends RecyclerView.Adapter<NearbyReportAdapter.ViewHolder> {

    private static final String TAG = "NearbyReportAdapter";

    private Context context;
    private List<NearbyReport> reports;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(NearbyReport report, int position);
    }

    public NearbyReportAdapter(Context context, List<NearbyReport> reports, OnItemClickListener listener) {
        this.context = context;
        this.reports = reports;
        this.listener = listener;
        Log.d(TAG, "Adapter created. Listener: " + (listener != null ? "NOT NULL" : "NULL"));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NearbyReport report = reports.get(position);

        holder.tvTitle.setText(report.getTitle());
        holder.tvVotes.setText("⬆ " + report.getVoteCount());
        holder.tvDistance.setText(String.format("%.0fm away", report.getDistance()));

        // Icon mapping
        int iconRes = android.R.drawable.ic_dialog_alert;
        String cat = report.getCategory().toLowerCase();
        if (cat.contains("pothole")) iconRes = android.R.drawable.ic_menu_report_image;
        else if (cat.contains("water")) iconRes = android.R.drawable.ic_dialog_alert;
        else if (cat.contains("drain")) iconRes = android.R.drawable.ic_menu_compass;
        else if (cat.contains("light")) iconRes = android.R.drawable.ic_menu_camera;
        holder.ivIcon.setImageResource(iconRes);

        Log.d(TAG, "Binding position " + position + ", listener: " + (listener != null ? "NOT NULL" : "NULL"));

        // ✅ Set click listener on the root itemView
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Item CLICKED at position " + position);
            if (listener != null) {
                listener.onItemClick(report, position);
            } else {
                Log.e(TAG, "Listener is NULL - cannot fire!");
            }
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvVotes, tvDistance;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvVotes = itemView.findViewById(R.id.tv_votes);
            tvDistance = itemView.findViewById(R.id.tv_distance);
        }
    }
}