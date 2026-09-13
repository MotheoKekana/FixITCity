package com.example.fixitcity.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseAuth {

    /**
     * Register a new user with Supabase Auth.
     * Returns the JSON response as a String, or null if failed.
     */
    public static String register(String email, String password) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                SupabaseClient.JSON
        );

        Request request = new Request.Builder()
                .url(SupabaseClient.AUTH_URL + "/signup")
                .addHeader("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        OkHttpClient client = SupabaseClient.getHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) return null;
            String responseStr = response.body().string();

            if (!response.isSuccessful()) {
                throw new IOException("Registration failed: " + responseStr);
            }
            return responseStr;
        }
    }

    /**
     * Login with email and password.
     * Returns JSON containing access_token, user info, etc.
     */
    public static String login(String email, String password) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                SupabaseClient.JSON
        );

        Request request = new Request.Builder()
                .url(SupabaseClient.AUTH_URL + "/token?grant_type=password")
                .addHeader("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        OkHttpClient client = SupabaseClient.getHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) return null;
            String responseStr = response.body().string();

            if (!response.isSuccessful()) {
                throw new IOException("Login failed: " + responseStr);
            }
            return responseStr;
        }
    }

    /**
     * Logout (revoke token).
     */
    public static void logout(String accessToken) throws IOException {
        Request request = new Request.Builder()
                .url(SupabaseClient.AUTH_URL + "/logout")
                .addHeader("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create("", SupabaseClient.JSON))
                .build();

        OkHttpClient client = SupabaseClient.getHttpClient();
        try (Response response = client.newCall(request).execute()) {
            // Ignore response — token is invalidated server-side
        }
    }
}
