package com.example.fixitcity.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public final class SupabaseNotifications {
    private static final Gson GSON = new Gson();

    private SupabaseNotifications() {}

    public static List<NotificationRow> getForCitizen(long citizenId,
                                                      String accessToken) throws IOException {
        if (citizenId <= 0) throw new IOException("No citizen profile is available");
        String path = "/notifications?citizen_id=eq." + citizenId
                + "&select=notification_id,citizen_id,report_id,subject,message,created_at,is_read"
                + "&order=created_at.desc&limit=100";
        String response = SupabaseClient.getWithAuth(path, accessToken);
        if (response == null || !response.trim().startsWith("[")) {
            throw new IOException("Could not load notifications");
        }
        Type type = new TypeToken<List<NotificationRow>>() {}.getType();
        List<NotificationRow> rows = GSON.fromJson(response, type);
        return rows != null ? rows : Collections.emptyList();
    }

    public static void markRead(long notificationId, long citizenId,
                                String accessToken) throws IOException {
        if (notificationId <= 0 || citizenId <= 0) return;
        String path = "/notifications?notification_id=eq." + notificationId
                + "&citizen_id=eq." + citizenId;
        String response = SupabaseClient.patchWithAuth(
                path, "{\"is_read\":true}", accessToken);
        if (response == null || response.trim().equals("[]")) {
            throw new IOException("The notification could not be marked as read");
        }
    }

    public static final class NotificationRow {
        public long notification_id;
        public long citizen_id;
        public Long report_id;
        public String subject;
        public String message;
        public String created_at;
        public boolean is_read;
    }
}
