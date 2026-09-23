package com.example.fixitcity.data;

import android.util.Log;

import com.example.fixitcity.FaultCategory;
import com.example.fixitcity.FaultReport;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SupabaseReports {

    private static final String TAG = "SupabaseReports";
    private static final Gson gson = new Gson();

    // ==========================================
    // Compute a client-side priority score (0-100).
    // ==========================================
    public static int computePriorityScore(double severityWeight) {
        int score = (int) Math.round(severityWeight * 10.0);
        if (score < 0) score = 0;
        if (score > 100) score = 100;
        return score;
    }

    // ==========================================
    // Haversine distance between two lat/lng in meters.
    // ==========================================
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ==========================================
    // Fetch all "open" reports (Pending / Assigned).
    // ==========================================
    public static List<FaultReport> getOpenReports(String accessToken) throws IOException {
        String path = "/fault_reports?status=in.(Pending,Assigned)"
                + "&order=created_at.desc";

        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }
        Log.d(TAG, "getOpenReports response: " + response);

        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            Log.w(TAG, "getOpenReports: non-array response");
            return new ArrayList<>();
        }

        Type listType = new TypeToken<List<FaultReport>>(){}.getType();
        List<FaultReport> results = gson.fromJson(response, listType);
        return results != null ? results : new ArrayList<>();
    }

    // ==========================================
    // Fetch all fault categories
    // Uses the user's access token when available so RLS-protected
    // reads succeed. Falls back to the anon key for pre-login screens.
    // ==========================================
    public static List<FaultCategory> getCategories(String accessToken) throws IOException {
        String path = "/fault_categories?select=*&order=name.asc";

        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }
        Log.d(TAG, "Categories raw: " + response);

        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            Log.e(TAG, "Invalid response: " + response);
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
                            android.R.drawable.ic_menu_report_image
                    ));
                }
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // Backwards-compatible overload for any old callers.
    @Deprecated
    public static List<FaultCategory> getCategories() throws IOException {
        return getCategories(null);
    }

    // ==========================================
    // Look up category_id by name
    // ==========================================
    public static long getCategoryIdByName(String name) throws IOException {
        String path = "/fault_categories?name=eq." + name + "&select=category_id&limit=1";
        String response = SupabaseClient.get(path);
        Log.d(TAG, "getCategoryIdByName response: " + response);

        if (response == null || !response.trim().startsWith("[")) return -1;

        Type listType = new TypeToken<List<CategoryIdDTO>>(){}.getType();
        List<CategoryIdDTO> results = gson.fromJson(response, listType);
        if (results == null || results.isEmpty()) return -1;
        return results.get(0).category_id;
    }

    // ==========================================
    // Submit a new fault report
    // ==========================================
    public static FaultReport submitReport(
            long citizenId,
            long categoryId,
            double severityWeight,
            String title,
            String description,
            double latitude,
            double longitude,
            String addressLine,
            String accessToken
    ) throws IOException {

        String year = String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
        String random = String.format("%04d", (int)(Math.random() * 10000));
        String referenceNumber = "FIX-" + year + "-" + random;

        int priorityScore = computePriorityScore(severityWeight);

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
        json.append("\"priority_score\":").append(priorityScore).append(",");
        json.append("\"is_offline\":false,");
        json.append("\"sync_status\":\"Synced\",");
        json.append("\"is_duplicate\":false");
        json.append("}");

        Log.d(TAG, "Submitting: " + json.toString());

        String response = SupabaseClient.postWithAuth("/fault_reports", json.toString(), accessToken);
        Log.d(TAG, "Submit response: " + response);

        Type listType = new TypeToken<List<FaultReport>>(){}.getType();
        List<FaultReport> created = gson.fromJson(response, listType);

        if (created == null || created.isEmpty()) {
            throw new IOException("Failed to create report");
        }
        return created.get(0);
    }

    // ==========================================
    // Insert a photo record
    // ==========================================
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

        Log.d(TAG, "Inserting photo: " + json);
        String response = SupabaseClient.postWithAuth("/report_photos", json, accessToken);
        Log.d(TAG, "Photo insert response: " + response);
    }

    // ==========================================
    // Get current user's reports
    // ==========================================
    public static List<FaultReport> getUserReports(long citizenId, String accessToken) throws IOException {
        String path = "/fault_reports?citizen_id=eq." + citizenId + "&order=created_at.desc";
        String response = SupabaseClient.getWithAuth(path, accessToken);
        Log.d(TAG, "getUserReports response: " + response);

        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            Log.w(TAG, "getUserReports: non-array response");
            return new ArrayList<>();
        }

        Type listType = new TypeToken<List<FaultReport>>(){}.getType();
        List<FaultReport> results = gson.fromJson(response, listType);
        return results != null ? results : new ArrayList<>();
    }

    // ==========================================
    // Get a report by reference number
    // Uses the user's token so RLS-protected reads succeed.
    // ==========================================
    public static FaultReport getReportByReference(String referenceNumber, String accessToken) throws IOException {
        String path = "/fault_reports?reference_number=eq." + referenceNumber + "&limit=1";

        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }

        Log.d(TAG, "getReportByReference[" + referenceNumber + "] response: " + response);

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

    // ==========================================
    // Get status history for a report (Phase 6.3)
    // Returns rows ordered newest-first.
    // ==========================================
    public static List<StatusHistoryDTO> getStatusHistory(long reportId, String accessToken) throws IOException {
        String path = "/fault_status_history?report_id=eq." + reportId
                + "&order=changed_at.desc";

        String response;
        if (accessToken != null && !accessToken.isEmpty()) {
            response = SupabaseClient.getWithAuth(path, accessToken);
        } else {
            response = SupabaseClient.get(path);
        }

        Log.d(TAG, "getStatusHistory[" + reportId + "] response: " + response);

        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            Log.w(TAG, "getStatusHistory: non-array response");
            return new ArrayList<>();
        }

        Type listType = new TypeToken<List<StatusHistoryDTO>>(){}.getType();
        List<StatusHistoryDTO> results = gson.fromJson(response, listType);
        return results != null ? results : new ArrayList<>();
    }

    // ==========================================
    // Helper: Escape JSON strings
    // ==========================================
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // ==========================================
    // DTOs
    // ==========================================
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