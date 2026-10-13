package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;

import java.util.ArrayList;
import java.util.List;

public class TrackReportResultActivity extends AppCompatActivity {

    private static final String TAG = "TrackReportResult";

    private RecyclerView rvStatusHistory;
    private TextView tvHistoryEmpty;
    private StatusHistoryAdapter historyAdapter;
    private final List<StatusHistoryItem> historyItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_report_result);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hideNavForGuests();

        // ---------- Intent extras ----------
        long   reportId      = getIntent().getLongExtra("report_id", -1);
        String reference     = getIntent().getStringExtra("reference_number");
        String title         = getIntent().getStringExtra("title");
        String description   = getIntent().getStringExtra("description");
        String addressLine   = getIntent().getStringExtra("address_line");
        String status        = getIntent().getStringExtra("status");
        int    priorityScore = getIntent().getIntExtra("priority_score", 0);
        String createdAt     = getIntent().getStringExtra("created_at");

        // ---------- Report detail card ----------
        setTextSafe(R.id.tv_report_title,
                title != null && !title.isEmpty() ? title : "Report");

        setTextSafe(R.id.tv_report_location,
                 (addressLine != null && !addressLine.isEmpty()
                        ? addressLine : "Unknown location"));

        setTextSafe(R.id.tv_report_date,
                "Reported: " + (createdAt != null ? createdAt : "—"));

        setTextSafe(R.id.tv_report_priority,
                "Priority Score: " + priorityScore + "/100");

        TextView tvDesc = findViewById(R.id.tv_report_description);
        if (tvDesc != null) {
            if (description != null && !description.isEmpty()) {
                tvDesc.setText(description);
                tvDesc.setVisibility(View.VISIBLE);
            } else {
                tvDesc.setVisibility(View.GONE);
            }
        }

        // ---------- Share button ----------
        View btnShare = findViewById(R.id.btn_share_status);
        if (btnShare != null) {
            final String shareRef    = reference;
            final String shareStatus = status;
            final String shareTitle  = title;
            btnShare.setOnClickListener(v -> {
                String msg = "FixITCity Report\n"
                        + "Reference: " + (shareRef != null ? shareRef : "—") + "\n"
                        + "Title: " + (shareTitle != null ? shareTitle : "Report") + "\n"
                        + "Status: " + (shareStatus != null ? shareStatus : "Pending");
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, msg);
                startActivity(Intent.createChooser(shareIntent, "Share report status"));
            });
        }

        // ---------- Notify Me button (now lives under the timeline) ----------
        View btnNotify = findViewById(R.id.btn_notify_me);
        if (btnNotify != null) {
            btnNotify.setOnClickListener(v ->
                    Toast.makeText(this,
                            "You'll be notified of status changes",
                            Toast.LENGTH_SHORT).show());
        }

        // ---------- Status history timeline ----------
        rvStatusHistory = findViewById(R.id.rv_status_history);
        tvHistoryEmpty  = findViewById(R.id.tv_history_empty);

        historyAdapter = new StatusHistoryAdapter(historyItems);

        if (rvStatusHistory != null) {
            rvStatusHistory.setLayoutManager(new LinearLayoutManager(this));
            rvStatusHistory.setNestedScrollingEnabled(false);
            rvStatusHistory.setAdapter(historyAdapter);
        }

        // Show loading state while we fetch
        if (tvHistoryEmpty != null) {
            tvHistoryEmpty.setText("Loading history…");
            tvHistoryEmpty.setVisibility(View.VISIBLE);
        }
        if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.GONE);

        if (reportId > 0) {
            loadStatusHistory(reportId);
        } else {
            showHistoryEmpty("No status history available.");
        }

        // ---------- Bottom navigation ----------
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
            navReport.setOnClickListener(v ->
                    startActivity(new Intent(this, ReportFaultCategoryActivity.class)));
        }

        View navTrack = findViewById(R.id.navTrack);
        if (navTrack != null) {
            navTrack.setOnClickListener(v ->
                    Toast.makeText(this, "You're on Track", Toast.LENGTH_SHORT).show());
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileSettingsActivity.class)));
        }

        View ivNotif = findViewById(R.id.ivNotifications);
        if (ivNotif != null) {
            ivNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, Notifications_Activity.class)));
        }
    }

    /**
     * Fetches the status history for this report and populates the timeline.
     *
     * Adjust the method name / signature in {@link SupabaseReports} if yours
     * is different (e.g. getStatusHistoryForReport, fetchHistory, etc.).
     */
    private void loadStatusHistory(long reportId) {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        new Thread(() -> {
            List<StatusHistoryItem> fetched = new ArrayList<>();
            try {
                List<SupabaseReports.StatusHistoryDTO> dtos =
                        SupabaseReports.getStatusHistory(reportId, accessToken);

                if (dtos != null) {
                    for (SupabaseReports.StatusHistoryDTO dto : dtos) {
                        fetched.add(new StatusHistoryItem(
                                dto.old_status,     // ← adjust these field names
                                dto.new_status,     //   to match your DTO
                                dto.changed_at,
                                dto.notes
                        ));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load status history", e);
            }

            // Sort newest first
            fetched.sort((a, b) -> {
                String da = a.changedAt != null ? a.changedAt : "";
                String db = b.changedAt != null ? b.changedAt : "";
                return db.compareTo(da);
            });

            final List<StatusHistoryItem> finalList = fetched;
            runOnUiThread(() -> {
                historyItems.clear();
                historyItems.addAll(finalList);
                historyAdapter.notifyDataSetChanged();

                if (historyItems.isEmpty()) {
                    showHistoryEmpty("No status changes yet.");
                } else {
                    if (tvHistoryEmpty != null) tvHistoryEmpty.setVisibility(View.GONE);
                    if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void showHistoryEmpty(String message) {
        if (tvHistoryEmpty != null) {
            tvHistoryEmpty.setText(message);
            tvHistoryEmpty.setVisibility(View.VISIBLE);
        }
        if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.GONE);
    }

    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    private void setTextSafe(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(text);
    }
}