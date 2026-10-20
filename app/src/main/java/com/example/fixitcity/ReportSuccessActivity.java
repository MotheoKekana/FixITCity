package com.example.fixitcity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;

public class ReportSuccessActivity extends AppCompatActivity {

    private TextView tvReference, tvPriority, tvStatus;
    private String referenceNumber;
    private int priorityScore;
    private boolean isQueued;
    private boolean photoQueued;
    private boolean photoFailed;
    private boolean hasPriorityScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_success);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToDashboard();
            }
        });

        ScreenInsets.apply(this);

        hideNavForGuests();

        tvReference = findViewById(R.id.tv_reference_number);
        tvPriority = findViewById(R.id.tv_priority_score);
        tvStatus = findViewById(R.id.tv_status);

        referenceNumber = getIntent().getStringExtra("reference_number");
        priorityScore = getIntent().getIntExtra("priority_score", 0);
        isQueued = getIntent().getBooleanExtra("is_queued", false);
        photoQueued = getIntent().getBooleanExtra("photo_queued", false);
        photoFailed = getIntent().getBooleanExtra("photo_failed", false);
        hasPriorityScore = !isQueued && getIntent().hasExtra("priority_score");

        if (referenceNumber == null) referenceNumber = "FIX-0000-0000";

        tvReference.setText(referenceNumber);
        tvPriority.setText(isQueued ? "After sync"
                : hasPriorityScore ? priorityScore + "/100" : "Calculating");
        tvStatus.setText(isQueued ? "Queued" : "Pending");
        ((TextView) findViewById(R.id.tv_success_title)).setText(
                isQueued ? "Report saved on this phone"
                        : photoFailed ? "Report submitted; photo not saved"
                          : "Report submitted!");
        ((TextView) findViewById(R.id.tv_success_message)).setText(isQueued
                ? "It will be sent when you're online. Keep this reference number to track it later."
                : photoQueued ? "Your report was sent. Its photo will sync when the connection is available."
                  : photoFailed ? "Your report was sent, but its photo could not be saved for retry. Keep the reference number to track the report."
                    : "Your report has been sent. Keep this reference number to track its progress.");

        findViewById(R.id.btn_copy_ref).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Reference", referenceNumber);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Reference copied: " + referenceNumber, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_share).setOnClickListener(v ->
                ReportShare.share(this, v, new ReportShareData(
                        referenceNumber,
                        getIntent().getStringExtra("report_title"),
                        isQueued ? "Pending sync" : "Pending",
                        isQueued,
                        hasPriorityScore ? priorityScore : null,
                        java.time.ZonedDateTime.now())));

        findViewById(R.id.btn_go_dashboard).setOnClickListener(v -> goToDashboard());

        findViewById(R.id.btn_report_another).setOnClickListener(v -> {
            ((FixITCityApp) getApplication()).clearReportData();
            Intent intent = new Intent(this, ReportFaultCategoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        boolean guest = new SessionManager(this).isGuest();
        if (guest) {
            ((TextView) findViewById(R.id.btn_track_report)).setText("Track by reference");
        } else if (isQueued) {
            ((TextView) findViewById(R.id.btn_track_report)).setText("View pending report");
        }

        findViewById(R.id.btn_track_report).setOnClickListener(v -> {
            if (!guest && isQueued) {
                startActivity(new Intent(this, ReportHistoryActivity.class));
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

    private void goToDashboard() {
        SessionManager session = new SessionManager(this);

        Intent intent = session.isGuest()
                ? new Intent(this, Guest_Dashboard_Activity.class)
                : new Intent(this, Dashboard_Activity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
