package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        // Back button (if you have one)
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        // Bottom navigation
        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navReport).setOnClickListener(v -> {
            startActivity(new Intent(this, ProximityCheckActivity.class));
            finish();
        });
        findViewById(R.id.navTrack).setOnClickListener(v ->
                Toast.makeText(this, "Track coming soon", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.navSettings).setOnClickListener(v ->
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        );
    }
}