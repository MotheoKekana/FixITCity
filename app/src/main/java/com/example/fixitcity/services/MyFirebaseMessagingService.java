package com.example.fixitcity.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.fixitcity.R;
import com.example.fixitcity.Splash_Activity;
import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseUsers;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "fixitcity_default";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed");

        SessionManager session = new SessionManager(this);
        long userId = session.getUserIdBigint();
        String accessToken = session.getAccessToken();

        if (userId > 0 && accessToken != null && !session.isGuest()) {
            new Thread(() -> {
                try {
                    SupabaseUsers.saveDeviceToken(userId, token, getDeviceName(), accessToken);
                    Log.d(TAG, "Device token saved for user " + userId);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save FCM token: " + e.getMessage(), e);
                }
            }).start();
        } else {
            Log.d(TAG, "No logged-in user — deferring token save until login");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        SessionManager session = new SessionManager(this);
        String recipient = message.getData().get("recipient_user_id");
        if (!session.isLoggedIn() || session.isGuest() || session.getUserIdBigint() <= 0
                || (recipient != null && !recipient.equals(String.valueOf(session.getUserIdBigint())))) {
            Log.d(TAG, "Ignored push: no matching signed-in citizen");
            return;
        }
        Log.d(TAG, "Report push received");

        String title = "FixITCity";
        String body = "";

        if (message.getNotification() != null) {
            CharSequence t = message.getNotification().getTitle();
            CharSequence b = message.getNotification().getBody();
            if (t != null) title = t.toString();
            if (b != null) body = b.toString();
        }

        if (message.getData().size() > 0) {
            if (message.getData().containsKey("title")) {
                title = message.getData().get("title");
            }
            if (message.getData().containsKey("body")) {
                body = message.getData().get("body");
            }
        }

        showNotification(title, body, message.getData().get("notification_id"));
    }

    private void showNotification(String title, String body, String notificationId) {
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted — dropping notification");
                return;
            }
        }

        // Reuse the normal startup route so a signed-in citizen keeps their session.
        Intent intent = new Intent(this, Splash_Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("open_notifications", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            if (notificationId != null && notificationId.matches("[1-9][0-9]{0,18}")) {
                NotificationManagerCompat.from(this).notify("report-update-" + notificationId, 0,
                        builder.build());
            } else {
                NotificationManagerCompat.from(this)
                        .notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to show notification: " + e.getMessage(), e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Report updates and municipal responses");
        manager.createNotificationChannel(channel);
    }

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
