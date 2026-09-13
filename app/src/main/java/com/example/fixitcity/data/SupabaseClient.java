package com.example.fixitcity.data;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class SupabaseClient {

    // ===== YOUR SUPABASE CREDENTIALS =====
    public static final String SUPABASE_URL = "https://ubokqcaxsqsxyuwffagh.supabase.co";
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVib2txY2F4c3FzeHl1d2ZmYWdoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg0NDgzMjgsImV4cCI6MjEwNDAyNDMyOH0.8uZe5372MIFel18D4eorYIdt60wIckDl0zKJowI1ocg";

    // REST endpoints
    public static final String REST_URL = SUPABASE_URL + "/rest/v1";
    public static final String AUTH_URL = SUPABASE_URL + "/auth/v1";
    public static final String STORAGE_URL = SUPABASE_URL + "/storage/v1";

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static OkHttpClient httpClient;

    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            httpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return httpClient;
    }

    // Base request with anon key (no user logged in)
    public static Request.Builder baseRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json");
    }

    // Request with logged-in user's access token
    public static Request.Builder userRequest(String url, String accessToken) {
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json");
    }

    // ============ GET ============
    public static String get(String path) throws IOException {
        Request request = baseRequest(REST_URL + path).get().build();
        try (Response response = getHttpClient().newCall(request).execute()) {
            if (response.body() == null) return "";
            return response.body().string();
        }
    }

    // ============ POST ============
    public static String post(String path, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = baseRequest(REST_URL + path)
                .post(body)
                .addHeader("Prefer", "return=representation")
                .build();
        try (Response response = getHttpClient().newCall(request).execute()) {
            if (response.body() == null) return "";
            return response.body().string();
        }
    }
}
