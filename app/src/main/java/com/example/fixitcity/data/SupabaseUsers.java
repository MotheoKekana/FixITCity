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
     * Finds the public.users row belonging to the authenticated
     * Supabase Auth account.
     */
    public static long findUserIdByAuthUuid(
            String authUuid,
            String accessToken
    ) throws IOException {

        String path = "/users"
                + "?supabase_user_id=eq." + authUuid
                + "&select=user_id,email,supabase_user_id,role"
                + "&limit=1";

        String response = SupabaseClient.getWithAuth(
                path,
                accessToken
        );

        Log.d(TAG, "findUserIdByAuthUuid response: " + response);

        if (response == null
                || response.trim().isEmpty()
                || !response.trim().startsWith("[")) {

            throw new IOException(
                    "Could not load the user account: " + response
            );
        }

        Type listType =
                new TypeToken<List<UserDTO>>() {}.getType();

        List<UserDTO> users = gson.fromJson(
                response,
                listType
        );

        if (users == null || users.isEmpty()) {
            return -1;
        }

        return users.get(0).user_id;
    }

    /**
     * Loads the citizen profile belonging to a public.users row.
     */
    public static CitizenProfileDTO findCitizenProfileByUserId(
            long userId,
            String accessToken
    ) throws IOException {

        String path = "/citizen_profiles"
                + "?user_id=eq." + userId
                + "&select=citizen_profile_id,user_id,"
                + "first_name,last_name,phone_number"
                + "&limit=1";

        String response = SupabaseClient.getWithAuth(
                path,
                accessToken
        );

        Log.d(TAG,
                "findCitizenProfileByUserId response: "
                        + response
        );

        if (response == null
                || response.trim().isEmpty()
                || !response.trim().startsWith("[")) {

            throw new IOException(
                    "Could not load the citizen profile: "
                            + response
            );
        }

        Type listType =
                new TypeToken<List<CitizenProfileDTO>>() {}.getType();

        List<CitizenProfileDTO> profiles = gson.fromJson(
                response,
                listType
        );

        if (profiles == null || profiles.isEmpty()) {
            return null;
        }

        return profiles.get(0);
    }

    /**
     * Kept for compatibility with existing code.
     */
    public static long findCitizenProfileIdByUserId(
            long userId,
            String accessToken
    ) throws IOException {

        CitizenProfileDTO profile =
                findCitizenProfileByUserId(
                        userId,
                        accessToken
                );

        if (profile == null) {
            return -1;
        }

        return profile.citizen_profile_id;
    }

    /**
     * Saves or refreshes an FCM device token.
     *
     * The required RLS policies will be added during the RLS phase.
     */
    public static void saveDeviceToken(
            long userId,
            String token,
            String deviceName,
            String accessToken
    ) throws IOException {

        String lookupPath = "/device_tokens"
                + "?user_id=eq." + userId
                + "&device_token=eq." + escape(token)
                + "&select=token_id"
                + "&limit=1";

        String lookupResponse =
                SupabaseClient.getWithAuth(
                        lookupPath,
                        accessToken
                );

        Log.d(TAG,
                "saveDeviceToken lookup: "
                        + lookupResponse
        );

        boolean exists =
                lookupResponse != null
                        && lookupResponse.trim().startsWith("[")
                        && !lookupResponse.trim().equals("[]");

        if (exists) {
            String patchBody = "{"
                    + "\"is_active\":true,"
                    + "\"last_used_at\":\""
                    + nowIso()
                    + "\""
                    + "}";

            String patchPath = "/device_tokens"
                    + "?user_id=eq." + userId
                    + "&device_token=eq."
                    + escape(token);

            SupabaseClient.patchWithAuth(
                    patchPath,
                    patchBody,
                    accessToken
            );

            Log.d(TAG, "Device token refreshed");

        } else {
            String json = "{"
                    + "\"user_id\":" + userId + ","
                    + "\"device_token\":\""
                    + escape(token) + "\","
                    + "\"device_name\":\""
                    + escape(deviceName) + "\","
                    + "\"is_active\":true"
                    + "}";

            String response =
                    SupabaseClient.postWithAuth(
                            "/device_tokens",
                            json,
                            accessToken
                    );

            Log.d(TAG,
                    "Device token inserted: " + response
            );
        }
    }

    public static void deactivateDeviceToken(
            long userId,
            String token,
            String accessToken
    ) throws IOException {

        String patchBody = "{\"is_active\":false}";

        String patchPath = "/device_tokens"
                + "?user_id=eq." + userId
                + "&device_token=eq."
                + escape(token);

        SupabaseClient.patchWithAuth(
                patchPath,
                patchBody,
                accessToken
        );

        Log.d(TAG, "Device token deactivated");
    }

    private static String nowIso() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        java.util.Locale.US
                );

        sdf.setTimeZone(
                java.util.TimeZone.getTimeZone("UTC")
        );

        return sdf.format(new java.util.Date());
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

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