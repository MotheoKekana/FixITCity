package com.example.fixitcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Guest_Dashboard_Activity extends AppCompatActivity {

    private static final String TAG = "GuestDashboard";
    private static final int LOCATION_PERMISSION_REQUEST = 200;

    // Fallback – Gqeberha city centre
    private static final double DEFAULT_LAT = -33.9608;
    private static final double DEFAULT_LNG = 25.6022;
    private static final double PROXIMITY_RADIUS_M = 200;

    private RecyclerView rvNearbyReports;
    private NearbyReportAdapter adapter;
    private final List<NearbyReport> nearbyList = new ArrayList<>();

    private double userLat = DEFAULT_LAT;
    private double userLng = DEFAULT_LNG;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bottom navigation
        findViewById(R.id.navHome).setOnClickListener(v ->
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.navReport).setOnClickListener(v ->
                startActivity(new Intent(this, ReportFaultCategoryActivity.class))
        );
        findViewById(R.id.navTrack).setOnClickListener(v ->
                Toast.makeText(this, "Track screen coming soon", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.navSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileSettingsActivity.class))
        );

        // RecyclerView
        rvNearbyReports = findViewById(R.id.rvNearbyReports);
        rvNearbyReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NearbyReportAdapter(this, nearbyList, null);
        rvNearbyReports.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Kick off load
        loadNearbyReports();
    }

    private void loadNearbyReports() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            requestCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    private void requestCurrentLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            userLat = location.getLatitude();
                            userLng = location.getLongitude();
                            Log.d(TAG, "Using cached location: " + userLat + ", " + userLng);
                        } else {
                            Log.d(TAG, "No cached location — using Gqeberha default");
                        }
                        loadReportsFromSupabase();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "getLastLocation failed", e);
                        loadReportsFromSupabase();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission revoked mid-flight", e);
            loadReportsFromSupabase();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCurrentLocation();
            } else {
                loadReportsFromSupabase();
            }
        }
    }

    private void loadReportsFromSupabase() {
        Log.d(TAG, "loadReportsFromSupabase: starting fetch, userLat=" + userLat + " userLng=" + userLng);
        new Thread(() -> {
            try {
                SessionManager session = new SessionManager(this);
                String accessToken = session.isGuest() ? null : session.getAccessToken();

                // Category lookup
                Map<Long, String> categoryNames = new HashMap<>();
                List<FaultCategory> categories = SupabaseReports.getCategories(accessToken);
                for (FaultCategory c : categories) {
                    categoryNames.put(c.getCategoryId(), c.getName());

                }
                Log.d(TAG, "Loaded " + categories.size() + " categories");

                // Fetch open reports
                List<FaultReport> openReports = SupabaseReports.getOpenReports(accessToken);
                Log.d(TAG, "Fetched " + openReports.size() + " open reports");

                // Filter by distance
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
                Log.d(TAG, "Filtered to " + fresh.size() + " reports within " + PROXIMITY_RADIUS_M + "m");

                runOnUiThread(() -> {
                    nearbyList.clear();
                    nearbyList.addAll(fresh);
                    adapter.notifyDataSetChanged();

                    if (nearbyList.isEmpty()) {
                        Toast.makeText(this, "No reports within 200m", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Failed to load reports", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Could not load nearby reports", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}