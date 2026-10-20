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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseNotifications;
import com.example.fixitcity.data.SupabaseReports;

import java.util.List;

public class Notifications_Activity extends AppCompatActivity {
    private static final String TAG = "Notifications";

    private LinearLayout notificationList;
    private TextView stateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppNavigation.requireCitizen(this)) return;
        setContentView(R.layout.activity_notifications);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        notificationList = findViewById(R.id.notification_list);
        stateText = findViewById(R.id.tv_notification_state);
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing() || !AppNavigation.requireCitizen(this)) return;
        loadNotifications();
    }

    private void loadNotifications() {
        SessionManager session = new SessionManager(this);
        long citizenId = session.getCitizenProfileId();
        String accessToken = session.getAccessToken();
        notificationList.removeAllViews();

        if (session.isGuest() || citizenId <= 0 || accessToken == null) {
            showState("Sign in to see your notifications.");
            return;
        }

        showState("Loading notifications…");
        new Thread(() -> {
            try {
                List<SupabaseNotifications.NotificationRow> rows =
                        SupabaseNotifications.getForCitizen(citizenId, accessToken);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()
                            || new SessionManager(this).getCitizenProfileId() != citizenId) return;
                    notificationList.removeAllViews();
                    if (rows.isEmpty()) {
                        showState("No notifications yet.");
                        return;
                    }
                    stateText.setVisibility(View.GONE);
                    for (SupabaseNotifications.NotificationRow row : rows) {
                        addNotification(row, citizenId, accessToken);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Could not load notifications", e);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()
                            || new SessionManager(this).getCitizenProfileId() != citizenId) return;
                    showState("Could not load notifications. Check your connection and reopen this screen.");
                });
            }
        }, "load-notifications").start();
    }

    private void addNotification(SupabaseNotifications.NotificationRow row,
                                 long citizenId, String accessToken) {
        View card = LayoutInflater.from(this).inflate(
                R.layout.item_notification, notificationList, false);
        TextView subject = card.findViewById(R.id.tv_notification_subject);
        TextView message = card.findViewById(R.id.tv_notification_message);
        TextView date = card.findViewById(R.id.tv_notification_date);
        TextView unread = card.findViewById(R.id.tv_notification_unread);

        subject.setText(row.subject != null ? row.subject : "Report update");
        message.setText(row.message != null ? row.message : "");
        String timestamp = row.created_at != null ? row.created_at : "";
        date.setText(timestamp.length() >= 16
                ? timestamp.substring(0, 16).replace('T', ' ') : timestamp);
        unread.setVisibility(row.is_read ? View.GONE : View.VISIBLE);

        card.setOnClickListener(v -> openNotification(row, citizenId, accessToken, unread));
        notificationList.addView(card);
    }

    private void openNotification(SupabaseNotifications.NotificationRow row,
                                  long citizenId, String accessToken, TextView unread) {
        new Thread(() -> {
            boolean markedRead = row.is_read;
            if (!markedRead) {
                try {
                    SupabaseNotifications.markRead(row.notification_id, citizenId, accessToken);
                    markedRead = true;
                } catch (Exception e) {
                    Log.w(TAG, "Could not mark notification as read", e);
                }
            }

            FaultReport report = null;
            if (row.report_id != null && row.report_id > 0) {
                try {
                    for (FaultReport candidate : SupabaseReports.getUserReports(citizenId, accessToken)) {
                        if (candidate.getReportId() == row.report_id) {
                            report = candidate;
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not open the notification's report", e);
                }
            }

            final boolean readFinal = markedRead;
            final FaultReport reportFinal = report;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()
                        || new SessionManager(this).getCitizenProfileId() != citizenId) return;
                if (readFinal) {
                    row.is_read = true;
                    unread.setVisibility(View.GONE);
                }
                if (reportFinal != null) {
                    Intent intent = new Intent(this, TrackReportResultActivity.class);
                    intent.putExtra("report_id", reportFinal.getReportId());
                    intent.putExtra("reference_number", reportFinal.getReferenceNumber());
                    intent.putExtra("title", reportFinal.getTitle());
                    intent.putExtra("description", reportFinal.getDescription());
                    intent.putExtra("address_line", reportFinal.getAddressLine());
                    intent.putExtra("status", reportFinal.getStatus());
                    intent.putExtra("priority_score", reportFinal.getPriorityScore() != null
                            ? reportFinal.getPriorityScore() : 0);
                    intent.putExtra("created_at", reportFinal.getCreatedAt());
                    startActivity(intent);
                } else if (row.report_id != null && row.report_id > 0) {
                    Toast.makeText(this, "This report is unavailable right now.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }, "open-notification").start();
    }

    private void showState(String message) {
        stateText.setText(message);
        stateText.setVisibility(View.VISIBLE);
    }

    private void setupNavigation() {
        View home = findViewById(R.id.navHome);
        if (home != null) home.setOnClickListener(v -> {
            Class<?> target = new SessionManager(this).isGuest()
                    ? Guest_Dashboard_Activity.class : Dashboard_Activity.class;
            startActivity(new Intent(this, target));
            finish();
        });
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
