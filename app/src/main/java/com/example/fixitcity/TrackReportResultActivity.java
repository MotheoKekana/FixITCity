package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TrackReportResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_report_result);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Read extras
        long reportId       = getIntent().getLongExtra("report_id", -1);
        String reference    = getIntent().getStringExtra("reference_number");
        String title        = getIntent().getStringExtra("title");
        String description  = getIntent().getStringExtra("description");
        String addressLine  = getIntent().getStringExtra("address_line");
        String status       = getIntent().getStringExtra("status");
        int priorityScore   = getIntent().getIntExtra("priority_score", 0);
        String createdAt    = getIntent().getStringExtra("created_at");

        // Populate views (matching your XML IDs)
        setTextSafe(R.id.tv_report_title, title != null && !title.isEmpty() ? title : "Report");
        setTextSafe(R.id.tv_report_location, "📍 " + (addressLine != null && !addressLine.isEmpty() ? addressLine : "Unknown location"));
        setTextSafe(R.id.tv_report_date, "📅 Reported: " + (createdAt != null ? createdAt : "—"));
        setTextSafe(R.id.tv_report_priority, "❗ Priority Score: " + priorityScore + "/100");
        setTextSafe(R.id.tv_report_status, status != null && !status.isEmpty() ? status : "Pending");

        // Description is hidden by default; show it only if there's text
        TextView tvDesc = findViewById(R.id.tv_report_description);
        if (tvDesc != null) {
            if (description != null && !description.isEmpty()) {
                tvDesc.setText(description);
                tvDesc.setVisibility(View.VISIBLE);
            } else {
                tvDesc.setVisibility(View.GONE);
            }
        }

        // Share button — share the current status as text
        View btnShare = findViewById(R.id.btn_share_status);
        if (btnShare != null) {
            final String shareRef = reference;
            final String shareStatus = status;
            final String shareTitle = title;
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

        // Notify me — placeholder for now (would subscribe to push notifications)
        View btnNotify = findViewById(R.id.btn_notify_me);
        if (btnNotify != null) {
            btnNotify.setOnClickListener(v ->
                    Toast.makeText(this, "You'll be notified of status changes", Toast.LENGTH_SHORT).show());
        }

        // Status history placeholder
        setTextSafe(R.id.tv_history_empty, "No status history yet.");

        // Bottom nav
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, Guest_Dashboard_Activity.class);
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

        // Notification bell (in header)
        View ivNotif = findViewById(R.id.ivNotifications);
        if (ivNotif != null) {
            ivNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, Notifications_Activity.class)));
        }
    }

    private void setTextSafe(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(text);
    }
}