package com.example.fixitcity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackHistoryAdapter
        extends RecyclerView.Adapter<TrackHistoryAdapter.ViewHolder> {

    private static final Pattern ISO_TIMESTAMP = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2})[T ](\\d{2}:\\d{2})(?::(\\d{2}))?"
                    + "(?:\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?$"
    );

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
        String readableDate = formatReportDate(r.getCreatedAt(), r.isPendingSync());
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

    private static String formatReportDate(String value, boolean savedLocally) {
        if (value == null || value.trim().isEmpty()) return "Date unavailable";

        Matcher match = ISO_TIMESTAMP.matcher(value.trim());
        if (!match.matches()) return "Date unavailable";

        String time = match.group(2) + ":" + (match.group(3) != null ? match.group(3) : "00");
        String zone = match.group(4);
        String normalized = match.group(1) + "T" + time;
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        if (zone != null) {
            if (!"Z".equals(zone) && zone.length() == 5) {
                zone = zone.substring(0, 3) + ":" + zone.substring(3);
            }
            normalized += zone;
            pattern += "XXX";
        }

        SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.US);
        parser.setLenient(false);
        // Local pending reports were saved in phone time. Supabase's created_at
        // values in this project are UTC, even when the response omits the Z.
        if (zone == null && !savedLocally) {
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        try {
            Date date = parser.parse(normalized);
            if (date == null) return "Date unavailable";
            // Display both server UTC and phone-saved times in the phone's time zone.
            return new SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            return "Date unavailable";
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
