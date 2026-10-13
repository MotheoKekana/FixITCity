package com.example.fixitcity.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fixitcity.FaultReport;
import com.example.fixitcity.data.AppDatabase;
import com.example.fixitcity.data.ReportDao;
import com.example.fixitcity.data.ReportEntity;
import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SessionRefresher;
import com.example.fixitcity.data.SupabaseClient;
import com.example.fixitcity.data.SupabaseReports;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class SyncWorker extends Worker {

    public static final String TAG = "SyncWorker";
    private static final String PHOTO_DIR = "offline_photos";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SyncWorker starting");

        SessionManager session = new SessionManager(getApplicationContext());
        if (!session.isLoggedIn() || session.isGuest()) {
            Log.d(TAG, "Not logged in as citizen — aborting sync");
            return Result.success();
        }

        // 1) Refresh before touching queued rows. This prevents an expired JWT
        // from turning a temporary sync problem into a permanent failed report.
        SessionRefresher.Result refreshResult = SessionRefresher.refresh(session);
        if (refreshResult == SessionRefresher.Result.OFFLINE) {
            Log.d(TAG, "Cannot refresh while offline — retrying later");
            return Result.retry();
        }
        if (refreshResult == SessionRefresher.Result.INVALID_SESSION) {
            Log.w(TAG, "Saved session is invalid — user must sign in again");
            session.logout();
            return Result.success();
        }

        String accessToken = session.getAccessToken();
        if (accessToken == null) {
            Log.w(TAG, "No access token — aborting");
            return Result.success();
        }

        // 2) Load pending rows
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        ReportDao dao = db.reportDao();
        List<ReportEntity> pending = dao.getPending();

        Log.d(TAG, "Pending reports to sync: " + pending.size());

        if (pending.isEmpty()) {
            return Result.success();
        }

        boolean anyTransientFailure = false;

        for (ReportEntity row : pending) {
            row.sync_status = "Syncing";
            row.last_sync_attempt = System.currentTimeMillis();
            dao.update(row);

            try {
                syncOne(row, dao, session, accessToken);
            } catch (TransientException te) {
                Log.w(TAG, "Transient sync failure for " + row.reference_number + ": " + te.getMessage());
                row.sync_status = "Pending";  // will retry
                row.sync_error = te.getMessage();
                dao.update(row);
                anyTransientFailure = true;
            } catch (Exception e) {
                Log.e(TAG, "Permanent sync failure for " + row.reference_number, e);
                row.sync_status = "Failed";
                row.sync_error = e.getMessage();
                dao.update(row);
            }
        }

        if (anyTransientFailure) {
            return Result.retry();
        }
        return Result.success();
    }

    private void syncOne(ReportEntity row,
                         ReportDao dao,
                         SessionManager session,
                         String accessToken) throws Exception {

        // --- Idempotency: does it already exist on the server? ---
        boolean exists = SupabaseReports.reportExistsByReference(row.reference_number, accessToken);
        if (exists) {
            Log.d(TAG, "Report " + row.reference_number + " already on server — cleanup");
            deleteLocalPhoto(row.photo_filename);
            dao.delete(row);
            return;
        }

        // --- Insert report ---
        FaultReport created;
        try {
            created = SupabaseReports.submitReport(
                    row.citizen_id,
                    row.category_id,
                    row.reference_number,
                    row.title,
                    row.description,
                    row.latitude,
                    row.longitude,
                    row.address_line,
                    accessToken
            );
        } catch (IOException io) {
            // Network error → transient
            throw new TransientException(io.getMessage());
        }

        // --- Upload photo if present ---
        if (row.photo_filename != null && !row.photo_filename.isEmpty()) {
            try {
                byte[] bytes = readLocalPhoto(row.photo_filename);
                if (bytes != null && bytes.length > 0) {
                    String storagePath = "reports/"
                            + created.getReportId() + "/"
                            + System.currentTimeMillis() + ".jpg";

                    String publicUrl = SupabaseClient.uploadToStorage(
                            SupabaseClient.REPORT_PHOTOS_BUCKET,
                            storagePath,
                            bytes,
                            row.photo_mime_type != null ? row.photo_mime_type : "image/jpeg",
                            accessToken
                    );

                    SupabaseReports.insertPhoto(
                            created.getReportId(),
                            publicUrl,
                            storagePath,
                            true,
                            accessToken
                    );

                    Log.d(TAG, "Photo synced for " + row.reference_number);
                }
            } catch (Exception photoEx) {
                // Photo failure shouldn't lose the report — log and move on
                Log.e(TAG, "Photo sync failed (report saved): " + photoEx.getMessage());
            }
        }

        // --- Cleanup ---
        deleteLocalPhoto(row.photo_filename);
        dao.delete(row);
        Log.d(TAG, "Synced and removed: " + row.reference_number);
    }

    private byte[] readLocalPhoto(String filename) {
        try {
            File dir = new File(getApplicationContext().getFilesDir(), PHOTO_DIR);
            File f = new File(dir, filename);
            if (!f.exists()) return null;
            FileInputStream fis = new FileInputStream(f);
            byte[] bytes = new byte[(int) f.length()];
            int read = fis.read(bytes);
            fis.close();
            return read > 0 ? bytes : null;
        } catch (IOException e) {
            Log.e(TAG, "readLocalPhoto failed: " + e.getMessage());
            return null;
        }
    }

    private void deleteLocalPhoto(String filename) {
        if (filename == null || filename.isEmpty()) return;
        try {
            File dir = new File(getApplicationContext().getFilesDir(), PHOTO_DIR);
            File f = new File(dir, filename);
            if (f.exists() && f.delete()) {
                Log.d(TAG, "Deleted local photo: " + filename);
            }
        } catch (Exception e) {
            Log.w(TAG, "deleteLocalPhoto failed: " + e.getMessage());
        }
    }

    /** Marker for retryable failures (network hiccups). */
    private static class TransientException extends Exception {
        TransientException(String msg) { super(msg); }
    }
}
