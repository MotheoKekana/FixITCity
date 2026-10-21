package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class Guest_Mode_Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppNavigation.requireGuestSession(this)) return;
        setContentView(R.layout.activity_guest_mode);

        ScreenInsets.apply(this);

        View btnContinue = findViewById(R.id.btnContinue);
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                startActivity(new Intent(this, Guest_Dashboard_Activity.class));
                finish();
            });
        }
    }
}