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
import androidx.work.WorkInfo;

import com.example.fixitcity.data.AppDatabase;
import com.example.fixitcity.data.ReportEntity;
import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;
import com.example.fixitcity.services.SyncScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ReportHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ReportHistory";

    private RecyclerView rvReports;
    private TrackHistoryAdapter adapter;              // ← was FaultReportAdapter
    private TextView tvEmptyState;
    private MaterialButton btnSyncPending;
    private int pendingSyncCount;
    private WorkInfo.State activeSyncState;
    private boolean observedActiveSync;
    private boolean waitingForEnqueue;
    private final Set<UUID> seenFinishedWorkIds = new HashSet<>();

    private final List<FaultReport> allReports = new ArrayList<>();
    private final List<FaultReport> filteredReports = new ArrayList<>();

    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppNavigation.requireCitizen(this)) return;
        setContentView(R.layout.activity_report_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hideNavForGuests();

        rvReports = findViewById(R.id.rv_reports);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        btnSyncPending = findViewById(R.id.btn_sync_pending);

        rvReports.setLayoutManager(new LinearLayoutManager(this));

        // Card-based adapter — each report becomes its own MaterialCardView row
        adapter = new TrackHistoryAdapter(filteredReports, this::onReportClicked);
        rvReports.setAdapter(adapter);

        setupFilterChips();
        setupBottomNav();

        if (btnSyncPending != null) {
            btnSyncPending.setOnClickListener(v -> {
                waitingForEnqueue = true;
                activeSyncState = WorkInfo.State.ENQUEUED;
                updateSyncButton(pendingSyncCount);
                SyncScheduler.scheduleSync(this);
            });

            SyncScheduler.observeSync(this).observe(this, this::onSyncWorkChanged);
        }
    }

    private void onSyncWorkChanged(List<WorkInfo> jobs) {
        WorkInfo.State state = null;
        boolean newCompletion = false;
        if (jobs != null) {
            for (WorkInfo job : jobs) {
                if (job.getState().isFinished()
                        && seenFinishedWorkIds.add(job.getId())) {
                    newCompletion = true;
                }
                if (job.getState() == WorkInfo.State.RUNNING) {
                    state = WorkInfo.State.RUNNING;
                    break;
                }
                if (job.getState() == WorkInfo.State.ENQUEUED
                        || job.getState() == WorkInfo.State.BLOCKED) {
                    state = WorkInfo.State.ENQUEUED;
                }
            }
        }

        if (state != null) {
            waitingForEnqueue = false;
            observedActiveSync = true;
            activeSyncState = state;
        } else if (observedActiveSync || (waitingForEnqueue && newCompletion)) {
            waitingForEnqueue = false;
            observedActiveSync = false;
            activeSyncState = null;
            loadReports();
        } else if (!waitingForEnqueue) {
            activeSyncState = null;
        }
        updateSyncButton(pendingSyncCount);
    }

    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    private void setupFilterChips() {
        ChipGroup group = findViewById(R.id.chip_group_filter);
        if (group == null) return;

        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_pending) currentFilter = "Pending";
            else if (id == R.id.chip_under_review) currentFilter = "Under Review";
            else if (id == R.id.chip_accepted) currentFilter = "Accepted";
            else if (id == R.id.chip_in_progress) currentFilter = "In Progress";
            else if (id == R.id.chip_fixed) currentFilter = "Fixed";
            else currentFilter = "All";

            applyFilter();
        });
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);
                Class<?> target = session.isGuest()
                        ? Guest_Dashboard_Activity.class
                        : Dashboard_Activity.class;
                startActivity(new Intent(this, target));
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
    }

    private void loadReports() {
        SessionManager session = new SessionManager(this);
        long citizenProfileId = session.getCitizenProfileId();
        String accessToken = session.getAccessToken();
        boolean guest = session.isGuest();
        String guestAuthId = guest ? session.getUserId() : null;

        if ((!guest && citizenProfileId <= 0) || accessToken == null
                || (guest && !session.hasGuestAuthSession())) {
            showEmptyState("Start a guest session to view your reports.");
            return;
        }

        if (filteredReports.isEmpty()) {
            showEmptyState("Loading reports…");
        }

        // 1) Local pending rows first — always available offline
        new Thread(() -> {
            List<ReportEntity> pending = guest
                    ? AppDatabase.getInstance(getApplicationContext()).reportDao()
                    .getPendingForGuest(guestAuthId + ":")
                    : AppDatabase.getInstance(getApplicationContext()).reportDao()
                    .getPendingForCitizen(citizenProfileId);

            List<FaultReport> localAsReports = new ArrayList<>();
            for (ReportEntity r : pending) {
                FaultReport f = new FaultReport();
                f.report_id = -1;
                f.reference_number = r.reference_number;
                f.title = r.title;
                f.description = r.description;
                if ("Failed".equalsIgnoreCase(r.sync_status)) {
                    f.status = "Sync failed";
                } else if ("Syncing".equalsIgnoreCase(r.sync_status)) {
                    f.status = "Syncing";
                } else {
                    f.status = "Pending sync";
                }
                f.priority_score = r.priority_score;
                f.created_at = formatLocalTimestamp(r.created_at_local);
                f.setPendingSync(true);
                localAsReports.add(f);
            }

            int pendingCount = localAsReports.size();

            // 2) Then Supabase
            List<FaultReport> serverReports;
            try {
                serverReports = guest
                        ? SupabaseReports.getGuestReports(guestAuthId, accessToken)
                        : SupabaseReports.getUserReports(citizenProfileId, accessToken);
                if (serverReports == null) serverReports = new ArrayList<>();
            } catch (Exception e) {
                Log.e(TAG, "Server fetch failed: " + e.getMessage(), e);
                serverReports = new ArrayList<>();
                if (localAsReports.isEmpty()) {
                    final String msg = "Could not load reports. Check your connection and try again.";
                    runOnUiThread(() -> {
                        updateSyncButton(0);
                        showEmptyState(msg);
                    });
                    return;
                }
            }

            final List<FaultReport> serverFinal = serverReports;
            final int pendingFinal = pendingCount;

            runOnUiThread(() -> {
                // An earlier load must not display another account's reports after sign-in changes.
                SessionManager current = new SessionManager(this);
                if (current.isGuest() != guest
                        || (guest ? !guestAuthId.equals(current.getUserId())
                        : current.getCitizenProfileId() != citizenProfileId)) return;
                Set<String> onlineReferences = new HashSet<>();
                for (FaultReport serverReport : serverFinal) {
                    onlineReferences.add(serverReport.getReferenceNumber());
                }
                allReports.clear();
                for (FaultReport localReport : localAsReports) {
                    // A queued photo belongs to an already submitted report.
                    // Show the server report once, while keeping the sync button count.
                    if (!onlineReferences.contains(localReport.getReferenceNumber())) {
                        allReports.add(localReport);
                    }
                }
                allReports.addAll(serverFinal);      // then server rows
                updateSyncButton(pendingFinal);
                applyFilter();
            });
        }).start();
    }

    private String formatLocalTimestamp(long millis) {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
        return sdf.format(new java.util.Date(millis));
    }

    private void updateSyncButton(int pendingCount) {
        if (btnSyncPending == null) return;
        pendingSyncCount = pendingCount;
        if (pendingCount > 0) {
            btnSyncPending.setVisibility(View.VISIBLE);
            if (activeSyncState == WorkInfo.State.RUNNING) {
                btnSyncPending.setEnabled(false);
                btnSyncPending.setText("Syncing…");
            } else if (activeSyncState == WorkInfo.State.ENQUEUED) {
                btnSyncPending.setEnabled(false);
                btnSyncPending.setText("Sync queued (" + pendingCount + ")");
            } else {
                btnSyncPending.setEnabled(true);
                btnSyncPending.setText("⟳ Sync pending (" + pendingCount + ")");
            }
        } else {
            btnSyncPending.setVisibility(View.GONE);
        }
    }

    private void applyFilter() {
        filteredReports.clear();

        for (FaultReport r : allReports) {
            if (r.isPendingSync()) {
                if (currentFilter.equals("All") || currentFilter.equals("Pending")) {
                    filteredReports.add(r);
                }
                continue;
            }

            String status = r.getStatus() != null ? r.getStatus() : "Pending";
            boolean include;

            if ("All".equalsIgnoreCase(currentFilter)) {
                include = true;
            } else if ("Fixed".equalsIgnoreCase(currentFilter)) {
                // Catch "Fixed" and any legacy "resolved" variants
                String s = status.toLowerCase(Locale.US);
                include = s.equals("fixed") || s.equals("resolved") || s.equals("completed") || s.equals("closed");
            } else {
                // Direct match for Pending, Under Review, Accepted, In Progress
                include = status.equalsIgnoreCase(currentFilter);
            }

            if (include) {
                filteredReports.add(r);
            }
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

    private void onReportClicked(FaultReport report) {
        if (report.isPendingSync()) {
            android.widget.Toast.makeText(this,
                    "Sync failed".equals(report.getStatus())
                            ? "This report could not sync. Tap Sync pending to retry."
                            : "This report hasn't synced yet — try Sync pending first.",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

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

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing() || !AppNavigation.requireCitizen(this)) return;
        loadReports();
    }
}
