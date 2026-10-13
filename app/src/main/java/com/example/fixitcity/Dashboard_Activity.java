package com.example.fixitcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dashboard_Activity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final int REQUEST_NOTIF_PERMISSION = 200;
    private static final int REQUEST_LOCATION_PERMISSION = 201;

    private static final double PROXIMITY_RADIUS_M = 2000;
    private static final int NEARBY_PREVIEW_LIMIT = 5;
    private static final int RECENT_PREVIEW_LIMIT = 5;   // max recent cards shown

    private TextView tvWelcome, tvSubtitle, tvReportCount, tvNearbyRadius, tvSeeMoreNearby;
    private RecyclerView rvRecentReports, rvNearbyReports;

    // ---- recent reports use the new card-based adapter ----
    private RecentReportAdapter recentAdapter;
    private final List<FaultReport> recentReports = new ArrayList<>();

    private NearbyReportAdapter nearbyAdapter;
    private final List<NearbyReport> nearbyList = new ArrayList<>();

    private double userLat;
    private double userLng;

    private SessionManager session;
    private FusedLocationProviderClient fusedLocationClient;

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

        requestNotificationPermissionIfNeeded();

        // Views
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvReportCount = findViewById(R.id.tvReportCount);
        tvNearbyRadius = findViewById(R.id.tvNearbyRadius);
        tvSeeMoreNearby = findViewById(R.id.tvSeeMoreNearby);
        rvRecentReports = findViewById(R.id.rvRecentReports);
        rvNearbyReports = findViewById(R.id.rvNearbyReports);

        // Welcome text
        String firstName = session.getFirstName();
        if (firstName == null || firstName.isEmpty()) firstName = "there";
        tvWelcome.setText("Hello, " + firstName);
        tvSubtitle.setText("View your recent reports, track progress, and see nearby issues");

        // ---------- Recent reports RecyclerView (card layout) ----------
        rvRecentReports.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new RecentReportAdapter(recentReports, this::onRecentReportClicked);
        rvRecentReports.setAdapter(recentAdapter);

        // ---------- Nearby reports RecyclerView ----------
        rvNearbyReports.setLayoutManager(new LinearLayoutManager(this));
        nearbyAdapter = new NearbyReportAdapter(this, nearbyList, null);
        rvNearbyReports.setAdapter(nearbyAdapter);

        // Nearby radius label
        tvNearbyRadius.setText("Reports within 2 km of your current location");

        // See all nearby reports link
        if (tvSeeMoreNearby != null) {
            tvSeeMoreNearby.setOnClickListener(v ->
                    startActivity(new Intent(this, NearbyReportsActivity.class)));
        }

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

        setupBottomNav();

        // Location client + nearby reports
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        loadNearbyReports();

        // My reports
        loadMyReports();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIF_PERMISSION
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIF_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
            } else {
                Log.d(TAG, "Notification permission denied — FCM will be silent");
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCurrentLocation();
            } else {
                showNearbyUnavailable("Enable location to see reports within 2 km");
            }
        }
    }

    private void setupBottomNav() {
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
                List<FaultReport> reports =
                        SupabaseReports.getUserReports(citizenProfileId, accessToken);
                if (reports == null) reports = new ArrayList<>();

                // Newest first (server already sorts, but be defensive)
                reports.sort((a, b) -> {
                    String da = a.getCreatedAt() != null ? a.getCreatedAt() : "";
                    String db = b.getCreatedAt() != null ? b.getCreatedAt() : "";
                    return db.compareTo(da);
                });

                // Limit how many cards we show on the dashboard
                int limit = Math.min(reports.size(), RECENT_PREVIEW_LIMIT);
                final List<FaultReport> preview = reports.subList(0, limit);
                final int total = reports.size();

                runOnUiThread(() -> {
                    recentReports.clear();
                    recentReports.addAll(preview);
                    recentAdapter.notifyDataSetChanged();

                    tvReportCount.setText(total + " report" + (total == 1 ? "" : "s"));
                    Log.d(TAG, "Loaded " + total + " reports (showing " + limit + ")");
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load reports: " + e.getMessage(), e);
                runOnUiThread(() -> tvReportCount.setText("Could not load reports"));
            }
        }).start();
    }

    // ------------------------------------------------------------------
    // Recent report card tapped
    // ------------------------------------------------------------------
    private void onRecentReportClicked(FaultReport report) {
        Intent intent = new Intent(this, TrackReportResultActivity.class);
        intent.putExtra("report_id", report.getReportId());
        intent.putExtra("reference_number", report.getReferenceNumber());
        intent.putExtra("title", report.getTitle());
        intent.putExtra("description", report.getDescription());
        intent.putExtra("address_line", report.getAddressLine());
        intent.putExtra("status", report.getStatus());
        intent.putExtra("priority_score",
                report.getPriorityScore() != null ? report.getPriorityScore() : 0);
        intent.putExtra("created_at", report.getCreatedAt());
        startActivity(intent);
    }

    // ------------------------------------------------------------------
    // Nearby reports
    // ------------------------------------------------------------------
    private void loadNearbyReports() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            requestCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private void requestCurrentLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            userLat = location.getLatitude();
                            userLng = location.getLongitude();
                            loadReportsFromSupabase();
                        } else {
                            showNearbyUnavailable("Could not get your location. Try again later.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "getCurrentLocation failed", e);
                        showNearbyUnavailable("Could not get your location. Try again later.");
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission revoked mid-flight", e);
            showNearbyUnavailable("Enable location to see reports within 2 km");
        }
    }

    private void showNearbyUnavailable(String message) {
        tvNearbyRadius.setText(message);
        nearbyList.clear();
        nearbyAdapter.notifyDataSetChanged();
        if (tvSeeMoreNearby != null) tvSeeMoreNearby.setVisibility(View.GONE);
    }

    private void loadReportsFromSupabase() {
        Log.d(TAG, "loadReportsFromSupabase: starting fetch, userLat=" + userLat + " userLng=" + userLng);
        new Thread(() -> {
            try {
                String accessToken = session.getAccessToken();

                // Category lookup
                Map<Long, String> categoryNames = new HashMap<>();
                List<FaultCategory> categories = SupabaseReports.getCategories(accessToken);
                for (FaultCategory c : categories) {
                    categoryNames.put(c.getCategoryId(), c.getName());
                }

                // The database RPC restricts results to open reports within 2 km.
                List<FaultReport> openReports = SupabaseReports.getNearbyReports(
                        userLat, userLng, accessToken);
                Log.d(TAG, "Fetched " + openReports.size() + " nearby reports");

                // Retain a local distance check for display and rounding at the boundary.
                List<NearbyReport> fresh = new ArrayList<>();
                for (FaultReport r : openReports) {
                    double distance = SupabaseReports.haversineMeters(
                            userLat, userLng, r.getLatitude(), r.getLongitude());

                    if (distance <= PROXIMITY_RADIUS_M) {
                        String categoryName = categoryNames.getOrDefault(r.category_id, "Report");
                        int safePriority = (r.getPriorityScore() != null) ? r.getPriorityScore() : 0;

                        fresh.add(new NearbyReport(
                                r.getReportId(),
                                r.getReferenceNumber(),
                                r.getTitle(),
                                categoryName,
                                r.getStatus(),
                                safePriority,
                                0,
                                distance
                        ));
                    }
                }

                // Sort nearest first
                fresh.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));

                Log.d(TAG, "Filtered to " + fresh.size() + " reports within " + PROXIMITY_RADIUS_M + "m");

                final int totalCount = fresh.size();

                runOnUiThread(() -> {
                    tvNearbyRadius.setText("Reports within 2 km of your current location");
                    nearbyList.clear();

                    // Show at most 5 on the dashboard
                    int limit = Math.min(totalCount, NEARBY_PREVIEW_LIMIT);
                    nearbyList.addAll(fresh.subList(0, limit));
                    nearbyAdapter.notifyDataSetChanged();

                    // Show "See all" only when there are more than 5
                    if (tvSeeMoreNearby != null) {
                        tvSeeMoreNearby.setVisibility(
                                totalCount > NEARBY_PREVIEW_LIMIT ? View.VISIBLE : View.GONE);
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Failed to load nearby reports", e);
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyReports();
    }
}
