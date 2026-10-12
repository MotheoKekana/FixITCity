package com.example.fixitcity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import java.util.List;

public class NearbyReportsActivity extends AppCompatActivity {

    private static final String TAG = "NearbyReports";
    private static final int REQ_LOCATION = 1001;

    /** Radius in meters — 2 km. */
    private static final double RADIUS_METERS = 2000.0;

    /** Fallback coordinates used only if the device location can't be obtained. */
    private static final double DEFAULT_LAT = -33.9608;
    private static final double DEFAULT_LNG = 25.6022;

    private RecyclerView rvNearbyReports;
    private NearbyReportAdapter adapter;
    private final List<NearbyReport> nearbyList = new ArrayList<>();

    private FusedLocationProviderClient fusedClient;
    private double userLat = DEFAULT_LAT;
    private double userLng = DEFAULT_LNG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_reports);

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- RecyclerView setup ---
        rvNearbyReports = findViewById(R.id.rvNearbyReports);
        if (rvNearbyReports == null) {
            throw new IllegalStateException(
                    "rvNearbyReports not found in activity_nearby_reports.xml");
        }
        rvNearbyReports.setLayoutManager(new LinearLayoutManager(this));
        rvNearbyReports.setNestedScrollingEnabled(false);
        adapter = new NearbyReportAdapter(this, nearbyList, null);
        rvNearbyReports.setAdapter(adapter);

        // --- Location client ---
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        // --- Ask for location, then load ---
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationThenLoad();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
        }
    }

    /** Gets the current location (or falls back to defaults) then loads reports. */
    private void fetchLocationThenLoad() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            loadAllNearbyReports();
            return;
        }

        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        userLat = loc.getLatitude();
                        userLng = loc.getLongitude();
                        android.util.Log.d(TAG, "Using device location: "
                                + userLat + ", " + userLng);
                    } else {
                        android.util.Log.d(TAG, "No location available, using defaults");
                    }
                    loadAllNearbyReports();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Location failed, using defaults", e);
                    loadAllNearbyReports();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationThenLoad();
            } else {
                android.util.Log.d(TAG, "Location permission denied, using defaults");
                loadAllNearbyReports();
            }
        }
    }

    /** Fetches open reports from Supabase and keeps those within RADIUS_METERS. */
    private void loadAllNearbyReports() {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        new Thread(() -> {
            try {
                List<FaultReport> allOpen = SupabaseReports.getOpenReports(accessToken);

                android.util.Log.d(TAG, "getOpenReports returned "
                        + (allOpen == null ? "NULL" : allOpen.size() + " items"));

                if (allOpen == null || allOpen.isEmpty()) {
                    runOnUiThread(() -> {
                        nearbyList.clear();
                        adapter.notifyDataSetChanged();
                    });
                    return;
                }

                List<NearbyReport> fresh = new ArrayList<>();
                for (FaultReport r : allOpen) {
                    double d = SupabaseReports.haversineMeters(
                            userLat, userLng,
                            r.getLatitude(), r.getLongitude());

                    if (d <= RADIUS_METERS) {
                        String title  = r.getTitle()           != null ? r.getTitle()           : "Report";
                        String status = r.getStatus()          != null ? r.getStatus()          : "Pending";
                        int    score  = r.getPriorityScore()   != null ? r.getPriorityScore()   : 0;
                        String ref    = r.getReferenceNumber() != null ? r.getReferenceNumber() : "";

                        fresh.add(new NearbyReport(
                                r.getReportId(), ref, title, title,
                                status, score, 0, d));
                    }
                }

                // Closest first
                fresh.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));

                android.util.Log.d(TAG, "after filter: " + fresh.size()
                        + " within " + RADIUS_METERS + "m of ("
                        + userLat + ", " + userLng + ")");

                runOnUiThread(() -> {
                    nearbyList.clear();
                    nearbyList.addAll(fresh);
                    adapter.notifyDataSetChanged();
                });

            } catch (IOException e) {
                android.util.Log.e(TAG, "Failed to load nearby reports", e);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Unexpected error loading reports", e);
            }
        }).start();
    }
}