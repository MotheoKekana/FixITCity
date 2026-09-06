package com.example.fixitcity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ReportSuccessActivity extends AppCompatActivity {

    private TextView tvReference, tvPriority, tvStatus;
    private String referenceNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_success);

        tvReference = findViewById(R.id.tv_reference_number);
        tvPriority = findViewById(R.id.tv_priority_score);
        tvStatus = findViewById(R.id.tv_status);

        // Generate reference
        referenceNumber = "FIX-" + new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date())
                + "-" + String.format("%03d", new Random().nextInt(1000));
        tvReference.setText(referenceNumber);

        // Priority – replace with actual value from API
        int priority = 92;
        tvPriority.setText(priority + "/100");
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
                    "\nPriority: " + priority + "/100" +
                    "\nStatus: Pending\n\nTrack at: https://fixitcity.gov/track/" + referenceNumber;
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // Go to Dashboard
        findViewById(R.id.btn_go_dashboard).setOnClickListener(v -> finish());

        // Report Another – clear data and start fresh
        findViewById(R.id.btn_report_another).setOnClickListener(v -> {
            ((FixITCityApp) getApplication()).clearReportData();
            Intent intent = new Intent(this, ProximityCheckActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Track Report
        findViewById(R.id.btn_track_report).setOnClickListener(v -> {
            Toast.makeText(this, "Tracking report: " + referenceNumber, Toast.LENGTH_SHORT).show();
            // Open tracking screen later
        });
    }
}