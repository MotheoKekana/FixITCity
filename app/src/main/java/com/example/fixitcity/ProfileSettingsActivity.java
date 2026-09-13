package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;

public class ProfileSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        // ===== Logout button =====
        View logoutBtn = findViewById(R.id.btnLogout);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);
                session.logout();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Login_Activity.class));
                finishAffinity();
            });
        }

        // ===== Back button (if it exists) =====
        View backBtn = findViewById(R.id.back_button);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        // ===== Bottom navigation (custom LinearLayouts) =====
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(this, Guest_Dashboard_Activity.class));
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
                    Toast.makeText(this, "Track coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    Toast.makeText(this, "You're already in Settings", Toast.LENGTH_SHORT).show()
            );
        }
    }
}