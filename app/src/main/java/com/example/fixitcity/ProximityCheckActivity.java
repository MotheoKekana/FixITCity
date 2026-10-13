package com.example.fixitcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ProximityCheckActivity extends AppCompatActivity {

    private static final String TAG = "ProximityCheck";
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final double RADIUS_METERS = 2000.0;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private RecyclerView rvReports;
    private NearbyReportAdapter adapter;
    private List<NearbyReport> reportList = new ArrayList<>();

    private TextView tvStatus, tvReportsCount;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton btnNewIssue;

    private ReportData reportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proximity_check);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hideNavForGuests();

        reportData = ((FixITCityApp) getApplication()).getReportData();

        initViews();
        setupBottomNavigation();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (reportData.getLatitude() != 0 && reportData.getLongitude() != 0) {
            queryNearbyReports(reportData.getLatitude(), reportData.getLongitude());
        } else if (reportData.getLocationAddress() != null) {
            geocodeAddress(reportData.getLocationAddress());
        } else {
            if (checkLocationPermission()) {
                getCurrentLocation();
            } else {
                requestLocationPermission();
            }
        }

        if (btnNewIssue != null) {
            btnNewIssue.setOnClickListener(v -> {
                startActivity(new Intent(this, ReportFaultPhotoActivity.class));
                finish();
            });
        }
    }

    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    private void initViews() {
        rvReports = findViewById(R.id.rv_nearby_reports);
        tvStatus = findViewById(R.id.tv_status);
        tvReportsCount = findViewById(R.id.tv_reports_count);
        progressIndicator = findViewById(R.id.progress_indicator);
        btnNewIssue = findViewById(R.id.btn_new_issue);

        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NearbyReportAdapter(this, reportList, this::onReportTapped);
        rvReports.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) navHome.setOnClickListener(v -> finish());

        View navReport = findViewById(R.id.navReport);
        if (navReport != null) {
            navReport.setOnClickListener(v ->
                    startActivity(new Intent(this, ReportFaultCategoryActivity.class)));
        }

        View navTrack = findViewById(R.id.navTrack);
        if (navTrack != null) {
            navTrack.setOnClickListener(v ->
                    Toast.makeText(this, "Track screen coming soon", Toast.LENGTH_SHORT).show());
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileSettingsActivity.class)));
        }

        View backBtn = findViewById(R.id.back_button);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
    }

    private void getCurrentLocation() {
        if (!checkLocationPermission()) return;

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    queryNearbyReports(location.getLatitude(), location.getLongitude());
                } else {
                    tvStatus.setText("Unable to get location");
                    progressIndicator.setIndeterminate(false);
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void geocodeAddress(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                reportData.setLatitude(addr.getLatitude());
                reportData.setLongitude(addr.getLongitude());
                queryNearbyReports(addr.getLatitude(), addr.getLongitude());
            } else {
                tvStatus.setText("Could not locate address");
                progressIndicator.setIndeterminate(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            tvStatus.setText("Geocoding error");
            progressIndicator.setIndeterminate(false);
        }
    }

    private void queryNearbyReports(double lat, double lng) {
        tvStatus.setText("Searching for nearby reports...");
        progressIndicator.setIndeterminate(true);

        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        new Thread(() -> {
            try {
                List<FaultReport> allOpen = SupabaseReports.getOpenReports(accessToken);

                List<NearbyReport> withinRadius = new ArrayList<>();

                for (FaultReport r : allOpen) {
                    double d = SupabaseReports.haversineMeters(
                            lat, lng, r.getLatitude(), r.getLongitude());
                    if (d <= RADIUS_METERS) {
                        String ref = r.getReferenceNumber() != null ? r.getReferenceNumber() : "";
                        String title = r.getTitle() != null ? r.getTitle() : "Report";
                        String status = r.getStatus() != null ? r.getStatus() : "Pending";
                        int score = r.getPriorityScore() != null ? r.getPriorityScore() : 0;

                        withinRadius.add(new NearbyReport(
                                r.getReportId(),
                                ref,
                                title,
                                title,
                                status,
                                score,
                                0,
                                d
                        ));
                    }
                }

                Collections.sort(withinRadius, (a, b) ->
                        Double.compare(a.getDistance(), b.getDistance()));

                runOnUiThread(() -> updateUI(withinRadius));

            } catch (Exception e) {
                Log.e(TAG, "Nearby query failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    progressIndicator.setIndeterminate(false);
                    tvStatus.setText("Could not load nearby reports");
                    tvReportsCount.setText("0 reports found");
                    reportList.clear();
                    adapter.notifyDataSetChanged();
                    if (btnNewIssue != null) btnNewIssue.setEnabled(true);
                });
            }
        }).start();
    }

    private void updateUI(List<NearbyReport> withinRadius) {
        progressIndicator.setIndeterminate(false);
        reportList.clear();
        reportList.addAll(withinRadius);
        adapter.notifyDataSetChanged();

        if (withinRadius.isEmpty()) {
            tvStatus.setText("No nearby reports found within 2 km");
            tvReportsCount.setText("0 reports found");
        } else {
            tvStatus.setText("Nearby reports found:");
            tvReportsCount.setText(withinRadius.size() + " report(s) found within "
                    + (int)(RADIUS_METERS) + "m");
        }
    }

    private void onReportTapped(NearbyReport report, int position) {
        Toast.makeText(this,
                "Upvoting coming soon for: " + report.getTitle(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
                tvStatus.setText("Location permission denied");
                progressIndicator.setIndeterminate(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}