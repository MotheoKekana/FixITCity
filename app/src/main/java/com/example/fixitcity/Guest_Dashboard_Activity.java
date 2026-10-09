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

public class Guest_Dashboard_Activity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_dashboard);

        session = new SessionManager(this);

        // Keep content above system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Notifications bell
        ImageView ivNotif = findViewById(R.id.ivNotifications);
        if (ivNotif != null) {
            ivNotif.setOnClickListener(v ->
                    startActivity(new Intent(this, Notifications_Activity.class)));
        }

        // Card: Report an Issue
        View cardReport = findViewById(R.id.cardReportIssue);
        if (cardReport != null) {
            cardReport.setOnClickListener(v ->
                    startActivity(new Intent(this, ReportFaultCategoryActivity.class)));
        }

        // Card: Track Report
        View cardTrack = findViewById(R.id.cardTrackReport);
        if (cardTrack != null) {
            cardTrack.setOnClickListener(v ->
                    startActivity(new Intent(this, TrackReportSearchActivity.class)));
        }
        // Card Recent Reports — coming soon for guests
        View cardRecent = findViewById(R.id.cardRecentReports);
        if (cardRecent != null) {
            cardRecent.setOnClickListener(v ->
                    Toast.makeText(this, "Recent reports coming soon", Toast.LENGTH_SHORT).show());
        }
        // Logout button
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                session.logout();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, Login_Activity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}