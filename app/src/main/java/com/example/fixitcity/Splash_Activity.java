package com.example.fixitcity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseClient;
import com.example.fixitcity.data.SupabaseUsers;
import com.google.firebase.messaging.FirebaseMessaging;

public class Splash_Activity extends AppCompatActivity {

    private static final String TAG = "SupabaseTest";
    private static final int SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Quick connectivity test to Supabase
        new Thread(() -> {
            try {
                String response = SupabaseClient.get("/fault_categories?select=*");
                Log.d(TAG, "✅ Supabase connected! Response: " + response);
            } catch (Exception e) {
                Log.e(TAG, "❌ Supabase connection failed: " + e.getMessage(), e);
            }
        }).start();

        // Fetch + save FCM token (fires only after login so userIdBigint is present)
        registerDeviceToken();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);

            if (session.isLoggedIn() && !session.isGuest() && session.getCitizenProfileId() > 0) {
                startActivity(new Intent(Splash_Activity.this, Dashboard_Activity.class));
            } else if (session.isLoggedIn() && session.isGuest()) {
                startActivity(new Intent(Splash_Activity.this, Guest_Dashboard_Activity.class));
            } else {
                startActivity(new Intent(Splash_Activity.this, Login_Activity.class));
            }
            finish();
        }, SPLASH_DELAY_MS);
    }

    /**
     * Grab the current FCM token and push it to Supabase.
     * Only does something if a real (non-guest) user is logged in.
     */
    private void registerDeviceToken() {
        SessionManager session = new SessionManager(this);
        long userIdBigint = session.getUserIdBigint();
        String accessToken = session.getAccessToken();

        if (userIdBigint <= 0 || accessToken == null || session.isGuest()) {
            Log.d(TAG, "registerDeviceToken: no logged-in user yet — skipping");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM token: " + token);

                    new Thread(() -> {
                        try {
                            SupabaseUsers.saveDeviceToken(
                                    userIdBigint,
                                    token,
                                    Build.MANUFACTURER + " " + Build.MODEL,
                                    accessToken
                            );
                            Log.d(TAG, "FCM token saved to device_tokens");
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to save FCM token: " + e.getMessage(), e);
                        }
                    }).start();
                });
    }
}