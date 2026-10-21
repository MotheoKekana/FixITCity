package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.AppDatabase;
import com.example.fixitcity.data.NetworkUtils;
import com.example.fixitcity.data.ReportEntity;
import com.example.fixitcity.data.SessionRefresher;
import com.example.fixitcity.services.SyncScheduler;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import com.example.fixitcity.data.SupabaseReports;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class TrackReportSearchActivity extends AppCompatActivity {

    private static final String TAG = "TrackSearch";

    private TextInputEditText etReference;
    private MaterialButton btnTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_report_search);

        ScreenInsets.apply(this);

        SessionManager initialSession = new SessionManager(this);
        if (!initialSession.isLoggedIn()
                || (initialSession.isGuest() && !initialSession.hasGuestAuthSession())) {
            AppNavigation.goHome(this);
            return;
        }
        hideNavForGuests();
        if (initialSession.isGuest()) {
            findViewById(R.id.recent_history_section).setVisibility(View.GONE);
        }

        etReference = findViewById(R.id.et_reference);
        btnTrack = findViewById(R.id.btn_track);

        String prefill = getIntent().getStringExtra("prefill_reference");
        if (prefill != null && !prefill.isEmpty() && etReference != null) {
            etReference.setText(prefill);
        }

        btnTrack.setOnClickListener(v -> attemptTrack());
        etReference.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                attemptTrack();
                return true;
            }
            return false;
        });

        View viewAll = findViewById(R.id.tv_view_all_archive);
        if (viewAll != null && !initialSession.isGuest()) {
            viewAll.setOnClickListener(v -> {
                Log.d(TAG, "Opening ReportHistoryActivity");
                startActivity(new Intent(this, ReportHistoryActivity.class));
            });
        }

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

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileSettingsActivity.class));
                finish();
            });
        }

        AppNavigation.bindNotificationBell(this);
    }

    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    private void attemptTrack() {
        if (!btnTrack.isEnabled()) return;
        String ref = etReference.getText() != null ? etReference.getText().toString().trim().toUpperCase(Locale.ROOT) : "";

        if (ref.isEmpty()) {
            Toast.makeText(this, "Please enter a reference number", Toast.LENGTH_SHORT).show();
            return;
        }

        btnTrack.setEnabled(false);

        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();
        boolean guest = session.isGuest();
        String ownerId = session.getUserId();

        new Thread(() -> {
            try {
                FaultReport report = guest
                        ? findGuestReport(ref, session, ownerId)
                        : SupabaseReports.getReportByReference(ref, accessToken);
                runOnUiThread(() -> {
                    if (!isCurrentSession(ownerId, guest)) return;
                    btnTrack.setEnabled(true);
                    if (report == null) {
                        Toast.makeText(this,
                                "No report found with that reference number",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Intent intent = new Intent(this, TrackReportResultActivity.class);
                        intent.putExtra("report_id", report.getReportId());
                        intent.putExtra("reference_number", report.getReferenceNumber());
                        intent.putExtra("title", report.getTitle());
                        intent.putExtra("description", report.getDescription());
                        intent.putExtra("address_line", report.getAddressLine());
                        intent.putExtra("status", report.getStatus());
                        if (report.getPriorityScore() != null) {
                            intent.putExtra("priority_score", report.getPriorityScore());
                        }
                        intent.putExtra("created_at", report.getCreatedAt());
                        intent.putExtra("external_guest", guest);
                        intent.putExtra("pending_sync", report.isPendingSync());
                        startActivity(intent);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Track failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (!isCurrentSession(ownerId, guest)) return;
                    btnTrack.setEnabled(true);
                    Toast.makeText(this, "Track failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    private boolean isCurrentSession(String ownerId, boolean guest) {
        if (isFinishing() || isDestroyed()) return false;
        SessionManager current = new SessionManager(this);
        return current.isLoggedIn() && current.isGuest() == guest
                && ownerId != null && ownerId.equals(current.getUserId());
    }

    /** Reference lookup supports this guest's pending outbox without a history screen. */
    private FaultReport findGuestReport(String reference, SessionManager session, String ownerId)
            throws IOException {
        ReportEntity queued = null;
        for (ReportEntity row : AppDatabase.getInstance(getApplicationContext())
                .reportDao().getPendingForGuest(ownerId + ":")) {
            if (reference.equalsIgnoreCase(row.reference_number)) {
                queued = row;
                break;
            }
        }
        // A local reference remains usable while the phone is offline.
        if (queued != null && !NetworkUtils.isOnline(this)) return pendingGuestReport(queued);
        if (!ownerId.equals(session.getUserId()) || !session.hasGuestAuthSession()) {
            throw new IOException("Your guest session changed. Please try again.");
        }
        SessionRefresher.Result result = SessionRefresher.refresh(session);
        if (result == SessionRefresher.Result.INVALID_SESSION) {
            throw new IOException("Your guest session expired. Please start guest mode again.");
        }
        if (!ownerId.equals(session.getUserId()) || !session.hasGuestAuthSession()) {
            throw new IOException("Your guest session changed. Please try again.");
        }
        try {
            FaultReport report = SupabaseReports.trackAsGuest(reference, session.getAccessToken());
            // A photo can still be queued for a report that already exists online.
            if (report != null) return report;
        } catch (IOException e) {
            if (queued == null) throw e;
            Log.w(TAG, "Could not reload report; showing the local pending reference", e);
        }
        return queued == null ? null : pendingGuestReport(queued);
    }

    private FaultReport pendingGuestReport(ReportEntity row) {
        FaultReport pending = new FaultReport();
        pending.reference_number = row.reference_number;
        pending.title = row.title;
        pending.status = "Failed".equals(row.sync_status) ? "Sync failed" : "Pending sync";
        pending.created_at = Instant.ofEpochMilli(row.created_at_local).toString();
        pending.setPendingSync(true);
        SyncScheduler.scheduleSync(getApplicationContext());
        return pending;
    }

}
