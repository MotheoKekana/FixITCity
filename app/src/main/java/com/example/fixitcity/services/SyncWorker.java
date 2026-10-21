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
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

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
        if (!session.isLoggedIn() || (session.isGuest() && !session.hasGuestAuthSession())) {
            Log.d(TAG, "No valid report session — aborting sync");
            return Result.success();
        }

        // Refresh before syncing so an expired token does not fail queued reports.
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
        long citizenProfileId = session.getCitizenProfileId();
        String authUserId = session.getUserId();
        boolean guest = session.isGuest();
        if ((!guest && citizenProfileId <= 0) || authUserId == null || authUserId.isEmpty()) {
            Log.w(TAG, "No report identity for current session — leaving outbox untouched");
            return Result.success();
        }

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        ReportDao dao = db.reportDao();
        List<ReportEntity> pending = guest
                ? dao.getPendingForGuest(authUserId + ":")
                : dao.getPendingForCitizen(citizenProfileId);

        Log.d(TAG, "Pending reports to sync: " + pending.size());

        if (pending.isEmpty()) {
            return Result.success();
        }

        boolean anyTransientFailure = false;

        for (ReportEntity row : pending) {
            // Stop if the active account changes during sync.
            if (session.isGuest() != guest
                    || session.getCitizenProfileId() != citizenProfileId
                    || !authUserId.equals(session.getUserId())) {
                Log.d(TAG, "Account changed — leaving remaining queued reports untouched");
                return Result.success();
            }
            row.sync_status = "Syncing";
            row.last_sync_attempt = System.currentTimeMillis();
            dao.update(row);

            try {
                syncOne(row, dao, accessToken, guest ? authUserId : null);
            } catch (TransientException te) {
                Log.w(TAG, "Transient sync failure for " + row.reference_number + ": " + te.getMessage());
                row.sync_status = "Pending";
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
                         String accessToken, String guestAuthId) throws Exception {

        // Reuse an inserted report when only its photo upload failed.
        FaultReport created;
        try {
            created = SupabaseReports.getReportByReference(row.reference_number, accessToken);
            if (created == null) {
                created = guestAuthId != null
                        ? SupabaseReports.submitGuestReport(guestAuthId, row.category_id,
                        row.reference_number, row.title, row.description,
                        row.latitude, row.longitude, row.address_line, accessToken)
                        : SupabaseReports.submitReport(row.citizen_id, row.category_id,
                        row.reference_number, row.title, row.description,
                        row.latitude, row.longitude, row.address_line, accessToken);
            }
        } catch (IOException io) {
            throw new TransientException(io.getMessage());
        }
        if (created == null || created.getReportId() <= 0) {
            throw new IOException("Report was saved without a valid report ID");
        }

        // Keep the outbox and local files until every photo exists remotely.
        if (row.photo_filename != null && !row.photo_filename.isEmpty()) {
            try {
                boolean legacy = !row.photo_filename.startsWith("[");
                List<String> filenames = photoFilenames(row.photo_filename);
                for (int i = 0; i < filenames.size(); i++) {
                    String storagePath = "reports/" + created.getReportId() + "/"
                            + row.reference_number + (legacy ? "" : "-" + (i + 1)) + ".jpg";
                    if (SupabaseReports.photoExists(created.getReportId(), storagePath, accessToken)) continue;
                    byte[] bytes = readLocalPhoto(filenames.get(i));
                    String publicUrl = SupabaseClient.uploadToStorage(
                            SupabaseClient.REPORT_PHOTOS_BUCKET, storagePath, bytes,
                            row.photo_mime_type != null ? row.photo_mime_type : "image/jpeg",
                            accessToken);
                    SupabaseReports.insertPhoto(created.getReportId(), publicUrl,
                            storagePath, i == 0, accessToken);
                }
                Log.d(TAG, "Photos synced for " + row.reference_number);
            } catch (FileNotFoundException photoEx) {
                // A retry cannot restore a missing local photo.
                throw photoEx;
            } catch (IOException photoEx) {
                Log.w(TAG, "Photo sync will retry for " + row.reference_number, photoEx);
                throw new TransientException(photoEx.getMessage());
            } catch (Exception photoEx) {
                // Keep the local row and photo on unexpected failures.
                throw photoEx;
            }
        }

        dao.delete(row);
        for (String filename : photoFilenames(row.photo_filename)) deleteLocalPhoto(filename);
        Log.d(TAG, "Synced and removed: " + row.reference_number);
    }

    private byte[] readLocalPhoto(String filename) throws IOException {
        File dir = new File(getApplicationContext().getFilesDir(), PHOTO_DIR);
        File f = new File(dir, filename);
        if (!f.isFile()) throw new FileNotFoundException("Offline photo is missing: " + filename);
        try (FileInputStream fis = new FileInputStream(f);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int count;
            while ((count = fis.read(chunk)) != -1) {
                buffer.write(chunk, 0, count);
            }
            byte[] bytes = buffer.toByteArray();
            if (bytes.length == 0) throw new FileNotFoundException("Offline photo is empty: " + filename);
            return bytes;
        }
    }

    private List<String> photoFilenames(String value) throws JSONException {
        List<String> filenames = new ArrayList<>();
        if (value == null || value.isEmpty()) return filenames;
        if (!value.startsWith("[")) {
            filenames.add(value);
            return filenames;
        }
        JSONArray array = new JSONArray(value);
        if (array.length() < 1 || array.length() > 3) {
            throw new JSONException("Invalid offline photo count");
        }
        for (int i = 0; i < array.length(); i++) {
            String filename = array.getString(i);
            if (!filename.matches("[a-fA-F0-9-]{36}\\.jpg")) {
                throw new JSONException("Invalid offline photo filename");
            }
            filenames.add(filename);
        }
        return filenames;
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

    private static class TransientException extends Exception {
        TransientException(String msg) { super(msg); }
    }
}
