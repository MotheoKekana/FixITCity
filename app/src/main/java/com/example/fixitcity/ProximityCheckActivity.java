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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProximityCheckActivity extends AppCompatActivity {

    private static final String TAG = "ProximityCheck";
    private static final int LOCATION_PERMISSION_REQUEST = 100;

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

        reportData = ((FixITCityApp) getApplication()).getReportData();

        initViews();
        setupBottomNavigation();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Use stored location if available, else GPS
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

        // New Issue – go to Photo
        if (btnNewIssue != null) {
            btnNewIssue.setOnClickListener(v -> {
                startActivity(new Intent(this, ReportFaultPhotoActivity.class));
                finish();
            });
        }
    }

    private void initViews() {
        rvReports = findViewById(R.id.rv_nearby_reports);
        tvStatus = findViewById(R.id.tv_status);
        tvReportsCount = findViewById(R.id.tv_reports_count);
        progressIndicator = findViewById(R.id.progress_indicator);
        btnNewIssue = findViewById(R.id.btn_new_issue);

        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NearbyReportAdapter(this, reportList, this::onReportUpvoted);
        rvReports.setAdapter(adapter);

        Log.d(TAG, "initViews complete. Adapter set with " + reportList.size() + " reports");
    }

    private void setupBottomNavigation() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> finish());
        }

        View navReport = findViewById(R.id.navReport);
        if (navReport != null) {
            navReport.setOnClickListener(v ->
                    startActivity(new Intent(this, ReportFaultCategoryActivity.class))
            );
        }

        View navTrack = findViewById(R.id.navTrack);
        if (navTrack != null) {
            navTrack.setOnClickListener(v ->
                    Toast.makeText(this, "Track screen coming soon", Toast.LENGTH_SHORT).show());
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileSettingsActivity.class))
            );
        }

        View backBtn = findViewById(R.id.back_button);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
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
        simulateApiResponse();
    }

    private void simulateApiResponse() {
        new android.os.Handler().postDelayed(() -> {
            reportList.clear();
            reportList.add(new NearbyReport(101, "Large Pothole", "Pothole", 8, 45));
            reportList.add(new NearbyReport(102, "Burst Water Pipe", "Water", 12, 78));
            reportList.add(new NearbyReport(103, "Blocked Storm Drain", "Drain", 3, 120));
            reportList.add(new NearbyReport(104, "Streetlight Outage", "Lighting", 5, 160));

            // Re-apply saved upvote so it persists across screen rebuilds
            if (reportData.getUpvotedReportId() != -1) {
                for (NearbyReport r : reportList) {
                    if (r.getId() == reportData.getUpvotedReportId()) {
                        r.incrementVote();
                        break;
                    }
                }
            }

            updateUI();
        }, 1500);
    }

    private void updateUI() {
        progressIndicator.setIndeterminate(false);
        tvStatus.setText("Nearby reports found:");
        tvReportsCount.setText(reportList.size() + " report(s) found within 200m");
        adapter.notifyDataSetChanged();

        if (reportList.isEmpty() && btnNewIssue != null) {
            btnNewIssue.performClick();
        }
    }

    private void onReportUpvoted(NearbyReport report, int position) {
        Log.d(TAG, "=== onReportUpvoted called ===");
        Log.d(TAG, "Report: " + report.getTitle() + ", position: " + position);
        Log.d(TAG, "hasUpvoted: " + reportData.hasUpvoted());

        if (reportData.hasUpvoted()) {
            Toast.makeText(this, "You already upvoted a report", Toast.LENGTH_SHORT).show();
            return;
        }

        // Increment vote and refresh the list row
        report.incrementVote();
        adapter.notifyItemChanged(position);

        // Persist across screen rebuilds
        reportData.setHasUpvoted(true);
        reportData.setUpvotedReportId(report.getId());

        Log.d(TAG, "Navigating to ThankYouActivity...");
        Toast.makeText(this, "Upvoted: " + report.getTitle(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, ThankYouActivity.class);
        intent.putExtra("report_name", report.getTitle());
        startActivity(intent);
        finish();
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