package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.NotificationDates;
import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SessionRefresher;
import com.example.fixitcity.data.SupabaseNotifications;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;

public class Notifications_Activity extends AppCompatActivity {
    private static final String TAG = "Notifications";
    private LinearLayout notificationList;
    private TextView stateText;
    private MaterialButton refreshButton;
    private int generation;
    private boolean resumed;
    private boolean loading;
    private boolean opening;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppNavigation.requireCitizen(this)) return;
        setContentView(R.layout.activity_notifications);
        ScreenInsets.apply(this);
        notificationList = findViewById(R.id.notification_list);
        stateText = findViewById(R.id.tv_notification_state);
        refreshButton = findViewById(R.id.btn_refresh_notifications);
        refreshButton.setOnClickListener(v -> loadNotifications());
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing() || !AppNavigation.requireCitizen(this)) return;
        resumed = true;
        opening = false;
        loadNotifications();
    }

    @Override
    protected void onPause() {
        resumed = false;
        generation++;
        super.onPause();
    }

    private boolean sameAccount(long citizenId, String authId) {
        SessionManager current = new SessionManager(this);
        return current.isLoggedIn() && !current.isGuest()
                && current.getCitizenProfileId() == citizenId
                && authId != null && authId.equals(current.getUserId());
    }

    private boolean canUpdate(int request, long citizenId, String authId) {
        return resumed && !isFinishing() && !isDestroyed()
                && generation == request && sameAccount(citizenId, authId);
    }

    private String freshAccessToken(long citizenId, String authId) throws IOException {
        if (!sameAccount(citizenId, authId)) throw new IOException("Account changed");
        SessionManager session = new SessionManager(getApplicationContext());
        SessionRefresher.Result result = SessionRefresher.refresh(session);
        if (!sameAccount(citizenId, authId)) throw new IOException("Account changed");
        if (result == SessionRefresher.Result.INVALID_SESSION) {
            throw new IOException("Session expired");
        }
        String token = session.getAccessToken();
        if (token == null || token.trim().isEmpty()) throw new IOException("Session expired");
        return token;
    }

    private void loadNotifications() {
        if (opening) return;
        SessionManager session = new SessionManager(this);
        long citizenId = session.getCitizenProfileId();
        String authId = session.getUserId();
        int request = ++generation;
        if (citizenId <= 0 || !sameAccount(citizenId, authId)) {
            notificationList.removeAllViews();
            showState("Sign in to see your notifications.");
            loading = false;
            updateRefreshButton();
            return;
        }
        loading = true;
        updateRefreshButton();
        showState("Loading notifications…");
        new Thread(() -> {
            try {
                String token = freshAccessToken(citizenId, authId);
                List<SupabaseNotifications.NotificationRow> rows =
                        SupabaseNotifications.getForCitizen(citizenId, token);
                Log.d(TAG, "Loaded " + rows.size() + " notification(s)");
                runOnUiThread(() -> {
                    if (!canUpdate(request, citizenId, authId)) return;
                    loading = false;
                    updateRefreshButton();
                    notificationList.removeAllViews();
                    if (rows.isEmpty()) {
                        showState("No notifications yet. Updates to your reports will appear here.");
                        return;
                    }
                    stateText.setVisibility(View.GONE);
                    for (SupabaseNotifications.NotificationRow row : rows) {
                        addNotification(row, citizenId, authId);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Could not load notifications", e);
                runOnUiThread(() -> {
                    if (!canUpdate(request, citizenId, authId)) return;
                    loading = false;
                    updateRefreshButton();
                    showState(isExpiredSession(e)
                            ? "Your session expired. Please sign out and sign in again."
                            : "Could not refresh notifications. Tap Refresh to try again.");
                });
            }
        }, "load-notifications").start();
    }

    private void addNotification(SupabaseNotifications.NotificationRow row,
                                 long citizenId, String authId) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_notification, notificationList, false);
        TextView subject = card.findViewById(R.id.tv_notification_subject);
        TextView message = card.findViewById(R.id.tv_notification_message);
        TextView date = card.findViewById(R.id.tv_notification_date);
        TextView unread = card.findViewById(R.id.tv_notification_unread);
        subject.setText(row.subject != null ? row.subject : "Report update");
        message.setText(row.message != null ? row.message : "");
        date.setText(NotificationDates.format(row.created_at));
        unread.setVisibility(row.is_read ? View.GONE : View.VISIBLE);
        card.setOnClickListener(v -> openNotification(row, citizenId, authId, unread));
        notificationList.addView(card);
    }

    private void openNotification(SupabaseNotifications.NotificationRow row,
                                  long citizenId, String authId, TextView unread) {
        if (opening || loading || !sameAccount(citizenId, authId)) return;
        opening = true;
        updateRefreshButton();
        int request = generation;
        new Thread(() -> {
            boolean markedRead = row.is_read;
            FaultReport report = null;
            String error = null;
            try {
                String token = freshAccessToken(citizenId, authId);
                if (!markedRead) {
                    try {
                        SupabaseNotifications.markRead(row.notification_id, citizenId, token);
                        markedRead = true;
                        Log.d(TAG, "Notification marked as read");
                    } catch (Exception e) {
                        Log.w(TAG, "Could not mark notification as read", e);
                        error = "Could not save the read status. Please try again later.";
                    }
                }
                if (row.report_id != null && row.report_id > 0) {
                    if (!sameAccount(citizenId, authId)) throw new IOException("Account changed");
                    report = SupabaseNotifications.getReportForCitizen(row.report_id, citizenId, token);
                    if (report == null) error = "This report is unavailable right now.";
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not open notification", e);
                error = isExpiredSession(e)
                        ? "Your session expired. Please sign out and sign in again."
                        : "Could not open the report. Please try again.";
            }
            final boolean readFinal = markedRead;
            final FaultReport reportFinal = report;
            final String errorFinal = error;
            runOnUiThread(() -> {
                if (!canUpdate(request, citizenId, authId)) return;
                opening = false;
                updateRefreshButton();
                if (readFinal) {
                    row.is_read = true;
                    unread.setVisibility(View.GONE);
                }
                if (errorFinal != null) Toast.makeText(this, errorFinal, Toast.LENGTH_LONG).show();
                if (reportFinal != null) {
                    Intent intent = new Intent(this, TrackReportResultActivity.class);
                    intent.putExtra("report_id", reportFinal.getReportId());
                    intent.putExtra("reference_number", reportFinal.getReferenceNumber());
                    intent.putExtra("title", reportFinal.getTitle());
                    intent.putExtra("description", reportFinal.getDescription());
                    intent.putExtra("address_line", reportFinal.getAddressLine());
                    intent.putExtra("status", reportFinal.getStatus());
                    if (reportFinal.getPriorityScore() != null) {
                        intent.putExtra("priority_score", reportFinal.getPriorityScore());
                    }
                    intent.putExtra("created_at", reportFinal.getCreatedAt());
                    startActivity(intent);
                }
            });
        }, "open-notification").start();
    }

    private boolean isExpiredSession(Exception error) {
        String message = error.getMessage();
        return message != null && (message.equals("Session expired") || message.contains("HTTP 401"));
    }

    private void updateRefreshButton() {
        refreshButton.setEnabled(!loading && !opening);
    }

    private void showState(String message) {
        stateText.setText(message);
        stateText.setVisibility(View.VISIBLE);
    }

    private void setupNavigation() {
        View home = findViewById(R.id.navHome);
        if (home != null) home.setOnClickListener(v -> AppNavigation.goHome(this));
        View report = findViewById(R.id.navReport);
        if (report != null) report.setOnClickListener(v -> {
            startActivity(new Intent(this, ReportFaultCategoryActivity.class));
            finish();
        });
        View track = findViewById(R.id.navTrack);
        if (track != null) track.setOnClickListener(v -> {
            startActivity(new Intent(this, TrackReportSearchActivity.class));
            finish();
        });
        View settings = findViewById(R.id.navSettings);
        if (settings != null) settings.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileSettingsActivity.class));
            finish();
        });
    }
}
