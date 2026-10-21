package com.example.fixitcity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import java.util.Locale;

import com.example.fixitcity.data.SessionManager;

public final class AppNavigation {
    private AppNavigation() {}

    public static void goHome(Activity activity) {
        SessionManager session = new SessionManager(activity);
        Class<?> target;
        if (!session.isLoggedIn() || (session.isGuest() && !session.hasGuestAuthSession())) {
            target = Login_Activity.class;
        } else {
            target = session.isGuest() ? Guest_Dashboard_Activity.class : Dashboard_Activity.class;
        }
        Intent intent = new Intent(activity, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    // Check the session even when an old back stack opens a citizen screen.
    public static boolean requireCitizen(Activity activity) {
        SessionManager session = new SessionManager(activity);
        if (session.isLoggedIn() && !session.isGuest()) return true;
        goHome(activity);
        return false;
    }

    public static boolean requireGuestSession(Activity activity) {
        if (new SessionManager(activity).hasGuestAuthSession()) return true;
        goHome(activity);
        return false;
    }

    public static void bindNotificationBell(Activity activity) {
        View bell = activity.findViewById(R.id.ivNotifications);
        if (bell == null) return;
        if (new SessionManager(activity).isGuest()) {
            bell.setVisibility(View.GONE);
        } else {
            bell.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, Notifications_Activity.class)));
        }
    }

    public static void openNearbySummary(Activity activity, NearbyReport report) {
        Intent intent = new Intent(activity, TrackReportResultActivity.class);
        intent.putExtra("report_id", report.getReportId());
        intent.putExtra("reference_number", report.getReferenceNumber());
        intent.putExtra("title", report.getTitle());
        String distance = report.getDistance() < 1000
                ? String.format(Locale.US, "About %.0f m away", report.getDistance())
                : String.format(Locale.US, "About %.1f km away", report.getDistance() / 1000.0);
        intent.putExtra("address_line", distance);
        intent.putExtra("status", report.getStatus());
        intent.putExtra("priority_score", report.getPriorityScore());
        intent.putExtra("summary_only", true);
        activity.startActivity(intent);
    }
}
