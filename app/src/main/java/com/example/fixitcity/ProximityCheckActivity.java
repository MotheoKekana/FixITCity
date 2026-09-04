package com.example.fixitcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class ProximityCheckActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private RecyclerView rvReports;
    private NearbyReportAdapter adapter;
    private List<NearbyReport> reportList = new ArrayList<>();

    private TextView tvStatus, tvReportsCount;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton btnContinue, btnNewIssue;

    private boolean hasUpvoted = false;
    private int upvotedReportId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proximity_check);

        initViews();
        setupBottomNavigation();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (checkLocationPermission()) {
            getCurrentLocation();
        } else {
            requestLocationPermission();
        }

        // "New Issue" button – clears draft and goes to category selection
        btnNewIssue.setOnClickListener(v -> {
            ((FixITCityApp) getApplication()).clearReportData();
            startActivity(new Intent(this, ReportFaultCategoryActivity.class));
            finish();
        });

        // Continue button – only enabled after upvoting
        btnContinue.setOnClickListener(v -> {
            if (hasUpvoted) {
                new AlertDialog.Builder(this)
                        .setTitle("Upvote Confirmed")
                        .setMessage("You've upvoted this report. Your draft has been discarded.")
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void initViews() {
        rvReports = findViewById(R.id.rv_nearby_reports);
        tvStatus = findViewById(R.id.tv_status);
        tvReportsCount = findViewById(R.id.tv_reports_count);
        progressIndicator = findViewById(R.id.progress_indicator);
        btnContinue = findViewById(R.id.btn_continue);
        btnNewIssue = findViewById(R.id.btn_new_issue);

        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NearbyReportAdapter(this, reportList, this::onReportUpvoted);
        rvReports.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navReport).setOnClickListener(v -> {
            // Already in report flow – do nothing or restart
        });
        findViewById(R.id.navTrack).setOnClickListener(v ->
                Toast.makeText(this, "Track screen coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navSettings).setOnClickListener(v ->
                Toast.makeText(this, "Settings screen coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
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

    private void queryNearbyReports(double lat, double lng) {
        tvStatus.setText("Searching for nearby reports...");
        progressIndicator.setIndeterminate(true);

        // Replace with actual API call
        simulateApiResponse();
    }

    private void simulateApiResponse() {
        new android.os.Handler().postDelayed(() -> {
            reportList.clear();
            reportList.add(new NearbyReport(101, "Large Pothole", "Pothole", 8, 45));
            reportList.add(new NearbyReport(102, "Burst Water Pipe", "Water", 12, 78));
            reportList.add(new NearbyReport(103, "Blocked Storm Drain", "Drain", 3, 120));
            reportList.add(new NearbyReport(104, "Streetlight Outage", "Lighting", 5, 160));
            updateUI();
        }, 1500);
    }

    private void updateUI() {
        progressIndicator.setIndeterminate(false);
        tvStatus.setText("Nearby reports found:");
        tvReportsCount.setText(reportList.size() + " report(s) found within 200m");
        adapter.notifyDataSetChanged();

        if (reportList.isEmpty()) {
            btnNewIssue.performClick(); // suggest new report
        }
    }

    private void onReportUpvoted(NearbyReport report, int position) {
        if (hasUpvoted) {
            Toast.makeText(this, "You already upvoted a report", Toast.LENGTH_SHORT).show();
            return;
        }

        // Increment vote
        report.incrementVote();
        adapter.notifyItemChanged(position);

        hasUpvoted = true;
        upvotedReportId = report.getId();
        btnContinue.setEnabled(true);
        btnContinue.setText("✓ Upvoted – Continue");

        Toast.makeText(this, "Upvoted: " + report.getTitle(), Toast.LENGTH_SHORT).show();

        // Discard draft
        ((FixITCityApp) getApplication()).clearReportData();

        // Here you would call your API to update the vote count on the server
        // and recalculate the priority score (votes = 20% weighting)
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