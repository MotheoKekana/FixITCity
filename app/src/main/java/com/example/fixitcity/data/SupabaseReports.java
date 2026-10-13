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
import java.util.UUID;

public class SupabaseReports {

    private static final String TAG = "SupabaseReports";
    private static final Gson gson = new Gson();

    //Builds tracking number in format: FIX-YYYY-XXXXXXXX.
    //The number is generated before submission so that it is reused on retry.
    public static String generateReferenceNumber() //returns reference number as string
    {
        String year = String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)); //Get current year
        String suffix = UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase(); //Takes 8 characters of random UUID
        return "FIX-" + year + "-" + suffix; //Return formatted ref number in string format
    }

    //Returns distance in metres between two GPS points
    //Using this instead of normal the normal euclidean formula as we need to account for the earth's curve
    public static double haversineMeters(double latitude1, double longitude1, double latitude2, double longitude2)
    {
        final double rad = 6371000.0; //Earth's mean radius (metres)
        double latRad1 = Math.toRadians(latitude1);
        double latRad2 = Math.toRadians(latitude2);

        double latDiffRad = Math.toRadians(latitude2 - latitude1); //Difference in latitude in radians
        double longDiffRad = Math.toRadians(longitude2 - longitude1); //Difference in longitude in radians

        double sinHalfLat = Math.sin(latDiffRad/2);
        double sinHalfLon = Math.sin(longDiffRad/2);

        double intermediateVal = sinHalfLat * sinHalfLat
                + Math.cos(latRad1) * Math.cos(latRad2) * sinHalfLon * sinHalfLon;

        double centralAngle = 2 * Math.atan2(Math.sqrt(intermediateVal), Math.sqrt(1-intermediateVal)); //Central angle between the two points
        double distance = rad * centralAngle; //Arc length along the Earth's surface in metres.

        return distance;

    }

    /** Open reports within 2 km, including reports owned by other citizens. */
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
        Log.d(TAG, "Categories raw: " + response);

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

    /**
     * Submit a report with a caller-supplied reference number.
     * The reference MUST come from generateReferenceNumber() so that
     * offline-queued reports keep the same reference after sync.
     */
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

        Log.d(TAG, "Submitting: " + json.toString());

        String response = SupabaseClient.postWithAuth("/fault_reports", json.toString(), accessToken);
        Log.d(TAG, "Submit response: " + response);

        Type listType = new TypeToken<List<FaultReport>>(){}.getType();
        List<FaultReport> created = gson.fromJson(response, listType);

        if (created == null || created.isEmpty()) {
            throw new IOException("Failed to create report");
        }

        // An AFTER INSERT database trigger calculates priority. INSERT's returned
        // representation may still contain the pre-trigger value, so read the
        // saved row once to display the authoritative score.
        try {
            FaultReport saved = getReportByReference(referenceNumber, accessToken);
            if (saved != null) return saved;
        } catch (IOException e) {
            // The insert succeeded. Do not treat a failed follow-up read as a
            // failed submission or queue a second copy of the same report.
            Log.w(TAG, "Created report, but could not reload its priority", e);
        }
        return created.get(0);
    }

    /**
     * Idempotency helper — returns true if a report with this reference
     * already exists on the server. Used by SyncWorker before retrying.
     */
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

        Log.d(TAG, "Inserting photo: " + json);
        String response = SupabaseClient.postWithAuth("/report_photos", json, accessToken);
        Log.d(TAG, "Photo insert response: " + response);
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

        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            return new ArrayList<>();
        }

        Type listType = new TypeToken<List<StatusHistoryDTO>>(){}.getType();
        List<StatusHistoryDTO> results = gson.fromJson(response, listType);
        return results != null ? results : new ArrayList<>();
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
