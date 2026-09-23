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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class TrackReportResultActivity extends AppCompatActivity {

    private static final String TAG = "TrackResult";

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

        // ---- Read extras ----
        long reportId = getIntent().getLongExtra("report_id", -1);
        String ref = getIntent().getStringExtra("reference_number");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String address = getIntent().getStringExtra("address_line");
        String status = getIntent().getStringExtra("status");
        int priority = getIntent().getIntExtra("priority_score", 0);
        String createdAt = getIntent().getStringExtra("created_at");

        if (ref == null) ref = "#----";
        if (title == null) title = "Report";
        if (status == null) status = "Pending";
        if (address == null || address.isEmpty()) address = "Location unavailable";

        String dateStr = "—";
        if (createdAt != null && createdAt.length() >= 10) {
            dateStr = createdAt.substring(0, 10);
        }

        // ---- Bind header ----
        TextView tvHeader = findViewById(R.id.tv_report_status_header);
        if (tvHeader != null) {
            tvHeader.setText("Report Status: " + ref);
        }

        TextView tvTitle = findViewById(R.id.tv_report_title);
        if (tvTitle != null) tvTitle.setText(title);

        TextView tvLocation = findViewById(R.id.tv_report_location);
        if (tvLocation != null) tvLocation.setText("📍 " + address);

        TextView tvDate = findViewById(R.id.tv_report_date);
        if (tvDate != null) tvDate.setText("📅 Reported: " + dateStr);

        TextView tvPriority = findViewById(R.id.tv_report_priority);
        if (tvPriority != null) tvPriority.setText("❗ Priority Score: " + priority + "/100");

        TextView tvStatus = findViewById(R.id.tv_report_status);
        if (tvStatus != null) tvStatus.setText(status);

        TextView tvDescription = findViewById(R.id.tv_report_description);
        if (tvDescription != null && description != null && !description.isEmpty()) {
            tvDescription.setText(description);
            tvDescription.setVisibility(TextView.VISIBLE);
        }

        // ---- Share ----
        final String fRef = ref;
        final String fStatus = status;
        final String fAddress = address;

        MaterialButton btnShare = findViewById(R.id.btn_share_status);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT,
                        "FixITCity Report " + fRef + "\nStatus: " + fStatus +
                                "\nLocation: " + fAddress);
                startActivity(Intent.createChooser(share, "Share via"));
            });
        }

        MaterialButton btnNotify = findViewById(R.id.btn_notify_me);
        if (btnNotify != null) {
            btnNotify.setOnClickListener(v ->
                    Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show());
        }

        // ---- Status history (Phase 6.3) ----
        rvStatusHistory = findViewById(R.id.rv_status_history);
        tvHistoryEmpty = findViewById(R.id.tv_history_empty);

        if (rvStatusHistory != null) {
            rvStatusHistory.setLayoutManager(new LinearLayoutManager(this));
            historyAdapter = new StatusHistoryAdapter(historyItems);
            rvStatusHistory.setAdapter(historyAdapter);
        }

        if (reportId > 0) {
            loadStatusHistory(reportId);
        } else {
            // Fallback: look up report_id by reference if the caller didn't pass it
            Log.w(TAG, "No report_id passed — attempting lookup by reference: " + fRef);
            lookupReportIdAndLoadHistory(fRef);
        }

        // ---- Bottom navigation ----
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
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileSettingsActivity.class)));
        }

        View ivNotif = findViewById(R.id.ivNotifications);
        if (ivNotif != null) {
            ivNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, Notifications_Activity.class)));
        }
    }

    private void loadStatusHistory(long reportId) {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        if (tvHistoryEmpty != null) {
            tvHistoryEmpty.setText("Loading history…");
            tvHistoryEmpty.setVisibility(View.VISIBLE);
        }
        if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                List<SupabaseReports.StatusHistoryDTO> history =
                        SupabaseReports.getStatusHistory(reportId, accessToken);

                Log.d(TAG, "Loaded " + history.size() + " history rows for report " + reportId);

                List<StatusHistoryItem> uiItems = new ArrayList<>();
                for (SupabaseReports.StatusHistoryDTO dto : history) {
                    uiItems.add(new StatusHistoryItem(
                            dto.old_status,
                            dto.new_status,
                            dto.changed_at,
                            dto.notes
                    ));
                }

                runOnUiThread(() -> renderHistory(uiItems));

            } catch (Exception e) {
                Log.e(TAG, "Failed to load status history: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (tvHistoryEmpty != null) {
                        tvHistoryEmpty.setText("Could not load status history.");
                        tvHistoryEmpty.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    private void lookupReportIdAndLoadHistory(String referenceNumber) {
        if (tvHistoryEmpty != null) {
            tvHistoryEmpty.setText("Loading history…");
            tvHistoryEmpty.setVisibility(View.VISIBLE);
        }
        if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.GONE);

        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        new Thread(() -> {
            try {
                FaultReport report = SupabaseReports.getReportByReference(referenceNumber, accessToken);
                if (report == null) {
                    runOnUiThread(() -> {
                        if (tvHistoryEmpty != null) {
                            tvHistoryEmpty.setText("Status history unavailable.");
                            tvHistoryEmpty.setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }
                runOnUiThread(() -> loadStatusHistory(report.getReportId()));
            } catch (Exception e) {
                Log.e(TAG, "Lookup failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (tvHistoryEmpty != null) {
                        tvHistoryEmpty.setText("Could not load status history.");
                        tvHistoryEmpty.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    private void renderHistory(List<StatusHistoryItem> items) {
        historyItems.clear();
        historyItems.addAll(items);

        if (historyAdapter != null) {
            historyAdapter.notifyDataSetChanged();
        }

        if (items.isEmpty()) {
            if (tvHistoryEmpty != null) {
                tvHistoryEmpty.setText("No status changes yet. Your report is in the queue.");
                tvHistoryEmpty.setVisibility(View.VISIBLE);
            }
            if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.GONE);
        } else {
            if (tvHistoryEmpty != null) tvHistoryEmpty.setVisibility(View.GONE);
            if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.VISIBLE);
        }
    }
}