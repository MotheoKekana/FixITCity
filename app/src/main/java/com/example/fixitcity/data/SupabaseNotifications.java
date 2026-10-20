package com.example.fixitcity.data;

import com.example.fixitcity.FaultReport;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public final class SupabaseNotifications {
    private static final Gson GSON = new Gson();
    private static final Type ROWS = new TypeToken<List<NotificationRow>>() {}.getType();
    private static final Type REPORTS = new TypeToken<List<FaultReport>>() {}.getType();

    private SupabaseNotifications() {}

    public static List<NotificationRow> getForCitizen(long citizenId,
                                                      String accessToken) throws IOException {
        if (citizenId <= 0) throw new IOException("No citizen profile is available");
        String path = "/notifications?citizen_id=eq." + citizenId
                + "&select=notification_id,citizen_id,report_id,subject,message,created_at,is_read"
                + "&order=created_at.desc,notification_id.desc&limit=100";
        String response = SupabaseClient.getWithAuth(path, accessToken);
        return parseRows(response);
    }

    public static void markRead(long notificationId, long citizenId,
                                String accessToken) throws IOException {
        if (notificationId <= 0 || citizenId <= 0) throw new IOException("Invalid notification");
        String ownership = "/notifications?notification_id=eq." + notificationId
                + "&citizen_id=eq." + citizenId;
        JsonObject patch = new JsonObject();
        patch.addProperty("is_read", true);
        patch.addProperty("read_at", Instant.now().toString());
        List<NotificationRow> changed = parseRows(SupabaseClient.patchWithAuth(
                ownership + "&is_read=not.is.true", patch.toString(), accessToken));
        for (NotificationRow row : changed) {
            if (row.notification_id == notificationId && row.citizen_id == citizenId && row.is_read) return;
        }
        // Another device may already have read it. Keep that first read timestamp.
        for (NotificationRow row : parseRows(SupabaseClient.getWithAuth(
                ownership + "&select=notification_id,citizen_id,is_read&limit=1", accessToken))) {
            if (row.notification_id == notificationId && row.citizen_id == citizenId && row.is_read) return;
        }
        throw new IOException("The notification could not be marked as read");
    }

    /** Load the one owned report, rather than downloading the citizen's whole history. */
    public static FaultReport getReportForCitizen(long reportId, long citizenId,
                                                  String accessToken) throws IOException {
        if (reportId <= 0 || citizenId <= 0) return null;
        String response = SupabaseClient.getWithAuth("/fault_reports?report_id=eq." + reportId
                + "&citizen_id=eq." + citizenId
                + "&select=report_id,reference_number,title,description,address_line,status,priority_score,created_at"
                + "&limit=1", accessToken);
        try {
            if (response == null || !response.trim().startsWith("[")) {
                throw new IOException("Invalid report response");
            }
            List<FaultReport> reports = GSON.fromJson(response, REPORTS);
            return reports == null || reports.isEmpty() ? null : reports.get(0);
        } catch (com.google.gson.JsonParseException e) {
            throw new IOException("Could not read the report response", e);
        }
    }

    private static List<NotificationRow> parseRows(String response) throws IOException {
        if (response == null || !response.trim().startsWith("[")) {
            throw new IOException("Invalid notification response");
        }
        try {
            List<NotificationRow> rows = GSON.fromJson(response, ROWS);
            return rows != null ? rows : Collections.emptyList();
        } catch (com.google.gson.JsonParseException e) {
            throw new IOException("Could not read the notification response", e);
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
