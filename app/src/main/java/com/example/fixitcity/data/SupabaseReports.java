package com.example.fixitcity.data;

import android.util.Log;

import com.example.fixitcity.FaultCategory;
import com.example.fixitcity.FaultReport;
import com.example.fixitcity.NearbyReport;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SupabaseReports {

    private static final String TAG = "SupabaseReports";
    private static final Gson gson = new Gson();

    public static String generateReferenceNumber() {
        String year = String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "FIX-" + year + "-" + suffix;
    }

    public static double haversineMeters(double latitude1, double longitude1,
                                         double latitude2, double longitude2) {
        final double rad = 6371000.0;
        double latRad1 = Math.toRadians(latitude1);
        double latRad2 = Math.toRadians(latitude2);
        double latDiffRad = Math.toRadians(latitude2 - latitude1);
        double longDiffRad = Math.toRadians(longitude2 - longitude1);
        double sinHalfLat = Math.sin(latDiffRad / 2);
        double sinHalfLon = Math.sin(longDiffRad / 2);
        double intermediateVal = sinHalfLat * sinHalfLat
                + Math.cos(latRad1) * Math.cos(latRad2) * sinHalfLon * sinHalfLon;
        double centralAngle = 2 * Math.atan2(Math.sqrt(intermediateVal),
                Math.sqrt(1 - intermediateVal));
        return rad * centralAngle;
    }

    public static List<FaultReport> getOpenReports(String accessToken) throws IOException {
        String path = "/fault_reports?status=in.(Pending,Assigned)"
                + "&order=created_at.desc";
        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }
        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<FaultReport>>(){}.getType();
        List<FaultReport> results = gson.fromJson(response, listType);
        return results != null ? results : new ArrayList<>();
    }

    /** Open reports within 2 km, including reports from other citizens. */
    public static List<FaultReport> getNearbyReports(
            double latitude, double longitude, String accessToken) throws IOException {
        if (Double.isNaN(latitude) || Double.isInfinite(latitude)
                || Double.isNaN(longitude) || Double.isInfinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw new IOException("Invalid location for nearby reports");
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IOException("Sign in to view nearby reports");
        }

        String body = "{\"lat\":" + latitude + ",\"lng\":" + longitude + "}";
        String response = SupabaseClient.postWithAuth(
                "/rpc/nearby_reports", body, accessToken);
        if (response == null || !response.trim().startsWith("[")) {
            throw new IOException("Invalid nearby reports response");
        }

        Type listType = new TypeToken<List<FaultReport>>() {}.getType();
        List<FaultReport> reports = gson.fromJson(response, listType);
        return reports != null ? reports : new ArrayList<>();
    }

    public static List<FaultCategory> getCategories(String accessToken) throws IOException {
        String path = "/fault_categories?select=*&order=name.asc";
        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }
        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<List<FaultCategoryDTO>>(){}.getType();
            List<FaultCategoryDTO> dtos = gson.fromJson(response, listType);
            List<FaultCategory> result = new ArrayList<>();
            if (dtos != null) {
                for (FaultCategoryDTO dto : dtos) {
                    result.add(new FaultCategory(
                            dto.category_id,
                            dto.name,
                            dto.description,
                            dto.severity_weight,
                            dto.estimated_fix_time,
                            android.R.drawable.ic_menu_report_image));
                }
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Deprecated
    public static List<FaultCategory> getCategories() throws IOException {
        return getCategories(null);
    }

    public static long getCategoryIdByName(String name) throws IOException {
        String path = "/fault_categories?name=eq." + name + "&select=category_id&limit=1";
        String response = SupabaseClient.get(path);
        if (response == null || !response.trim().startsWith("[")) return -1;
        Type listType = new TypeToken<List<CategoryIdDTO>>(){}.getType();
        List<CategoryIdDTO> results = gson.fromJson(response, listType);
        if (results == null || results.isEmpty()) return -1;
        return results.get(0).category_id;
    }

    public static FaultReport submitReport(
            long citizenId,
            long categoryId,
            String referenceNumber,
            String title,
            String description,
            double latitude,
            double longitude,
            String addressLine,
            String accessToken
    ) throws IOException {

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"citizen_id\":").append(citizenId).append(",");
        json.append("\"category_id\":").append(categoryId).append(",");
        json.append("\"title\":\"").append(escape(title)).append("\",");
        json.append("\"description\":\"").append(escape(description)).append("\",");
        json.append("\"latitude\":").append(latitude).append(",");
        json.append("\"longitude\":").append(longitude).append(",");
        json.append("\"address_line\":\"").append(escape(addressLine)).append("\",");
        json.append("\"status\":\"Pending\",");
        json.append("\"reference_number\":\"").append(referenceNumber).append("\",");
        json.append("\"is_offline\":false,");
        json.append("\"sync_status\":\"Synced\",");
        json.append("\"is_duplicate\":false");
        json.append("}");

        String response = SupabaseClient.postWithAuth("/fault_reports", json.toString(), accessToken);

        Type listType = new TypeToken<List<FaultReport>>(){}.getType();
        List<FaultReport> created = gson.fromJson(response, listType);
        if (created == null || created.isEmpty()) {
            throw new IOException("Failed to create report");
        }

        // The database trigger calculates priority after insert. The POST response
        // can still contain the pre-trigger value, so reload once for display.
        try {
            FaultReport saved = getReportByReference(referenceNumber, accessToken);
            if (saved != null) return saved;
        } catch (IOException e) {
            // The insert already succeeded. Do not retry it or queue a duplicate.
            Log.w(TAG, "Created report, but could not reload its priority", e);
        }
        return created.get(0);
    }

    /** Guest-owned report: RLS must enforce guest_auth_user_id = auth.uid(). */
    public static FaultReport submitGuestReport(String guestAuthId, long categoryId,
                                                String referenceNumber, String title,
                                                String description, double latitude,
                                                double longitude, String addressLine,
                                                String accessToken) throws IOException {
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            json.put("guest_auth_user_id", guestAuthId);
            json.put("category_id", categoryId);
            json.put("reference_number", referenceNumber);
            json.put("title", title);
            json.put("description", description == null ? "" : description);
            json.put("latitude", latitude);
            json.put("longitude", longitude);
            json.put("address_line", addressLine == null ? "" : addressLine);
            json.put("status", "Pending");
            json.put("is_offline", false);
            json.put("sync_status", "Synced");
            json.put("is_duplicate", false);
        } catch (org.json.JSONException e) {
            throw new IOException("Could not prepare guest report", e);
        }
        String response = SupabaseClient.postWithAuth("/fault_reports", json.toString(), accessToken);
        Type type = new TypeToken<List<FaultReport>>() {}.getType();
        List<FaultReport> created = gson.fromJson(response, type);
        if (created == null || created.isEmpty()) throw new IOException("Could not create guest report");
        try {
            FaultReport saved = getReportByReference(referenceNumber, accessToken);
            if (saved != null) return saved;
        } catch (IOException e) {
            Log.w(TAG, "Guest report created; priority reload failed", e);
        }
        return created.get(0);
    }

    public static boolean reportExistsByReference(String referenceNumber, String accessToken) throws IOException {
        String path = "/fault_reports?reference_number=eq." + referenceNumber
                + "&select=report_id&limit=1";
        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }
        return response != null
                && response.trim().startsWith("[")
                && !response.trim().equals("[]");
    }

    public static void insertPhoto(long reportId,
                                   String publicUrl,
                                   String storagePath,
                                   boolean isPrimary,
                                   String accessToken) throws IOException {
        String json = "{"
                + "\"report_id\":" + reportId + ","
                + "\"photo_url\":\"" + escape(publicUrl) + "\","
                + "\"supabase_storage_path\":\"" + escape(storagePath) + "\","
                + "\"is_primary\":" + isPrimary + ","
                + "\"is_offline\":false,"
                + "\"sync_status\":\"Synced\""
                + "}";
        SupabaseClient.postWithAuth("/report_photos", json, accessToken);
    }

    /** Avoid inserting a second photo row after a retry or an uncertain response. */
    public static boolean photoExists(long reportId, String storagePath,
                                      String accessToken) throws IOException {
        String path = "/report_photos?report_id=eq." + reportId
                + "&supabase_storage_path=eq."
                + java.net.URLEncoder.encode(storagePath, "UTF-8")
                + "&select=report_id&limit=1";
        String response = SupabaseClient.getWithAuth(path, accessToken);
        if (response == null || !response.trim().startsWith("[")) {
            throw new IOException("Could not check whether the photo already synced");
        }
        return !response.trim().equals("[]");
    }

    /** Photo rows for a report, with the primary photo first. */
    public static List<String> getPhotoUrls(long reportId, String accessToken) throws IOException {
        String response = SupabaseClient.getWithAuth("/report_photos?report_id=eq."
                        + reportId + "&select=photo_url,is_primary&order=is_primary.desc,supabase_storage_path.asc",
                accessToken);
        try {
            org.json.JSONArray rows = new org.json.JSONArray(response);
            List<String> urls = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                String url = rows.getJSONObject(i).optString("photo_url", "");
                if (!url.isEmpty()) urls.add(url);
            }
            return urls;
        } catch (org.json.JSONException e) {
            throw new IOException("Could not read report photos", e);
        }
    }

    public static void insertStatusHistory(long reportId,
                                           String oldStatus,
                                           String newStatus,
                                           String notes,
                                           String accessToken) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"report_id\":").append(reportId).append(",");
        if (oldStatus == null) {
            json.append("\"old_status\":null,");
        } else {
            json.append("\"old_status\":\"").append(escape(oldStatus)).append("\",");
        }
        json.append("\"new_status\":\"").append(escape(newStatus)).append("\",");
        if (notes == null) {
            json.append("\"notes\":null");
        } else {
            json.append("\"notes\":\"").append(escape(notes)).append("\"");
        }
        json.append("}");
        SupabaseClient.postWithAuth("/fault_status_history", json.toString(), accessToken);
    }

    public static List<FaultReport> getUserReports(long citizenId, String accessToken) throws IOException {
        String path = "/fault_reports?citizen_id=eq." + citizenId + "&order=created_at.desc";
        String response = SupabaseClient.getWithAuth(path, accessToken);
        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<FaultReport>>(){}.getType();
        List<FaultReport> results = gson.fromJson(response, listType);
        return results != null ? results : new ArrayList<>();
    }

    public static List<FaultReport> getGuestReports(String guestAuthId, String accessToken)
            throws IOException {
        String response = SupabaseClient.getWithAuth("/fault_reports?guest_auth_user_id=eq."
                + guestAuthId + "&order=created_at.desc", accessToken);
        Type type = new TypeToken<List<FaultReport>>() {}.getType();
        List<FaultReport> reports = gson.fromJson(response, type);
        return reports != null ? reports : new ArrayList<>();
    }

    /** Exposes only the limited fields returned by the database's guest tracking RPC. */
    public static FaultReport trackExternalAsGuest(String reference, String accessToken)
            throws IOException {
        org.json.JSONObject request = new org.json.JSONObject();
        try { request.put("p_reference", reference); }
        catch (org.json.JSONException e) { throw new IOException("Invalid reference", e); }
        String response = SupabaseClient.postWithAuth("/rpc/guest_track_report",
                request.toString(), accessToken);
        Type type = new TypeToken<List<FaultReport>>() {}.getType();
        List<FaultReport> matches = gson.fromJson(response, type);
        return matches == null || matches.isEmpty() ? null : matches.get(0);
    }

    public static FaultReport getReportByReference(String referenceNumber, String accessToken) throws IOException {
        String path = "/fault_reports?reference_number=eq." + referenceNumber + "&limit=1";
        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }
        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            return null;
        }
        Type listType = new TypeToken<List<FaultReport>>(){}.getType();
        List<FaultReport> results = gson.fromJson(response, listType);
        if (results == null || results.isEmpty()) return null;
        return results.get(0);
    }

    @Deprecated
    public static FaultReport getReportByReference(String referenceNumber) throws IOException {
        return getReportByReference(referenceNumber, null);
    }

    public static List<StatusHistoryDTO> getStatusHistory(long reportId, String accessToken) throws IOException {
        String path = "/fault_status_history?report_id=eq." + reportId
                + "&order=changed_at.desc";
        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }
        if (response == null || !response.trim().startsWith("[")) {
            throw new IOException("Could not load status history");
        }
        Type listType = new TypeToken<List<StatusHistoryDTO>>(){}.getType();
        List<StatusHistoryDTO> results = gson.fromJson(response, listType);
        if (results == null) throw new IOException("Invalid status history response");
        return results;
    }

    // ==================================================================
    // NEW: Category-filtered nearby reports
    // ==================================================================
    /**
     * Fetches open reports of a specific category within radiusMeters of
     * the given location. Returns a NearbyReport list sorted nearest-first.
     */
    public static List<NearbyReport> getNearbyReportsForCategory(
            long categoryId,
            long currentCitizenId,
            double userLat,
            double userLng,
            double radiusMeters,
            String accessToken
    ) throws IOException {

        if (currentCitizenId <= 0) {
            List<NearbyReport> out = new ArrayList<>();
            for (FaultReport r : getNearbyReports(userLat, userLng, accessToken)) {
                if (r.category_id != categoryId) continue;
                double d = haversineMeters(userLat, userLng, r.getLatitude(), r.getLongitude());
                if (d > radiusMeters) continue;
                String title = r.getTitle() != null ? r.getTitle() : "Report";
                NearbyReport nearby = new NearbyReport(r.getReportId(),
                        r.getReferenceNumber(), title, title, r.getStatus(),
                        r.getPriorityScore() != null ? r.getPriorityScore() : 0,
                        r.getVoteCount() != null ? r.getVoteCount() : 0, d);
                nearby.setOwnerCitizenId(r.getCitizenId() != null ? r.getCitizenId() : -1L);
                out.add(nearby);
            }
            out.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
            return out;
        }

        String path = "/fault_reports"
                + "?category_id=eq." + categoryId
                + "&status=in.(Pending,%22Under%20Review%22)"
                + "&order=created_at.desc";

        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }

        if (response == null || !response.trim().startsWith("[")) {
            throw new IOException("Could not load nearby reports for this category");
        }

        Type listType = new TypeToken<List<FaultReport>>(){}.getType();
        List<FaultReport> reports = gson.fromJson(response, listType);
        if (reports == null) return new ArrayList<>();

        List<NearbyReport> out = new ArrayList<>();
        for (FaultReport r : reports) {

            // NOTE: own reports are no longer filtered out — they're shown
            // so the user can see them, but the app blocks upvoting them.

            double d = haversineMeters(userLat, userLng, r.getLatitude(), r.getLongitude());
            if (d <= radiusMeters) {
                String ref   = r.getReferenceNumber() != null ? r.getReferenceNumber() : "";
                String title = r.getTitle() != null ? r.getTitle() : "Report";
                String stat  = r.getStatus() != null ? r.getStatus() : "Pending";
                int score    = r.getPriorityScore() != null ? r.getPriorityScore() : 0;
                int votes    = r.getVoteCount() != null ? r.getVoteCount() : 0;
                Long ownerId = r.getCitizenId();

                NearbyReport nr = new NearbyReport(
                        r.getReportId(), ref, title, title, stat, score, votes, d);
                nr.setOwnerCitizenId(ownerId != null ? ownerId : -1L);
                out.add(nr);
            }
        }
        out.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
        return out;
    }

    // ==================================================================
    // NEW: Upvote
    // ==================================================================
    /**
     * Inserts a row into report_votes. The bump_vote_count trigger on
     * the DB side increments fault_reports.vote_count automatically.
     * Let HTTP errors reach the caller so it can distinguish an existing vote
     * from a connection or permissions problem.
     */
    public static void upvoteReport(long reportId,
                                    long citizenProfileId,
                                    String accessToken) throws IOException {
        String json = "{"
                + "\"report_id\":" + reportId + ","
                + "\"citizen_id\":" + citizenProfileId
                + "}";
        SupabaseClient.postWithAuth("/report_votes", json, accessToken);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static class FaultCategoryDTO {
        public long category_id;
        public String name;
        public String description;
        public double severity_weight;
        public int estimated_fix_time;
        public String icon;
        public boolean is_active;
    }

    public static class CategoryIdDTO {
        public long category_id;
    }

    public static class StatusHistoryDTO {
        public long history_id;
        public long report_id;
        public String old_status;
        public String new_status;
        public String changed_at;
        public String notes;
    }
}
