package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Guest_Dashboard_Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_dashboard);

        // Get Started button – starts the report flow
//        Button btnGetStarted = findViewById(R.id.btnGetStarted);
//        btnGetStarted.setOnClickListener(v ->
//                startActivity(new Intent(this, ProximityCheckActivity.class))
//        );

        // Bottom navigation clicks
        findViewById(R.id.navHome).setOnClickListener(v ->
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.navReport).setOnClickListener(v ->
                startActivity(new Intent(this, ReportFaultCategoryActivity.class))
        );

        findViewById(R.id.navTrack).setOnClickListener(v ->
                Toast.makeText(this, "Track screen coming soon", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.navSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileSettingsActivity.class))
        );
    }
}