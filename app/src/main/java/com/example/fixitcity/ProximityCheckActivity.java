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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProximityCheckActivity extends AppCompatActivity {

    private static final String TAG = "ProximityCheck";
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final double RADIUS_METERS = 2000.0;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private RecyclerView rvReports;
    private NearbyReportAdapter adapter;
    private final List<NearbyReport> reportList = new ArrayList<>();

    private TextView tvStatus, tvReportsCount;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton btnNewIssue;

    private ReportData reportData;

    /** My own citizen id — used to exclude own reports and block self-upvote. */
    private long myCitizenId = -1;

    private final Set<Long> votedReportIds = new HashSet<>();

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
        AppNavigation.bindNotificationBell(this);
        TextView nearbyHint = findViewById(R.id.tv_nearby_hint);
        if (nearbyHint != null && new SessionManager(this).isGuest()) {
            nearbyHint.setText("We found open reports within 2 km. Tap one to see its status.");
        }

        reportData = ((FixITCityApp) getApplication()).getReportData();

        // Cache the citizen id once
        SessionManager session = new SessionManager(this);
        myCitizenId = session.getCitizenProfileId();
        Log.d(TAG, "myCitizenId=" + myCitizenId);

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

        if (rvReports != null) {
            rvReports.setLayoutManager(new LinearLayoutManager(this));
            adapter = new NearbyReportAdapter(this, reportList, this::onReportTapped);
            rvReports.setAdapter(adapter);
        }
    }

    private void setupBottomNavigation() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) navHome.setOnClickListener(v -> AppNavigation.goHome(this));

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
                    if (tvStatus != null) tvStatus.setText("Unable to get location");
                    if (progressIndicator != null) progressIndicator.setIndeterminate(false);
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
                if (tvStatus != null) tvStatus.setText("Could not locate address");
                if (progressIndicator != null) progressIndicator.setIndeterminate(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (tvStatus != null) tvStatus.setText("Geocoding error");
            if (progressIndicator != null) progressIndicator.setIndeterminate(false);
        }
    }

    private void queryNearbyReports(double lat, double lng) {
        long categoryId = reportData.getCategoryId();
        String categoryName = reportData.getCategoryName();

        if (tvStatus != null) {
            tvStatus.setText("Searching for nearby "
                    + (categoryName != null ? categoryName.toLowerCase(Locale.US) + " " : "")
                    + "reports…");
        }
        if (progressIndicator != null) progressIndicator.setIndeterminate(true);

        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();
        final long myId = myCitizenId;

        new Thread(() -> {
            List<NearbyReport> withinRadius = new ArrayList<>();
            String loadError = null;
            try {
                if (categoryId <= 0) {
                    Log.w(TAG, "No categoryId set on ReportData — nothing to filter by");
                    loadError = "Choose a report category before checking nearby reports.";
                } else {
                    withinRadius = SupabaseReports.getNearbyReportsForCategory(
                            categoryId,
                            myId,
                            lat, lng,
                            RADIUS_METERS,
                            accessToken);
                }
            } catch (Exception e) {
                Log.e(TAG, "Nearby query failed: " + e.getMessage(), e);
                loadError = "Could not check nearby reports. Check your connection and try again.";
            }

            final List<NearbyReport> finalList = withinRadius;
            final String finalError = loadError;
            runOnUiThread(() -> updateUI(finalList, categoryName, finalError));
        }).start();
    }

    private void updateUI(List<NearbyReport> withinRadius, String categoryName, String loadError) {
        if (progressIndicator != null) progressIndicator.setIndeterminate(false);

        reportList.clear();
        reportList.addAll(withinRadius);
        if (adapter != null) adapter.notifyDataSetChanged();

        if (loadError != null) {
            if (tvStatus != null) tvStatus.setText(loadError);
            if (tvReportsCount != null) tvReportsCount.setText("Nearby reports unavailable");
            return;
        }

        String catLabel = categoryName != null
                ? categoryName.toLowerCase(Locale.US) + " "
                : "";

        if (withinRadius.isEmpty()) {
            if (tvStatus != null) tvStatus.setText("No similar reports found nearby — this looks new.");
            if (tvReportsCount != null) {
                tvReportsCount.setText("0 " + catLabel + "reports within "
                        + (int) RADIUS_METERS + "m");
            }
        } else {
            if (tvStatus != null) tvStatus.setText(new SessionManager(this).isGuest()
                    ? "Tap a nearby report to see whether this issue was already reported."
                    : "Tap a card to upvote it instead of creating a duplicate.");
            if (tvReportsCount != null) {
                tvReportsCount.setText(withinRadius.size() + " " + catLabel
                        + "report(s) within " + (int) RADIUS_METERS + "m");
            }
        }
    }

    private void onReportTapped(NearbyReport report, int position) {
        SessionManager guestSession = new SessionManager(this);
        if (guestSession.isGuest()) {
            new Thread(() -> {
                try {
                    FaultReport existing = SupabaseReports.trackExternalAsGuest(
                            report.getReferenceNumber(), guestSession.getAccessToken());
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (existing == null) {
                            Toast.makeText(this, "Your own reports are in Recent reports.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent intent = new Intent(this, TrackReportResultActivity.class);
                        intent.putExtra("report_id", existing.getReportId());
                        intent.putExtra("reference_number", existing.getReferenceNumber());
                        intent.putExtra("title", existing.getTitle());
                        intent.putExtra("description", existing.getDescription());
                        intent.putExtra("address_line", existing.getAddressLine());
                        intent.putExtra("status", existing.getStatus());
                        intent.putExtra("priority_score", existing.getPriorityScore() != null
                                ? existing.getPriorityScore() : 0);
                        intent.putExtra("created_at", existing.getCreatedAt());
                        intent.putExtra("external_guest", true);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Guest nearby details failed", e);
                    runOnUiThread(() -> Toast.makeText(this,
                            "Could not open this report. Check your connection.", Toast.LENGTH_LONG).show());
                }
            }, "guest-nearby-details").start();
            return;
        }
        long reportId = report.getReportId();

        // ---- Self-upvote guard (defensive — list should already exclude them) ----
        if (myCitizenId > 0 && report.getOwnerCitizenId() == myCitizenId) {
            Toast.makeText(this,
                    "You can't upvote your own report.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (votedReportIds.contains(reportId)) {
            Toast.makeText(this,
                    "You already upvoted this report.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        SessionManager session = new SessionManager(this);
        long citizenId = session.getCitizenProfileId();
        String token = session.getAccessToken();

        if (citizenId <= 0 || token == null) {
            Toast.makeText(this, "Please log in to upvote", Toast.LENGTH_SHORT).show();
            return;
        }

        // Optimistic UI bump
        int oldCount = report.getVoteCount();
        report.setVoteCount(oldCount + 1);
        if (adapter != null) adapter.notifyItemChanged(position);
        votedReportIds.add(reportId);

        new Thread(() -> {
            String errorMessage = null;
            try {
                SupabaseReports.upvoteReport(reportId, citizenId, token);
            } catch (IOException e) {
                Log.e(TAG, "Upvote failed", e);
                String detail = e.getMessage();
                errorMessage = detail != null
                        && detail.contains("report_votes_unique_per_citizen")
                        ? "You already upvoted this report."
                        : "Could not upvote. Check your connection and try again.";
            }

            final String failure = errorMessage;
            runOnUiThread(() -> {
                if (failure == null) {
                    // Success → open Thank You screen
                    Intent i = new Intent(this, ThankYouActivity.class);
                    i.putExtra("title", "Thank you!");
                    i.putExtra("message",
                            "Your upvote has been counted. It helps push this issue up the priority list.");
                    startActivity(i);
                    finish();
                } else {
                    report.setVoteCount(oldCount);
                    if (adapter != null) adapter.notifyItemChanged(position);
                    votedReportIds.remove(reportId);

                    Toast.makeText(this, failure, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
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
                if (tvStatus != null) tvStatus.setText("Location permission denied");
                if (progressIndicator != null) progressIndicator.setIndeterminate(false);
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
