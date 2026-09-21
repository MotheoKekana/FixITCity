package com.example.fixitcity.data;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class SupabaseUsers {

    private static final String TAG = "SupabaseUsers";
    private static final Gson gson = new Gson();

    /**
     * Insert a row into public.users with the Supabase auth UUID and email.
     * Returns the newly created user_id (bigint).
     */
    public static long createUserRecord(String authUuid, String email, String accessToken) throws IOException {
        String json = "{"
                + "\"email\":\"" + escape(email) + "\","
                + "\"supabase_user_id\":\"" + escape(authUuid) + "\","
                + "\"role\":\"Citizen\","
                + "\"is_active\":true"
                + "}";

        Log.d(TAG, "createUserRecord payload: " + json);

        String response = SupabaseClient.postWithAuth("/users", json, accessToken);
        Log.d(TAG, "createUserRecord response: " + response);

        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            throw new IOException("Failed to create user record: " + response);
        }

        Type listType = new TypeToken<List<UserDTO>>(){}.getType();
        List<UserDTO> created = gson.fromJson(response, listType);

        if (created == null || created.isEmpty()) {
            throw new IOException("Empty response when creating user record");
        }
        return created.get(0).user_id;
    }

    /**
     * Insert a row into public.citizen_profiles.
     * Returns the newly created citizen_profile_id (bigint).
     */
    public static long createCitizenProfile(long userId,
                                            String firstName,
                                            String lastName,
                                            String phone,
                                            String accessToken) throws IOException {
        String json = "{"
                + "\"user_id\":" + userId + ","
                + "\"first_name\":\"" + escape(firstName) + "\","
                + "\"last_name\":\"" + escape(lastName) + "\","
                + "\"phone_number\":\"" + escape(phone) + "\""
                + "}";

        Log.d(TAG, "createCitizenProfile payload: " + json);

        String response = SupabaseClient.postWithAuth("/citizen_profiles", json, accessToken);
        Log.d(TAG, "createCitizenProfile response: " + response);

        if (response == null || response.trim().isEmpty() || !response.trim().startsWith("[")) {
            throw new IOException("Failed to create citizen profile: " + response);
        }

        Type listType = new TypeToken<List<CitizenProfileDTO>>(){}.getType();
        List<CitizenProfileDTO> created = gson.fromJson(response, listType);

        if (created == null || created.isEmpty()) {
            throw new IOException("Empty response when creating citizen profile");
        }
        return created.get(0).citizen_profile_id;
    }

    /**
     * Look up an existing user by Supabase auth UUID.
     * Returns user_id, or -1 if not found.
     */
    public static long findUserIdByAuthUuid(String authUuid) throws IOException {
        String path = "/users?supabase_user_id=eq." + authUuid + "&select=user_id&limit=1";
        String response = SupabaseClient.get(path);
        Log.d(TAG, "findUserIdByAuthUuid[" + authUuid + "] response: " + response);

        if (response == null || !response.trim().startsWith("[")) {
            Log.w(TAG, "findUserIdByAuthUuid: non-array response");
            return -1;
        }

        Type listType = new TypeToken<List<UserDTO>>(){}.getType();
        List<UserDTO> results = gson.fromJson(response, listType);
        if (results == null || results.isEmpty()) {
            Log.w(TAG, "findUserIdByAuthUuid: no user found for uuid " + authUuid);
            return -1;
        }
        Log.d(TAG, "findUserIdByAuthUuid: found user_id = " + results.get(0).user_id);
        return results.get(0).user_id;
    }

    /**
     * Look up a citizen_profile_id by user_id (bigint).
     * Returns citizen_profile_id, or -1 if not found.
     * Tries both authenticated and anonymous reads.
     */
    public static long findCitizenProfileIdByUserId(long userId, String accessToken) throws IOException {
        String path = "/citizen_profiles?user_id=eq." + userId + "&select=citizen_profile_id&limit=1";

        // 1) Try authenticated read first
        String response;
        try {
            response = SupabaseClient.getWithAuth(path, accessToken);
            Log.d(TAG, "findCitizenProfileIdByUserId[authenticated] response: " + response);
        } catch (Exception e) {
            Log.e(TAG, "Authenticated read failed: " + e.getMessage(), e);
            response = null;
        }

        // 2) Fallback to anonymous read
        if (response == null || !response.trim().startsWith("[") || response.trim().equals("[]")) {
            Log.d(TAG, "Falling back to anonymous read for citizen_profiles");
            response = SupabaseClient.get(path);
            Log.d(TAG, "findCitizenProfileIdByUserId[anon] response: " + response);
        }

        if (response == null || !response.trim().startsWith("[")) {
            Log.w(TAG, "findCitizenProfileIdByUserId: non-array response");
            return -1;
        }

        Type listType = new TypeToken<List<CitizenProfileDTO>>(){}.getType();
        List<CitizenProfileDTO> results = gson.fromJson(response, listType);
        if (results == null || results.isEmpty()) {
            Log.w(TAG, "findCitizenProfileIdByUserId: no profile found for user_id " + userId);
            return -1;
        }
        Log.d(TAG, "findCitizenProfileIdByUserId: found citizen_profile_id = " + results.get(0).citizen_profile_id);
        return results.get(0).citizen_profile_id;
    }

    /**
     * Overload for backward compatibility — uses anon read only.
     */
    public static long findCitizenProfileIdByUserId(long userId) throws IOException {
        return findCitizenProfileIdByUserId(userId, null);
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
    public static class UserDTO {
        public long user_id;
        public String email;
        public String supabase_user_id;
        public String role;
    }

    public static class CitizenProfileDTO {
        public long citizen_profile_id;
        public long user_id;
        public String first_name;
        public String last_name;
        public String phone_number;
    }
}