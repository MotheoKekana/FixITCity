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
     * Refresh an expired access token using the stored refresh_token.
     * Returns the full JSON response on success, or null on failure.
     */
    public static String refreshToken(String refreshToken) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("refresh_token", refreshToken);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                SupabaseClient.JSON
        );

        Request request = new Request.Builder()
                .url(SupabaseClient.AUTH_URL + "/token?grant_type=refresh_token")
                .addHeader("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        OkHttpClient client = SupabaseClient.getHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) return null;
            String responseStr = response.body().string();

            if (!response.isSuccessful()) {
                throw new IOException("Token refresh failed: " + responseStr);
            }
            return responseStr;
        }
    }

    public static void logout(String accessToken) throws IOException {
        Request request = new Request.Builder()
                .url(SupabaseClient.AUTH_URL + "/logout")
                .addHeader("apikey", SupabaseClient.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create("", SupabaseClient.JSON))
                .build();

        OkHttpClient client = SupabaseClient.getHttpClient();
        try (Response response = client.newCall(request).execute()) {
            // Ignore
        }
    }
}