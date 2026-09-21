package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;

import java.util.ArrayList;
import java.util.List;

public class Dashboard_Activity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private TextView tvWelcome, tvSubtitle, tvReportCount, tvNearbyRadius;
    private RecyclerView rvRecentReports;
    private FaultReportAdapter adapter;
    private List<FaultReport> recentReports = new ArrayList<>();

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);

        // Redirect if not properly logged in
        if (session.isGuest() || !session.isLoggedIn() || session.getCitizenProfileId() <= 0) {
            Toast.makeText(this, "Please log in to view your dashboard", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, Login_Activity.class));
            finish();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Views
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvReportCount = findViewById(R.id.tvReportCount);
        tvNearbyRadius = findViewById(R.id.tvNearbyRadius);
        rvRecentReports = findViewById(R.id.rvRecentReports);

        // Welcome text
        String firstName = session.getFirstName();
        if (firstName == null || firstName.isEmpty()) firstName = "there";
        tvWelcome.setText("Hello, " + firstName);
        tvSubtitle.setText("View your recent reports, track progress, and see nearby issues");

        // Recycler
        rvRecentReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FaultReportAdapter(this, recentReports);
        rvRecentReports.setAdapter(adapter);

        // Nearby radius label (map card is a placeholder for now)
        tvNearbyRadius.setText("Reports within a 200 m radius of your current location");

        // Notifications bell
        ImageView ivNotif = findViewById(R.id.ivNotifications);
        if (ivNotif != null) {
            ivNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, Notifications_Activity.class)));
        }

        // Quick action: Track Report
        View cardTrack = findViewById(R.id.cardTrackReport);
        if (cardTrack != null) {
            cardTrack.setOnClickListener(v ->
                    startActivity(new Intent(this, TrackReportSearchActivity.class)));
        }

        // Bottom nav
        setupBottomNav();

        // Load reports
        loadMyReports();
    }

    private void setupBottomNav() {
        // Home is current screen — no-op
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v ->
                    Toast.makeText(this, "You're on Home", Toast.LENGTH_SHORT).show());
        }

        View navReport = findViewById(R.id.navReport);
        if (navReport != null) {
            navReport.setOnClickListener(v ->
                    startActivity(new Intent(this, ReportFaultCategoryActivity.class)));
        }

        View navTrack = findViewById(R.id.navTrack);
        if (navTrack != null) {
            navTrack.setOnClickListener(v ->
                    startActivity(new Intent(this, TrackReportSearchActivity.class)));
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileSettingsActivity.class)));
        }
    }

    private void loadMyReports() {
        long citizenProfileId = session.getCitizenProfileId();
        String accessToken = session.getAccessToken();

        if (citizenProfileId <= 0 || accessToken == null) {
            tvReportCount.setText("No reports yet");
            return;
        }

        tvReportCount.setText("Loading...");

        new Thread(() -> {
            try {
                List<FaultReport> reports = SupabaseReports.getUserReports(citizenProfileId, accessToken);
                if (reports == null) reports = new ArrayList<>();

                final List<FaultReport> finalReports = reports;
                runOnUiThread(() -> {
                    recentReports.clear();
                    recentReports.addAll(finalReports);
                    adapter.notifyDataSetChanged();

                    tvReportCount.setText(finalReports.size() + " report" + (finalReports.size() == 1 ? "" : "s"));
                    Log.d(TAG, "Loaded " + finalReports.size() + " reports");
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load reports: " + e.getMessage(), e);
                runOnUiThread(() -> tvReportCount.setText("Could not load reports"));
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning from report submission
        loadMyReports();
    }
}