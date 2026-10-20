package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.NotificationTokenManager;
import com.example.fixitcity.data.SupabaseReports;

import java.util.List;

public class ProfileSettingsActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSettings";

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppNavigation.requireCitizen(this)) return;
        setContentView(R.layout.activity_profile_settings);

        ScreenInsets.apply(this);

        session = new SessionManager(this);

        // ===== Populate profile header from session =====
        TextView tvName = findViewById(R.id.tv_profile_name);
        TextView tvEmail = findViewById(R.id.tv_profile_email);
        TextView tvPhone = findViewById(R.id.tv_profile_phone);
        TextView tvReportsCount = findViewById(R.id.tv_profile_reports_count);

        String first = session.getFirstName();
        String last = session.getLastName();
        String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        if (fullName.isEmpty()) {
            fullName = session.isGuest() ? "Guest User" : "Citizen";
        }

        if (tvName != null) tvName.setText(fullName);
        if (tvEmail != null) {
            String email = session.getEmail();
            tvEmail.setText(email != null && !email.isEmpty() ? email : "—");
        }
        if (tvPhone != null) {
            String phone = session.getPhone();
            tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "No phone number saved");
        }

        // ===== Load real report count =====
        if (tvReportsCount != null) {
            long citizenProfileId = session.getCitizenProfileId();
            String accessToken = session.getAccessToken();

            if ((session.isGuest() && session.hasGuestAuthSession())
                    || (!session.isGuest() && citizenProfileId > 0 && accessToken != null)) {
                tvReportsCount.setText("…");
                new Thread(() -> {
                    try {
                        List<com.example.fixitcity.FaultReport> reports =
                                session.isGuest()
                                        ? SupabaseReports.getGuestReports(session.getUserId(), accessToken)
                                        : SupabaseReports.getUserReports(citizenProfileId, accessToken);
                        int count = (reports != null) ? reports.size() : 0;
                        runOnUiThread(() -> tvReportsCount.setText(String.valueOf(count)));
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to load report count: " + e.getMessage(), e);
                        runOnUiThread(() -> tvReportsCount.setText("—"));
                    }
                }).start();
            } else {
                tvReportsCount.setText("—");
            }
        }

        bindSettingsRow(R.id.settingsReportHistory, R.drawable.ic_report_24,
                "Report history", "View your submitted and pending reports",
                v -> startActivity(new Intent(this, ReportHistoryActivity.class)));

        bindSettingsRow(R.id.settingsTrackReport, R.drawable.ic_track_24,
                "Track a report", "Look up a report by its reference number",
                v -> startActivity(new Intent(this, TrackReportSearchActivity.class)));

        bindSettingsRow(R.id.settingsNotifications, R.drawable.ic_bell_outline_24,
                "Phone notifications", "Manage FixITCity notification permission",
                v -> {
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                });
        if (session.isGuest()) findViewById(R.id.settingsNotifications).setVisibility(View.GONE);

        // ===== Logout button =====
        View logoutBtn = findViewById(R.id.btnLogout);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                if (session.isGuest()) {
                    Intent intent = new Intent(this, Guest_Dashboard_Activity.class);
                    intent.putExtra("request_guest_logout", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return;
                }
                NotificationTokenManager.deactivateOnLogout(
                        getApplicationContext(), session.getUserIdBigint(), session.getAccessToken());
                session.logout();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Login_Activity.class));
                finishAffinity();
            });
        }

        // ===== Back button (if it exists) =====
        View backBtn = findViewById(R.id.back_button);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        // ===== Bottom navigation =====
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Class<?> target = session.isGuest()
                        ? Guest_Dashboard_Activity.class
                        : Dashboard_Activity.class;
                startActivity(new Intent(this, target));
                finish();
            });
        }

        View navReport = findViewById(R.id.navReport);
        if (navReport != null) {
            navReport.setOnClickListener(v -> {
                startActivity(new Intent(this, ReportFaultCategoryActivity.class));
                finish();
            });
        }

        View navTrack = findViewById(R.id.navTrack);
        if (navTrack != null) {
            navTrack.setOnClickListener(v ->
                    startActivity(new Intent(this, TrackReportSearchActivity.class)));
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    Toast.makeText(this, "You're already in Settings", Toast.LENGTH_SHORT).show());
        }

    }

    private void bindSettingsRow(int rowId, int iconRes, String title,
                                 String subtitle, View.OnClickListener action) {
        View row = findViewById(rowId);
        ((ImageView) row.findViewById(R.id.iv_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.tv_title)).setText(title);
        ((TextView) row.findViewById(R.id.tv_subtitle)).setText(subtitle);
        row.setContentDescription(title + ". " + subtitle);
        row.setOnClickListener(action);
    }
}
