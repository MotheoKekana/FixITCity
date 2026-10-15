package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.AppDatabase;
import com.example.fixitcity.data.ReportEntity;

import org.json.JSONArray;

import java.io.File;
import java.util.List;

public class Guest_Dashboard_Activity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_dashboard);

        session = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        AppNavigation.bindNotificationBell(this);

        View cardReport = findViewById(R.id.cardReportIssue);
        if (cardReport != null) {
            cardReport.setOnClickListener(v ->
                    startActivity(new Intent(this, ReportFaultCategoryActivity.class)));
        }

        View cardTrack = findViewById(R.id.cardTrackReport);
        if (cardTrack != null) {
            cardTrack.setOnClickListener(v ->
                    startActivity(new Intent(this, TrackReportSearchActivity.class)));
        }

        View cardRecent = findViewById(R.id.cardRecentReports);
        if (cardRecent != null) {
            cardRecent.setOnClickListener(v ->
                    startActivity(new Intent(this, ReportHistoryActivity.class)));
        }

        View cardNearby = findViewById(R.id.cardNearbyReports);
        if (cardNearby != null) cardNearby.setOnClickListener(v ->
                startActivity(new Intent(this, NearbyReportsActivity.class)));

        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> new Thread(() -> {
                int pending = AppDatabase.getInstance(getApplicationContext()).reportDao()
                        .getPendingForGuest(session.getUserId() + ":").size();
                runOnUiThread(() -> {
                    if (pending == 0) { leaveGuest(); return; }
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Unsynced guest reports")
                            .setMessage(pending + " report(s) still need to sync. Logging out permanently removes these pending guest reports. Stay signed in until they finish.")
                            .setNegativeButton("Stay signed in", null)
                            .setPositiveButton("Log out anyway", (dialog, which) -> leaveGuest())
                            .show();
                });
            }, "guest-pending-check").start());
            if (getIntent().getBooleanExtra("request_guest_logout", false)) {
                getIntent().removeExtra("request_guest_logout");
                btnLogout.performClick();
            }
        }
    }

    private void leaveGuest() {
        String guestPrefix = session.getUserId() + ":";
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                List<ReportEntity> rows = db.reportDao().getPendingForGuest(guestPrefix);
                for (ReportEntity row : rows) {
                    db.reportDao().delete(row);
                    if (row.photo_filename == null) continue;
                    JSONArray names = row.photo_filename.startsWith("[")
                            ? new JSONArray(row.photo_filename)
                            : new JSONArray().put(row.photo_filename);
                    for (int i = 0; i < names.length(); i++) {
                        String name = names.optString(i, "");
                        if (name.matches("[a-fA-F0-9-]{36}\\.jpg")) {
                            new File(new File(getFilesDir(), "offline_photos"), name).delete();
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("GuestDashboard", "Could not clear guest outbox", e);
                runOnUiThread(() -> Toast.makeText(this,
                        "Could not end guest session. Please try again.", Toast.LENGTH_LONG).show());
                return;
            }
            runOnUiThread(() -> {
                session.logout();
                Intent intent = new Intent(this, Login_Activity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }, "guest-logout").start();
    }
}
