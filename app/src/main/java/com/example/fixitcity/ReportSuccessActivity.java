package com.example.fixitcity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;

public class ReportSuccessActivity extends AppCompatActivity {

    private TextView tvReference, tvPriority, tvStatus;
    private String referenceNumber;
    private int priorityScore;
    private boolean isQueued;
    private boolean hasPriorityScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_success);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hideNavForGuests();

        tvReference = findViewById(R.id.tv_reference_number);
        tvPriority = findViewById(R.id.tv_priority_score);
        tvStatus = findViewById(R.id.tv_status);

        referenceNumber = getIntent().getStringExtra("reference_number");
        priorityScore = getIntent().getIntExtra("priority_score", 0);
        isQueued = getIntent().getBooleanExtra("is_queued", false);
        hasPriorityScore = !isQueued && getIntent().hasExtra("priority_score");

        if (referenceNumber == null) referenceNumber = "FIX-0000-0000";

        tvReference.setText(referenceNumber);
        tvPriority.setText(isQueued ? "After sync"
                : hasPriorityScore ? priorityScore + "/100" : "Calculating");
        tvStatus.setText(isQueued ? "Queued" : "Pending");
        ((TextView) findViewById(R.id.tv_success_title)).setText(
                isQueued ? "Report saved on this phone" : "Report submitted!");
        ((TextView) findViewById(R.id.tv_success_message)).setText(isQueued
                ? "It will be sent when you're online. Keep this reference number to track it later."
                : "Your report has been sent. Keep this reference number to track its progress.");

        findViewById(R.id.btn_copy_ref).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Reference", referenceNumber);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Reference copied: " + referenceNumber, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_share).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareText = "FixITCity Report\nReference: " + referenceNumber
                    + (hasPriorityScore ? "\nPriority: " + priorityScore + "/100" : "")
                    + "\nStatus: " + (isQueued ? "Queued (will sync when online)" : "Pending");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

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

        findViewById(R.id.btn_report_another).setOnClickListener(v -> {
            ((FixITCityApp) getApplication()).clearReportData();
            Intent intent = new Intent(this, ReportFaultCategoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        findViewById(R.id.btn_track_report).setOnClickListener(v -> {
            if (isQueued) {
                Toast.makeText(this, "You can track this report after it syncs", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(this, TrackReportSearchActivity.class);
            intent.putExtra("prefill_reference", referenceNumber);
            startActivity(intent);
        });
    }

    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
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
