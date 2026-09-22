package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ReportHistory";

    private RecyclerView rvReports;
    private FaultReportAdapter adapter;
    private TextView tvEmptyState;

    private final List<FaultReport> allReports = new ArrayList<>();
    private final List<FaultReport> filteredReports = new ArrayList<>();

    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvReports = findViewById(R.id.rv_reports);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FaultReportAdapter(this, filteredReports);
        adapter.setOnItemClickListener(this::onReportClicked);
        rvReports.setAdapter(adapter);

        setupFilterChips();
        setupBottomNav();

        View backBtn = findViewById(R.id.back_button);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        // NOTE: loadReports() is intentionally NOT called here.
        // onResume() handles the initial load AND every return to this screen,
        // so we avoid firing two Supabase requests on first entry.
    }

    private void setupFilterChips() {
        ChipGroup group = findViewById(R.id.chip_group_filter);
        Chip all = findViewById(R.id.chip_all);
        Chip pending = findViewById(R.id.chip_pending);
        Chip inProgress = findViewById(R.id.chip_in_progress);
        Chip resolved = findViewById(R.id.chip_resolved);

        if (group == null) return;

        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_pending) currentFilter = "Pending";
            else if (id == R.id.chip_in_progress) currentFilter = "In Progress";
            else if (id == R.id.chip_resolved) currentFilter = "Resolved";
            else currentFilter = "All";

            applyFilter();
        });
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, Dashboard_Activity.class));
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

        View ivNotif = findViewById(R.id.ivNotifications);
        if (ivNotif != null) {
            ivNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, Notifications_Activity.class)));
        }
    }

    private void loadReports() {
        SessionManager session = new SessionManager(this);
        long citizenProfileId = session.getCitizenProfileId();
        String accessToken = session.getAccessToken();

        if (citizenProfileId <= 0 || accessToken == null) {
            showEmptyState("Please log in to view your report history.");
            return;
        }

        Log.d(TAG, "Loading reports for citizen_profile_id = " + citizenProfileId);

        // Loading state — only show if we don't already have content on screen
        if (filteredReports.isEmpty()) {
            showEmptyState("Loading reports…");
        }

        new Thread(() -> {
            try {
                List<FaultReport> reports = SupabaseReports.getUserReports(citizenProfileId, accessToken);
                if (reports == null) reports = new ArrayList<>();

                final List<FaultReport> finalReports = reports;
                Log.d(TAG, "Loaded " + finalReports.size() + " reports");

                runOnUiThread(() -> {
                    allReports.clear();
                    allReports.addAll(finalReports);
                    applyFilter();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load reports: " + e.getMessage(), e);
                runOnUiThread(() ->
                        showEmptyState("Could not load reports. Check your connection and try again."));
            }
        }).start();
    }

    /**
     * Phase 5.4 — filter by status.
     * "Resolved" matches status = "Resolved" / "Completed" / "Closed".
     * "In Progress" matches status = "In Progress" / "Assigned".
     * "Pending" matches status = "Pending" / "Open".
     * "All" shows everything.
     */
    private void applyFilter() {
        filteredReports.clear();

        for (FaultReport r : allReports) {
            String status = r.getStatus() != null ? r.getStatus() : "Pending";
            String s = status.toLowerCase(Locale.US);

            boolean include;
            switch (currentFilter) {
                case "Pending":
                    include = s.equals("pending") || s.equals("open");
                    break;
                case "In Progress":
                    include = s.equals("in progress") || s.equals("assigned");
                    break;
                case "Resolved":
                    include = s.equals("resolved") || s.equals("completed")
                            || s.equals("closed");
                    break;
                default:
                    include = true;
            }

            if (include) filteredReports.add(r);
        }

        adapter.notifyDataSetChanged();

        if (filteredReports.isEmpty()) {
            if (allReports.isEmpty()) {
                showEmptyState("You haven't submitted any reports yet.");
            } else {
                showEmptyState("No reports match this filter.");
            }
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState(String message) {
        if (tvEmptyState != null) {
            tvEmptyState.setText(message);
            tvEmptyState.setVisibility(View.VISIBLE);
        }
        if (rvReports != null) rvReports.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
        if (rvReports != null) rvReports.setVisibility(View.VISIBLE);
    }

    /**
     * Phase 5.5 — open details in TrackReportResultActivity.
     */
    private void onReportClicked(FaultReport report) {
        Log.d(TAG, "Opening details for ref = " + report.getReferenceNumber());

        Intent intent = new Intent(this, TrackReportResultActivity.class);
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

    @Override
    protected void onResume() {
        super.onResume();
        // Refreshes on first entry AND whenever the user returns from a detail screen,
        // so status changes made by municipal staff show up immediately.
        loadReports();
    }
}