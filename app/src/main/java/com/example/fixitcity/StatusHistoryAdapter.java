package com.example.fixitcity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

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

        // ----- Date (left column) -----
        holder.tvDate.setText(formatDate(item.changedAt));

        // ----- Description (right column) -----
        holder.tvDescription.setText(buildDescription(item));

        // ----- Timeline rail cosmetics -----
        boolean isFirst = position == 0;
        boolean isLast  = position == getItemCount() - 1;

        // Hide line above the first dot and below the last dot so it doesn't
        // extend past the ends of the list.
        ViewGroup.MarginLayoutParams lineLp =
                (ViewGroup.MarginLayoutParams) holder.vLine.getLayoutParams();
        lineLp.topMargin    = isFirst ? dpToPx(holder.itemView, 23) : 0;
        lineLp.bottomMargin = isLast  ? dpToPx(holder.itemView, 9999) : 0;
        holder.vLine.setLayoutParams(lineLp);

        // Emphasise the newest event (position 0) with a slightly bigger dot.
        float dotSizeDp = isFirst ? 18f : 14f;
        int   dotSizePx = dpToPx(holder.itemView, (int) dotSizeDp);
        ViewGroup.LayoutParams dotLp = holder.vDot.getLayoutParams();
        dotLp.width  = dotSizePx;
        dotLp.height = dotSizePx;
        holder.vDot.setLayoutParams(dotLp);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * "Pending" for the very first event, otherwise "Old → New".
     * If notes are present, they're appended on a new line.
     */
    private String buildDescription(StatusHistoryItem item) {
        StringBuilder sb = new StringBuilder();

        if (item.oldStatus == null || item.oldStatus.isEmpty()) {
            sb.append(item.newStatus != null ? item.newStatus : "—");
        } else {
            sb.append(item.oldStatus)
                    .append(" → ")
                    .append(item.newStatus != null ? item.newStatus : "—");
        }

        if (item.notes != null && !item.notes.trim().isEmpty()) {
            sb.append("\n").append(item.notes.trim());
        }

        return sb.toString();
    }

    /**
     * Accepts Supabase timestamps in a few common shapes and returns
     * a friendly "16 Sep 2026\n15:37" string. Falls back to the raw value.
     */
    private String formatDate(String raw) {
        if (raw == null || raw.length() < 16) return raw != null ? raw : "—";

        try {
            // Expect "YYYY-MM-DDTHH:MM..." or "YYYY-MM-DD HH:MM..."
            String datePart = raw.substring(0, 10);
            String timePart = raw.substring(11, 16);

            String[] ymd = datePart.split("-");
            int year  = Integer.parseInt(ymd[0]);
            int month = Integer.parseInt(ymd[1]);
            int day   = Integer.parseInt(ymd[2]);

            String[] months = {
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            };

            String monthName = (month >= 1 && month <= 12)
                    ? months[month - 1]
                    : String.format(Locale.ROOT, "%02d", month);

            return day + " " + monthName + " " + year + "\n" + timePart;

        } catch (Exception e) {
            return raw;
        }
    }

    private static int dpToPx(View v, int dp) {
        float density = v.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ------------------------------------------------------------------

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvDescription;
        View     vDot;
        View     vLine;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate        = itemView.findViewById(R.id.tv_history_date);
            tvDescription = itemView.findViewById(R.id.tv_history_description);
            vDot          = itemView.findViewById(R.id.v_timeline_dot);
            vLine         = itemView.findViewById(R.id.v_timeline_line);
        }
    }
}