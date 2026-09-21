package com.example.fixitcity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;

public class ReportSuccessActivity extends AppCompatActivity {

    private TextView tvReference, tvPriority, tvStatus;
    private String referenceNumber;
    private int priorityScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_success);

        tvReference = findViewById(R.id.tv_reference_number);
        tvPriority = findViewById(R.id.tv_priority_score);
        tvStatus = findViewById(R.id.tv_status);

        // Get real data from previous activity
        referenceNumber = getIntent().getStringExtra("reference_number");
        priorityScore = getIntent().getIntExtra("priority_score", 0);

        if (referenceNumber == null) referenceNumber = "FIX-0000-0000";

        tvReference.setText(referenceNumber);
        tvPriority.setText(priorityScore + "/100");
        tvStatus.setText("Pending");

        // Copy Ref
        findViewById(R.id.btn_copy_ref).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Reference", referenceNumber);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Reference copied: " + referenceNumber, Toast.LENGTH_SHORT).show();
        });

        // Share
        findViewById(R.id.btn_share).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareText = "FixITCity Report\nReference: " + referenceNumber +
                    "\nPriority: " + priorityScore + "/100" +
                    "\nStatus: Pending\n\nTrack at: https://fixitcity.gov/track/" + referenceNumber;
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // ===== FIXED: Go to Dashboard =====
        // We can't just finish() here because ReportFaultSummaryActivity called
        // finishAffinity() after starting this activity, so the whole task stack
        // is gone. Instead, launch Dashboard_Activity explicitly and clear the task.
        findViewById(R.id.btn_go_dashboard).setOnClickListener(v -> {
            SessionManager session = new SessionManager(this);
            Class<?> target = session.isGuest()
                    ? Guest_Dashboard_Activity.class
                    : Dashboard_Activity.class;

            Intent intent = new Intent(this, target);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Report Another
        findViewById(R.id.btn_report_another).setOnClickListener(v -> {
            ((FixITCityApp) getApplication()).clearReportData();
            Intent intent = new Intent(this, ReportFaultCategoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Track Report
        findViewById(R.id.btn_track_report).setOnClickListener(v -> {
            Intent intent = new Intent(this, TrackReportSearchActivity.class);
            intent.putExtra("prefill_reference", referenceNumber);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        // Same as tapping "Go to Dashboard" — we can't pop the stack because it's empty
        SessionManager session = new SessionManager(this);
        Class<?> target = session.isGuest()
                ? Guest_Dashboard_Activity.class
                : Dashboard_Activity.class;

        Intent intent = new Intent(this, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}