package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;

public class Onboard_Track_Reports_Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_onboard_track_reports);
        ScreenInsets.apply(this);

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            startActivity(new Intent(this, Onboard_Community_Impact_Activity.class));
            finish();
        });

        findViewById(R.id.btn_skip).setOnClickListener(v -> {
            new SessionManager(this).setSeenOnboarding(true);
            startActivity(new Intent(this, Login_Activity.class));
            finish();
        });
    }
}