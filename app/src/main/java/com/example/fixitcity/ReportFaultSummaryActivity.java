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

import com.example.fixitcity.data.AppDatabase;
import com.example.fixitcity.data.ReportEntity;
import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseClient;
import com.example.fixitcity.data.SupabaseReports;
import com.example.fixitcity.services.SyncScheduler;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ReportFaultSummaryActivity extends AppCompatActivity {

    private static final String TAG = "ReportSummary";
    private static final String PHOTO_DIR = "offline_photos";

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
        tvPhoto.setText(reportData.getPhotoCount() + " Image"
                + (reportData.getPhotoCount() > 1 ? "s" : "") + " Attached");
        tvLocation.setText(reportData.getLocationAddress());

        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            reportData.setAdditionalDetails(etDetails.getText().toString());
            submitReport();
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

    private void submitReport() {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();
        long citizenProfileId = session.getCitizenProfileId();

        // --- Validation (never queued) ---
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

        // --- Generate reference NOW so it's stable through queue ---
        final String referenceNumber = SupabaseReports.generateReferenceNumber();
        Log.d(TAG, "Generated reference: " + referenceNumber);

        View submitBtn = findViewById(R.id.btn_submit);
        submitBtn.setEnabled(false);

        new Thread(() -> {
            try {
                // Attempt Supabase
                FaultReport created = SupabaseReports.submitReport(
                        citizenProfileId,
                        categoryId,
                        reportData.getCategorySeverityWeight(),
                        referenceNumber,
                        reportData.getCategory(),
                        reportData.getAdditionalDetails(),
                        reportData.getLatitude(),
                        reportData.getLongitude(),
                        reportData.getLocationAddress(),
                        accessToken
                );

                Log.d(TAG, "Online submit success: " + created.getReferenceNumber());

                // Photo upload (best-effort — failure doesn't lose report)
                byte[] photoBytes = reportData.getPhotoBytes();
                if (photoBytes != null && photoBytes.length > 0) {
                    try {
                        String storagePath = "reports/"
                                + created.getReportId() + "/"
                                + System.currentTimeMillis() + ".jpg";
                        String publicUrl = SupabaseClient.uploadToStorage(
                                SupabaseClient.REPORT_PHOTOS_BUCKET,
                                storagePath,
                                photoBytes,
                                reportData.getPhotoMimeType() != null
                                        ? reportData.getPhotoMimeType() : "image/jpeg",
                                accessToken
                        );
                        SupabaseReports.insertPhoto(
                                created.getReportId(),
                                publicUrl,
                                storagePath,
                                true,
                                accessToken
                        );
                    } catch (Exception photoEx) {
                        Log.e(TAG, "Photo upload failed (report still saved): "
                                + photoEx.getMessage(), photoEx);
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
                    intent.putExtra("is_queued", false);
                    startActivity(intent);
                    ((FixITCityApp) getApplication()).clearReportData();
                    finishAffinity();
                });

            } catch (Exception e) {
                // Distinguish network failure from real errors
                boolean isNetworkFailure = isNetworkFailure(e);

                if (isNetworkFailure) {
                    Log.w(TAG, "Network failure — queueing offline: " + e.getMessage());
                    queueOffline(referenceNumber, session, citizenProfileId);
                } else {
                    Log.e(TAG, "Server/validation error: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        submitBtn.setEnabled(true);
                        Toast.makeText(this,
                                "Submit failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }


    // Decides whether an exception means "network problem"
    // or it is a "real error" so we must show the user. True = network problem.
    private boolean isNetworkFailure(Exception e)
    {
        if (e instanceof java.net.UnknownHostException) return true;    //DNS failed, cant find host
        if (e instanceof java.net.SocketTimeoutException) return true;  //Connection took too long
        if (e instanceof java.net.ConnectException) return true; //Connection refused
        if (e instanceof java.net.NoRouteToHostException) return true; //The network cannot be reached

        String message = e.getMessage(); //Pulls message
        if (message == null) return false; //If no message, nothing to search
        String lower = message.toLowerCase(); //Converts message to lowercase for case-insensitivity

        return lower.contains("failed to connect")      //Searching for potential network phrases
                || lower.contains("unable to resolve host")
                || lower.contains("network is unreachable")
                || lower.contains("timeout")
                || lower.contains("connection reset")
                || lower.contains("software caused connection abort");
    }

    private void queueOffline(String referenceNumber, SessionManager session, long citizenProfileId) {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // 1) Save photo bytes to disk
            String photoFilename = null;
            String photoMime = null;
            byte[] photoBytes = reportData.getPhotoBytes();
            if (photoBytes != null && photoBytes.length > 0) {
                photoFilename = UUID.randomUUID().toString() + ".jpg";
                photoMime = reportData.getPhotoMimeType() != null
                        ? reportData.getPhotoMimeType() : "image/jpeg";

                File dir = new File(getFilesDir(), PHOTO_DIR);
                if (!dir.exists()) dir.mkdirs();

                File out = new File(dir, photoFilename);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(photoBytes);
                }
                Log.d(TAG, "Saved offline photo: " + photoFilename
                        + " (" + photoBytes.length + " bytes)");
            }

            // 2) Insert Room outbox row
            ReportEntity row = new ReportEntity();
            row.offline_id = UUID.randomUUID().toString();
            row.citizen_id = citizenProfileId;
            row.category_id = reportData.getCategoryId();
            row.severity_weight = reportData.getCategorySeverityWeight();
            row.title = reportData.getCategory();
            row.description = reportData.getAdditionalDetails() != null
                    ? reportData.getAdditionalDetails() : "";
            row.latitude = reportData.getLatitude();
            row.longitude = reportData.getLongitude();
            row.address_line = reportData.getLocationAddress();
            row.reference_number = referenceNumber;
            row.priority_score = SupabaseReports.computePriorityScore(
                    reportData.getCategorySeverityWeight());
            row.photo_filename = photoFilename;
            row.photo_mime_type = photoMime;
            row.sync_status = "Pending";
            row.created_at_local = System.currentTimeMillis();

            db.reportDao().insert(row);
            Log.d(TAG, "Queued offline report: " + referenceNumber);

            // 3) Schedule sync
            SyncScheduler.scheduleSync(getApplicationContext());

            // 4) Show queued success screen
            final int finalPriority = row.priority_score;
            runOnUiThread(() -> {
                Intent intent = new Intent(this, ReportSuccessActivity.class);
                intent.putExtra("reference_number", referenceNumber);
                intent.putExtra("priority_score", finalPriority);
                intent.putExtra("is_queued", true);
                startActivity(intent);
                ((FixITCityApp) getApplication()).clearReportData();
                finishAffinity();
            });

        } catch (Exception queueEx) {
            Log.e(TAG, "Offline queue failed: " + queueEx.getMessage(), queueEx);
            runOnUiThread(() -> {
                View submitBtn = findViewById(R.id.btn_submit);
                submitBtn.setEnabled(true);
                Toast.makeText(this,
                        "Could not save report. Check your connection and try again.",
                        Toast.LENGTH_LONG).show();
            });
        }
    }
}