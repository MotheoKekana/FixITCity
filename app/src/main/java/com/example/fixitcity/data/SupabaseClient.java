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

    // Bucket name (already created by Person 1)
    public static final String REPORT_PHOTOS_BUCKET = "reports";

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

    // ============ PATCH ============
    public static String patch(String path, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = baseRequest(REST_URL + path)
                .patch(body)
                .addHeader("Prefer", "return=representation")
                .build();
        try (Response response = getHttpClient().newCall(request).execute()) {
            if (response.body() == null) return "";
            return response.body().string();
        }
    }

    // ============ DELETE ============
    public static String delete(String path) throws IOException {
        Request request = baseRequest(REST_URL + path).delete().build();
        try (Response response = getHttpClient().newCall(request).execute()) {
            if (response.body() == null) return "";
            return response.body().string();
        }
    }

    // ============ USER-AUTHENTICATED GET ============
    public static String getWithAuth(String path, String accessToken) throws IOException {
        Request request = userRequest(REST_URL + path, accessToken).get().build();
        try (Response response = getHttpClient().newCall(request).execute()) {
            if (response.body() == null) return "";
            return response.body().string();
        }
    }

    // ============ USER-AUTHENTICATED POST ============
    public static String postWithAuth(String path, String jsonBody, String accessToken) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = userRequest(REST_URL + path, accessToken)
                .post(body)
                .addHeader("Prefer", "return=representation")
                .build();
        try (Response response = getHttpClient().newCall(request).execute()) {
            if (response.body() == null) return "";
            return response.body().string();
        }
    }

    // ============ STORAGE UPLOAD ============
    /**
     * Uploads raw bytes to a storage bucket and returns the public URL.
     *
     * @param bucketName e.g. "reports"
     * @param storagePath e.g. "user42/photo_123.jpg"
     * @param imageBytes the actual image data
     * @param mimeType e.g. "image/jpeg"
     * @param accessToken the logged-in user's token
     * @return public URL of the uploaded file
     */
    public static String uploadToStorage(String bucketName,
                                         String storagePath,
                                         byte[] imageBytes,
                                         String mimeType,
                                         String accessToken) throws IOException {

        // Build the upload URL: {STORAGE_URL}/object/{bucket}/{path}
        String url = STORAGE_URL + "/object/" + bucketName + "/" + storagePath;

        MediaType mediaType = MediaType.get(mimeType != null ? mimeType : "image/jpeg");
        RequestBody body = RequestBody.create(imageBytes, mediaType);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", mediaType.toString())
                .addHeader("x-upsert", "true")
                .post(body)
                .build();

        try (Response response = getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "unknown";
                throw new IOException("Storage upload failed: " + response.code() + " " + err);
            }
        }

        // Build the public URL: {SUPABASE_URL}/storage/v1/object/public/{bucket}/{path}
        return SUPABASE_URL + "/storage/v1/object/public/" + bucketName + "/" + storagePath;
    }
}