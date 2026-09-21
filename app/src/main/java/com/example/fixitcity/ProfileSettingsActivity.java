package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;

import java.util.List;

public class ProfileSettingsActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSettings";

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

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
            tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "—");
        }

        // ===== Load real report count =====
        if (tvReportsCount != null) {
            long citizenProfileId = session.getCitizenProfileId();
            String accessToken = session.getAccessToken();

            if (!session.isGuest() && citizenProfileId > 0 && accessToken != null) {
                tvReportsCount.setText("…");
                new Thread(() -> {
                    try {
                        List<com.example.fixitcity.FaultReport> reports =
                                SupabaseReports.getUserReports(citizenProfileId, accessToken);
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

        // ===== Logout button =====
        View logoutBtn = findViewById(R.id.btnLogout);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
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
        // Home routes to the correct dashboard depending on guest status
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

        // ===== Notification bell =====
        View ivNotif = findViewById(R.id.ivNotifications);
        if (ivNotif != null) {
            ivNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, Notifications_Activity.class)));
        }
    }
}