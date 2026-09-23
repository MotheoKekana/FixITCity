package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class TrackReportSearchActivity extends AppCompatActivity {

    private static final String TAG = "TrackSearch";

    private TextInputEditText etReference;
    private MaterialButton btnTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_report_search);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etReference = findViewById(R.id.et_reference);
        btnTrack = findViewById(R.id.btn_track);

        String prefill = getIntent().getStringExtra("prefill_reference");
        if (prefill != null && !prefill.isEmpty() && etReference != null) {
            etReference.setText(prefill);
        }

        btnTrack.setOnClickListener(v -> attemptTrack());

        View viewAll = findViewById(R.id.tv_view_all_archive);
        if (viewAll != null) {
            viewAll.setOnClickListener(v -> {
                Log.d(TAG, "Opening ReportHistoryActivity");
                startActivity(new Intent(this, ReportHistoryActivity.class));
            });
        }

        // Bottom navigation
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
            navTrack.setOnClickListener(v ->
                    Toast.makeText(this, "You're on Track", Toast.LENGTH_SHORT).show());
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

    private void attemptTrack() {
        String ref = etReference.getText() != null ? etReference.getText().toString().trim() : "";

        if (ref.isEmpty()) {
            Toast.makeText(this, "Please enter a reference number", Toast.LENGTH_SHORT).show();
            return;
        }

        btnTrack.setEnabled(false);

        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        new Thread(() -> {
            try {
                FaultReport report = SupabaseReports.getReportByReference(ref, accessToken);
                runOnUiThread(() -> {
                    btnTrack.setEnabled(true);
                    if (report == null) {
                        Toast.makeText(this,
                                "No report found with that reference number",
                                Toast.LENGTH_LONG).show();
                    } else {
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
                });
            } catch (Exception e) {
                Log.e(TAG, "Track failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    btnTrack.setEnabled(true);
                    Toast.makeText(this, "Track failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}