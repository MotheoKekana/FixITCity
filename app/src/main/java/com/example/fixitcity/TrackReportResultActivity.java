package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class TrackReportResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_report_result);

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

        // Header
        TextView tvHeader = findViewById(R.id.tv_report_status_header);
        if (tvHeader != null) {
            tvHeader.setText("Report Status: " + ref);
        }

        // Details
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

        // ---- Final copies for lambda capture ----
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
    }
}