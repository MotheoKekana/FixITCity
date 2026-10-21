package com.example.fixitcity.data;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

public final class NotificationTokenManager {
    private static final String TAG = "NotificationTokens";

    private NotificationTokenManager() {}

    public static void registerAfterLogin(Context context, long userId, String accessToken) {
        if (userId <= 0 || accessToken == null || accessToken.isEmpty()) return;
        Context appContext = context.getApplicationContext();
        Log.d(TAG, "Registering FCM token after citizen login");

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Log.w(TAG, "Could not obtain FCM token after login", task.getException());
                return;
            }

            String deviceToken = task.getResult();
            Log.d(TAG, "FCM token fetched after citizen login");
            new Thread(() -> {
                SessionManager current = new SessionManager(appContext);
                // Ignore the token callback if the account changed while it ran.
                String currentAccessToken = current.getAccessToken();
                if (current.isGuest() || current.getUserIdBigint() != userId
                        || currentAccessToken == null || currentAccessToken.isEmpty()) {
                    Log.d(TAG, "Skipped FCM registration: citizen session changed");
                    return;
                }
                try {
                    SupabaseUsers.saveDeviceToken(userId, deviceToken,
                            Build.MANUFACTURER + " " + Build.MODEL, currentAccessToken);
                    Log.d(TAG, "FCM token registered after login");
                } catch (Exception e) {
                    Log.w(TAG, "Could not register FCM token after login", e);
                }
            }, "fcm-login-register").start();
        });
    }

    // Allow logout even if Firebase or Supabase is offline.
    public static void deactivateOnLogout(Context context, long userId, String accessToken) {
        if (userId <= 0 || accessToken == null || accessToken.isEmpty()) return;

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Log.w(TAG, "Could not obtain FCM token for logout", task.getException());
                return;
            }
            String deviceToken = task.getResult();
            new Thread(() -> {
                try {
                    SupabaseUsers.deactivateDeviceToken(userId, deviceToken, accessToken);
                    Log.d(TAG, "FCM token deactivated for former user");
                } catch (Exception e) {
                    Log.w(TAG, "Could not deactivate FCM token on logout", e);
                }
            }, "fcm-logout-deactivate").start();
        });
    }
}
