package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.AppDatabase;
import com.example.fixitcity.data.ReportEntity;
import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseClient;
import com.example.fixitcity.data.SupabaseReports;
import com.example.fixitcity.services.SyncScheduler;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
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

        ScreenInsets.apply(this);

        hideNavForGuests();
        AppNavigation.bindNotificationBell(this);

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

    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    private void submitReport() {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();
        long citizenProfileId = session.getCitizenProfileId();
        boolean guest = session.isGuest();
        String guestAuthId = guest ? session.getUserId() : null;

        if (accessToken == null || (guest && !session.hasGuestAuthSession())) {
            Toast.makeText(this, "Start a guest session to submit a report", Toast.LENGTH_LONG).show();
            return;
        }
        if (!guest && citizenProfileId <= 0) {
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

        final String referenceNumber = SupabaseReports.generateReferenceNumber();
        Log.d(TAG, "Generated reference: " + referenceNumber);

        View submitBtn = findViewById(R.id.btn_submit);
        submitBtn.setEnabled(false);

        new Thread(() -> {
            try {
                FaultReport created = guest
                        ? SupabaseReports.submitGuestReport(guestAuthId, categoryId,
                        referenceNumber, reportData.getCategory(),
                        reportData.getAdditionalDetails(), reportData.getLatitude(),
                        reportData.getLongitude(), reportData.getLocationAddress(), accessToken)
                        : SupabaseReports.submitReport(citizenProfileId, categoryId,
                        referenceNumber, reportData.getCategory(),
                        reportData.getAdditionalDetails(), reportData.getLatitude(),
                        reportData.getLongitude(), reportData.getLocationAddress(), accessToken);

                Log.d(TAG, "Online submit success: " + created.getReferenceNumber());

                List<byte[]> photos = reportData.getPhotos();
                if (!photos.isEmpty()) {
                    try {
                        for (int i = 0; i < photos.size(); i++) {
                            String storagePath = "reports/" + created.getReportId() + "/"
                                    + referenceNumber + "-" + (i + 1) + ".jpg";
                            // A retry may follow a partial upload or an uncertain response.
                            if (SupabaseReports.photoExists(created.getReportId(), storagePath, accessToken)) continue;
                            String publicUrl = SupabaseClient.uploadToStorage(
                                    SupabaseClient.REPORT_PHOTOS_BUCKET,
                                    storagePath, photos.get(i), "image/jpeg", accessToken);
                            SupabaseReports.insertPhoto(created.getReportId(), publicUrl,
                                    storagePath, i == 0, accessToken);
                        }
                    } catch (Exception photoEx) {
                        Log.e(TAG, "Photo upload failed; saving it for retry: "
                                + photoEx.getMessage(), photoEx);
                        queueOffline(referenceNumber, citizenProfileId, guestAuthId, true, created);
                        return;
                    }
                }

                showSuccess(referenceNumber, created.getPriorityScore(), false, false, false);

            } catch (Exception e) {
                boolean isNetworkFailure = isNetworkFailure(e);

                if (isNetworkFailure) {
                    Log.w(TAG, "Network failure — queueing offline: " + e.getMessage());
                    queueOffline(referenceNumber, citizenProfileId, guestAuthId, false, null);
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

    private boolean isNetworkFailure(Exception e) {
        if (e instanceof java.net.UnknownHostException) return true;
        if (e instanceof java.net.SocketTimeoutException) return true;
        if (e instanceof java.net.ConnectException) return true;
        if (e instanceof java.net.NoRouteToHostException) return true;

        String message = e.getMessage();
        if (message == null) return false;
        String lower = message.toLowerCase();

        return lower.contains("failed to connect")
                || lower.contains("unable to resolve host")
                || lower.contains("network is unreachable")
                || lower.contains("timeout")
                || lower.contains("connection reset")
                || lower.contains("software caused connection abort");
    }

    private void queueOffline(String referenceNumber, long citizenProfileId,
                              String guestAuthId,
                              boolean reportAlreadyOnline, FaultReport created) {
        List<File> savedPhotos = new ArrayList<>();
        boolean rowInserted = false;
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            JSONArray photoFilenames = new JSONArray();
            if (!reportData.getPhotos().isEmpty()) {
                File dir = new File(getFilesDir(), PHOTO_DIR);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new java.io.IOException("Could not create offline photo directory");
                }
                for (byte[] bytes : reportData.getPhotos()) {
                    String filename = UUID.randomUUID().toString() + ".jpg";
                    File out = new File(dir, filename);
                    savedPhotos.add(out);
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        fos.write(bytes);
                    }
                    photoFilenames.put(filename);
                }
            }

            ReportEntity row = new ReportEntity();
            row.offline_id = guestAuthId != null
                    ? guestAuthId + ":" + UUID.randomUUID() : UUID.randomUUID().toString();
            row.citizen_id = guestAuthId != null ? -1 : citizenProfileId;
            row.category_id = reportData.getCategoryId();
            row.severity_weight = reportData.getCategorySeverityWeight();
            row.title = reportData.getCategory();
            row.description = reportData.getAdditionalDetails() != null
                    ? reportData.getAdditionalDetails() : "";
            row.latitude = reportData.getLatitude();
            row.longitude = reportData.getLongitude();
            row.address_line = reportData.getLocationAddress();
            row.reference_number = referenceNumber;
            // The database calculates priority after this report syncs.
            // Keep the Room column for existing installs, but do not invent a score.
            row.priority_score = 0;
            // JSON in the existing Room column avoids dropping older pending reports.
            row.photo_filename = photoFilenames.length() == 0 ? null : photoFilenames.toString();
            row.photo_mime_type = photoFilenames.length() == 0 ? null : "image/jpeg";
            row.sync_status = "Pending";
            row.created_at_local = System.currentTimeMillis();

            db.reportDao().insert(row);
            rowInserted = true;
            Log.d(TAG, "Queued offline report: " + referenceNumber);

            SyncScheduler.scheduleSync(getApplicationContext());

            showSuccess(referenceNumber,
                    created != null ? created.getPriorityScore() : null,
                    !reportAlreadyOnline, reportAlreadyOnline, false);

        } catch (Exception queueEx) {
            if (!rowInserted) for (File photo : savedPhotos) photo.delete();
            Log.e(TAG, "Offline queue failed: " + queueEx.getMessage(), queueEx);
            if (reportAlreadyOnline) {
                // The report is already in Supabase. Never enable another submit
                // that would create a second report just because its photo failed.
                showSuccess(referenceNumber,
                        created != null ? created.getPriorityScore() : null,
                        false, false, true);
                return;
            }
            runOnUiThread(() -> {
                View submitBtn = findViewById(R.id.btn_submit);
                submitBtn.setEnabled(true);
                Toast.makeText(this,
                        "Could not save report. Check your connection and try again.",
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    private void showSuccess(String referenceNumber, Integer priorityScore,
                             boolean reportQueued, boolean photoQueued, boolean photoFailed) {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, ReportSuccessActivity.class);
            intent.putExtra("reference_number", referenceNumber);
            intent.putExtra("report_title", reportData.getCategory());
            if (priorityScore != null) intent.putExtra("priority_score", priorityScore);
            intent.putExtra("is_queued", reportQueued);
            intent.putExtra("photo_queued", photoQueued);
            intent.putExtra("photo_failed", photoFailed);
            startActivity(intent);
            ((FixITCityApp) getApplication()).clearReportData();
            finishAffinity();
        });
    }
}
