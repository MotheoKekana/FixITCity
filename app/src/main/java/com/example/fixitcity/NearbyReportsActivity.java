package com.example.fixitcity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NearbyReportsActivity extends AppCompatActivity {

    private static final String TAG = "NearbyReports";
    private static final int REQ_LOCATION = 1001;

    /** Radius in meters — 2 km. */
    private static final double RADIUS_METERS = 2000.0;

    /** Dropdown options. */
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

    /** Titles containing one of these are NOT "Other". */
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

    /** Full list within 2 km — source for filtering. */
    private final List<NearbyReport> allWithinRadius = new ArrayList<>();

    /** Currently displayed list. */
    private final List<NearbyReport> nearbyList = new ArrayList<>();

    private String currentFilter = FILTER_ALL;

    private FusedLocationProviderClient fusedClient;
    private double userLat;
    private double userLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_reports);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ---------- RecyclerView ----------
        rvNearbyReports = findViewById(R.id.rvNearbyReports);
        if (rvNearbyReports == null) {
            throw new IllegalStateException(
                    "rvNearbyReports not found in activity_nearby_reports.xml");
        }
        rvNearbyReports.setLayoutManager(new LinearLayoutManager(this));
        rvNearbyReports.setNestedScrollingEnabled(false);
        adapter = new NearbyReportAdapter(this, nearbyList, null);
        rvNearbyReports.setAdapter(adapter);

        tvEmptyState = findViewById(R.id.tvEmptyState);

        // ---------- Filter dropdown ----------
        // --- Filter dropdown ---
        actCategoryFilter = findViewById(R.id.actCategoryFilter);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                FILTER_OPTIONS) {
            @NonNull
            @Override
            public android.widget.Filter getFilter() {
                // No-op filter: always show every option, regardless of current text.
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

        // ---------- Location ----------
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
            showLocationUnavailable("Enable location to see reports within 2 km.");
            return;
        }

        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        userLat = loc.getLatitude();
                        userLng = loc.getLongitude();
                        loadAllNearbyReports();
                    } else {
                        showLocationUnavailable("Could not get your location. Try again later.");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Location failed", e);
                    showLocationUnavailable("Could not get your location. Try again later.");
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
                showLocationUnavailable("Enable location to see reports within 2 km.");
            }
        }
    }

    private void showLocationUnavailable(String message) {
        allWithinRadius.clear();
        nearbyList.clear();
        adapter.notifyDataSetChanged();
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
    }

    /** Fetch open reports within 2 km from the database RPC. */
    private void loadAllNearbyReports() {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            showLocationUnavailable("Sign in to see nearby reports.");
            return;
        }

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

                runOnUiThread(() -> {
                    allWithinRadius.clear();
                    allWithinRadius.addAll(fresh);
                    applyFilter();
                    if (fresh.isEmpty()) {
                        tvEmptyState.setText("No open reports found within 2 km.");
                    } else {
                        tvEmptyState.setText("No reports match this filter.");
                    }
                });

            } catch (IOException e) {
                android.util.Log.e(TAG, "Failed to load nearby reports", e);
                runOnUiThread(() -> showLocationUnavailable("Could not load nearby reports. Try again later."));
            } catch (Exception e) {
                android.util.Log.e(TAG, "Unexpected error loading reports", e);
                runOnUiThread(() -> showLocationUnavailable("Could not load nearby reports. Try again later."));
            }
        }).start();
    }

    /**
     * Rebuild {@link #nearbyList} from {@link #allWithinRadius} based on
     * {@link #currentFilter}.
     *
     *  - "All Reports" -> everything
     *  - "Burst Pipe" / "Pothole" / "Water Leak" / "Streetlight"
     *        -> title contains that keyword
     *  - "Other"       -> title contains NONE of the known categories
     */
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

    /** True if the report's title contains any of the four known keywords. */
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
