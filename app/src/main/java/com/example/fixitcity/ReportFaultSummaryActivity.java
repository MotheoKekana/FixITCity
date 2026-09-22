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

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseClient;
import com.example.fixitcity.data.SupabaseReports;
import com.google.android.material.textfield.TextInputEditText;

public class ReportFaultSummaryActivity extends AppCompatActivity {

    private static final String TAG = "ReportSummary";

    private TextView tvCategory, tvPhoto, tvLocation;
    private TextInputEditText etDetails;
    private ReportData reportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_fault_summary);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        reportData = ((FixITCityApp) getApplication()).getReportData();

        tvCategory = findViewById(R.id.tv_category_value);
        tvPhoto = findViewById(R.id.tv_photo_value);
        tvLocation = findViewById(R.id.tv_location_value);
        etDetails = findViewById(R.id.et_details);

        tvCategory.setText(reportData.getCategory());
        tvPhoto.setText(reportData.getPhotoCount() + " Image" + (reportData.getPhotoCount() > 1 ? "s" : "") + " Attached");
        tvLocation.setText(reportData.getLocationAddress());

        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            reportData.setAdditionalDetails(etDetails.getText().toString());
            submitReportToSupabase();
        });

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
                    startActivity(new Intent(this, TrackReportSearchActivity.class)));
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileSettingsActivity.class)));
        }
    }

    private void submitReportToSupabase() {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();
        long citizenProfileId = session.getCitizenProfileId();

        if (accessToken == null || session.isGuest()) {
            Toast.makeText(this, "Please log in to submit a report", Toast.LENGTH_LONG).show();
            return;
        }

        if (citizenProfileId <= 0) {
            Toast.makeText(this,
                    "Your account profile is incomplete. Please log out, then register a new account.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        long categoryId = reportData.getCategoryId();
        if (categoryId <= 0) {
            Toast.makeText(this, "Please select a fault category", Toast.LENGTH_LONG).show();
            return;
        }

        View submitBtn = findViewById(R.id.btn_submit);
        submitBtn.setEnabled(false);

        new Thread(() -> {
            try {
                Log.d(TAG, "Using citizen_profile_id = " + citizenProfileId);
                Log.d(TAG, "Using category_id = " + categoryId);
                Log.d(TAG, "Using severity_weight = " + reportData.getCategorySeverityWeight());

                FaultReport created = SupabaseReports.submitReport(
                        citizenProfileId,
                        categoryId,
                        reportData.getCategorySeverityWeight(),
                        reportData.getCategory(),
                        reportData.getAdditionalDetails(),
                        reportData.getLatitude(),
                        reportData.getLongitude(),
                        reportData.getLocationAddress(),
                        accessToken
                );

                Log.d(TAG, "Report created: " + created.getReferenceNumber());

                byte[] photoBytes = reportData.getPhotoBytes();
                if (photoBytes != null && photoBytes.length > 0) {
                    try {
                        String storagePath = "reports/"
                                + created.getReportId() + "/"
                                + System.currentTimeMillis() + ".jpg";

                        Log.d(TAG, "Uploading photo, size = " + photoBytes.length + " bytes");
                        String publicUrl = SupabaseClient.uploadToStorage(
                                SupabaseClient.REPORT_PHOTOS_BUCKET,
                                storagePath,
                                photoBytes,
                                reportData.getPhotoMimeType() != null ? reportData.getPhotoMimeType() : "image/jpeg",
                                accessToken
                        );

                        Log.d(TAG, "Photo uploaded: " + publicUrl);

                        SupabaseReports.insertPhoto(
                                created.getReportId(),
                                publicUrl,
                                storagePath,
                                true,
                                accessToken
                        );

                        Log.d(TAG, "Photo row inserted");
                    } catch (Exception photoEx) {
                        Log.e(TAG, "Photo upload failed (report still saved): " + photoEx.getMessage(), photoEx);
                    }
                }

                final int finalPriority = created.getPriorityScore() != null
                        ? created.getPriorityScore()
                        : SupabaseReports.computePriorityScore(reportData.getCategorySeverityWeight());

                final FaultReport reportFinal = created;
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, ReportSuccessActivity.class);
                    intent.putExtra("reference_number", reportFinal.getReferenceNumber());
                    intent.putExtra("priority_score", finalPriority);
                    startActivity(intent);

                    ((FixITCityApp) getApplication()).clearReportData();
                    finishAffinity();
                });

            } catch (Exception e) {
                Log.e(TAG, "Submit error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    submitBtn.setEnabled(true);
                    Toast.makeText(this, "Submit failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}