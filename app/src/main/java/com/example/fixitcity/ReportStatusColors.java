package com.example.fixitcity;

import android.content.res.ColorStateList;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/** Matching foreground/background pairs for report statuses in either theme. */
public final class ReportStatusColors {
    private ReportStatusColors() { }

    public static void apply(TextView badge, String status) {
        int[] pair = resources(status);
        badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(badge.getContext(), pair[0])));
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), pair[1]));
    }

    public static void applyIcon(ImageView icon, String status) {
        int[] pair = resources(status);
        icon.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(icon.getContext(), pair[0])));
        icon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(icon.getContext(), pair[1])));
    }

    private static int[] resources(String status) {
        switch (status != null ? status : "") {
            case "Pending":
            case "Pending sync":
                return new int[]{R.color.status_pending_bg, R.color.status_pending_text};
            case "Under Review":
            case "Assigned":
            case "Syncing":
                return new int[]{R.color.status_review_bg, R.color.status_review_text};
            case "Accepted":
            case "Fixed":
                return new int[]{R.color.status_success_bg, R.color.status_success_text};
            case "Rejected":
            case "Sync failed":
                return new int[]{R.color.status_error_bg, R.color.status_error_text};
            case "In Progress":
                return new int[]{R.color.status_progress_bg, R.color.status_progress_text};
            default:
                return new int[]{R.color.status_neutral_bg, R.color.status_neutral_text};
        }
    }
}
