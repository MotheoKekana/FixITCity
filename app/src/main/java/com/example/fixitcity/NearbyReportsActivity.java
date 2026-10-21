package com.example.fixitcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NearbyReportsActivity extends AppCompatActivity {

    private static final String TAG = "NearbyReports";
    private static final int REQ_LOCATION = 1001;

    private static final double RADIUS_METERS = 2000.0;


    private static final String FILTER_ALL   = "All Reports";
    private static final String FILTER_OTHER = "Other";

    private static final String[] FILTER_OPTIONS = {
            FILTER_ALL,
            "Burst Pipe",
            "Pothole",
            "Water Leak",
            "Streetlight",
            FILTER_OTHER
    };

    private static final String[] KNOWN_CATEGORIES = {
            "Burst Pipe",
            "Pothole",
            "Water Leak",
            "Streetlight"
    };

    private RecyclerView rvNearbyReports;
    private MaterialAutoCompleteTextView actCategoryFilter;
    private TextView tvEmptyState;

    private NearbyReportAdapter adapter;

    private final List<NearbyReport> allWithinRadius = new ArrayList<>();

    private final List<NearbyReport> nearbyList = new ArrayList<>();

    private String currentFilter = FILTER_ALL;

    private FusedLocationProviderClient fusedClient;
    private double userLat;
    private double userLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppNavigation.requireCitizen(this)) return;
        setContentView(R.layout.activity_nearby_reports);

        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);
                Class<?> target = session.isGuest()
                        ? Guest_Dashboard_Activity.class
                        : Dashboard_Activity.class;
                Intent intent = new Intent(this, target);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
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
            navTrack.setOnClickListener(v -> {
                startActivity(new Intent(this, TrackReportSearchActivity.class));
                finish();
            });
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileSettingsActivity.class));
                finish();
            });
        }

        AppNavigation.bindNotificationBell(this);

        ScreenInsets.apply(this);

        rvNearbyReports = findViewById(R.id.rvNearbyReports);
        if (rvNearbyReports == null) {
            throw new IllegalStateException(
                    "rvNearbyReports not found in activity_nearby_reports.xml");
        }
        rvNearbyReports.setLayoutManager(new LinearLayoutManager(this));
        rvNearbyReports.setNestedScrollingEnabled(false);
        adapter = new NearbyReportAdapter(this, nearbyList, (report, position) -> {
            SessionManager session = new SessionManager(this);
            if (!session.isGuest()) {
                AppNavigation.openNearbySummary(this, report);
                return;
            }
            new Thread(() -> {
                try {
                    FaultReport details = SupabaseReports.trackExternalAsGuest(
                            report.getReferenceNumber(), session.getAccessToken());
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (details == null) {
                            Toast.makeText(this, "Your own reports are in Recent reports.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent intent = new Intent(this, TrackReportResultActivity.class);
                        intent.putExtra("report_id", details.getReportId());
                        intent.putExtra("reference_number", details.getReferenceNumber());
                        intent.putExtra("title", details.getTitle());
                        intent.putExtra("description", details.getDescription());
                        intent.putExtra("address_line", details.getAddressLine());
                        intent.putExtra("status", details.getStatus());
                        intent.putExtra("priority_score", details.getPriorityScore() != null
                                ? details.getPriorityScore() : 0);
                        intent.putExtra("created_at", details.getCreatedAt());
                        intent.putExtra("external_guest", true);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Could not open this report. Check your connection.", Toast.LENGTH_LONG).show());
                }
            }, "guest-nearby-detail").start();
        });
        rvNearbyReports.setAdapter(adapter);

        tvEmptyState = findViewById(R.id.tvEmptyState);

        actCategoryFilter = findViewById(R.id.actCategoryFilter);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                FILTER_OPTIONS) {
            @NonNull
            @Override
            public android.widget.Filter getFilter() {
                // Keep every category visible in the dropdown while a filter is selected.
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence c) {
                        FilterResults r = new FilterResults();
                        r.values = FILTER_OPTIONS;
                        r.count = FILTER_OPTIONS.length;
                        return r;
                    }
                    @Override
                    protected void publishResults(CharSequence c, FilterResults r) {
                        notifyDataSetChanged();
                    }
                };
            }
        };

        actCategoryFilter.setAdapter(filterAdapter);
        actCategoryFilter.setText(FILTER_ALL, false);

        actCategoryFilter.setOnItemClickListener((parent, view, position, id) -> {
            Object sel = parent.getItemAtPosition(position);
            if (sel != null) {
                currentFilter = sel.toString();
                android.util.Log.d(TAG, "dropdown selected: " + currentFilter);
                applyFilter();
            }
        });

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationThenLoad();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
        }
    }

    private void fetchLocationThenLoad() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showLocationUnavailable();
            return;
        }

        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        userLat = loc.getLatitude();
                        userLng = loc.getLongitude();
                        android.util.Log.d(TAG, "Using device location: "
                                + userLat + ", " + userLng);
                        loadAllNearbyReports();
                    } else {
                        showLocationUnavailable();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Location unavailable", e);
                    showLocationUnavailable();
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
                showLocationUnavailable();
            }
        }
    }

    private void showLocationUnavailable() {
        if (tvEmptyState != null) {
            tvEmptyState.setText("Enable location to find reports within 2 km of you.");
            tvEmptyState.setVisibility(View.VISIBLE);
        }
        allWithinRadius.clear();
        nearbyList.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadAllNearbyReports() {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        new Thread(() -> {
            try {
                List<FaultReport> allOpen = SupabaseReports.getNearbyReports(
                        userLat, userLng, accessToken);

                android.util.Log.d(TAG, "getNearbyReports returned "
                        + (allOpen == null ? "NULL" : allOpen.size() + " items"));

                List<NearbyReport> fresh = new ArrayList<>();
                if (allOpen != null) {
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
                }

                fresh.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));

                android.util.Log.d(TAG, "after radius filter: " + fresh.size()
                        + " within " + RADIUS_METERS + "m of ("
                        + userLat + ", " + userLng + ")");

                for (NearbyReport n : fresh) {
                    android.util.Log.d(TAG, "  report title=\"" + n.getTitle() + "\"");
                }

                runOnUiThread(() -> {
                    allWithinRadius.clear();
                    allWithinRadius.addAll(fresh);
                    applyFilter();
                });

            } catch (IOException e) {
                android.util.Log.e(TAG, "Failed to load nearby reports", e);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Unexpected error loading reports", e);
            }
        }).start();
    }

    private void applyFilter() {
        nearbyList.clear();

        if (FILTER_ALL.equals(currentFilter)) {
            nearbyList.addAll(allWithinRadius);

        } else if (FILTER_OTHER.equals(currentFilter)) {
            for (NearbyReport r : allWithinRadius) {
                if (!matchesAnyKnownCategory(r)) {
                    nearbyList.add(r);
                }
            }

        } else {
            String needle = currentFilter.toLowerCase(Locale.ROOT);
            for (NearbyReport r : allWithinRadius) {
                String title = r.getTitle() != null
                        ? r.getTitle().toLowerCase(Locale.ROOT) : "";
                if (title.contains(needle)) {
                    nearbyList.add(r);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(
                    nearbyList.isEmpty() ? View.VISIBLE : View.GONE);
        }

        android.util.Log.d(TAG, "filter=" + currentFilter
                + " -> showing " + nearbyList.size() + " of "
                + allWithinRadius.size());
    }

    private boolean matchesAnyKnownCategory(NearbyReport report) {
        String title = report.getTitle() != null
                ? report.getTitle().toLowerCase(Locale.ROOT) : "";

        for (String known : KNOWN_CATEGORIES) {
            if (title.contains(known.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
