package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseClient;

public class Splash_Activity extends AppCompatActivity {

    private static final String TAG = "SupabaseTest";
    private static final int SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Test Supabase connection in background
        new Thread(() -> {
            try {
                String response = SupabaseClient.get("/fault_categories?select=*");
                Log.d(TAG, "✅ Supabase connected! Response: " + response);
            } catch (Exception e) {
                Log.e(TAG, "❌ Supabase connection failed: " + e.getMessage(), e);
            }
        }).start();

        // After delay, decide where to go
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);

            if (session.isLoggedIn()) {
                // Already logged in → go to dashboard
                startActivity(new Intent(Splash_Activity.this, Guest_Dashboard_Activity.class));
            } else {
                // Not logged in → go to login
                startActivity(new Intent(Splash_Activity.this, Login_Activity.class));
            }

            finish();
        }, SPLASH_DELAY_MS);
        new SessionManager(this).logout();

    }
}