package com.example.fixitcity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SessionRefresher;
import com.example.fixitcity.data.SupabaseUsers;
import com.google.firebase.messaging.FirebaseMessaging;

public class Splash_Activity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long MIN_SPLASH_TIME_MS = 2000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long splashStartedAt;
    private boolean navigationScheduled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        splashStartedAt = SystemClock.elapsedRealtime();
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new Thread(this::restoreSession, "session-refresh").start();
    }

    private void restoreSession() {
        SessionManager session = new SessionManager(getApplicationContext());

        // A user with a saved session must never be forced back through
        // onboarding, including users affected by older versions that cleared
        // the onboarding flag during login.
        if (!session.hasSeenOnboarding() && !session.isLoggedIn()) {
            navigateAfterMinimumDelay(Onboard_Report_Faults_Activity.class);
            return;
        }

        if (!session.isLoggedIn()) {
            navigateAfterMinimumDelay(Login_Activity.class);
            return;
        }

        if (session.isGuest()) {
            navigateAfterMinimumDelay(Guest_Dashboard_Activity.class);
            return;
        }

        SessionRefresher.Result refreshResult = SessionRefresher.refresh(session);

        if (refreshResult == SessionRefresher.Result.INVALID_SESSION) {
            Log.i(TAG, "Saved session is no longer valid; returning to login");
            session.logout();
            navigateAfterMinimumDelay(Login_Activity.class);
            return;
        }

        if (session.getCitizenProfileId() <= 0 || session.getUserIdBigint() <= 0) {
            Log.w(TAG, "Saved session is missing the required account identifiers");
            session.logout();
            navigateAfterMinimumDelay(Login_Activity.class);
            return;
        }

        if (refreshResult == SessionRefresher.Result.REFRESHED) {
            // Only contact Supabase/Firebase after the fresh token is stored.
            registerDeviceToken(session);
        } else {
            Log.i(TAG, "Offline at startup; continuing with cached account data");
        }

        navigateAfterMinimumDelay(Dashboard_Activity.class);
    }

    private void navigateAfterMinimumDelay(Class<?> destination) {
        long elapsed = SystemClock.elapsedRealtime() - splashStartedAt;
        long remaining = Math.max(0L, MIN_SPLASH_TIME_MS - elapsed);

        mainHandler.postDelayed(() -> {
            if (navigationScheduled || isFinishing() || isDestroyed()) {
                return;
            }
            navigationScheduled = true;
            Intent intent = new Intent(Splash_Activity.this, destination);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, remaining);
    }

    /**
     * Save this device's FCM token after the Supabase session has been refreshed.
     */
    private void registerDeviceToken(SessionManager session) {
        long userId = session.getUserIdBigint();
        String accessToken = session.getAccessToken();

        if (userId <= 0 || accessToken == null || accessToken.trim().isEmpty()) {
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Could not obtain the FCM token", task.getException());
                        return;
                    }

                    String deviceToken = task.getResult();
                    if (deviceToken == null || deviceToken.trim().isEmpty()) {
                        return;
                    }

                    new Thread(() -> {
                        try {
                            SupabaseUsers.saveDeviceToken(
                                    userId,
                                    deviceToken,
                                    Build.MANUFACTURER + " " + Build.MODEL,
                                    accessToken
                            );
                            Log.d(TAG, "FCM token registered");
                        } catch (Exception e) {
                            Log.w(TAG, "Could not register the FCM token: " + e.getMessage());
                        }
                    }, "fcm-token-save").start();
                });
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
