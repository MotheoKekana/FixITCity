package com.example.fixitcity.data;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {

    private static final String TAG = "SupabaseClient";

    // Keep your existing Supabase project credentials here.
    public static final String SUPABASE_URL =
            "https://ubokqcaxsqsxyuwffagh.supabase.co";

    public static final String SUPABASE_ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                    + "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6"
                    + "InVib2txY2F4c3FzeHl1d2ZmYWdoIiwicm9sZSI6"
                    + "ImFub24iLCJpYXQiOjE3ODg0NDgzMjgsImV4cCI6"
                    + "MjEwNDAyNDMyOH0."
                    + "8uZe5372MIFel18D4eorYIdt60wIckDl0zKJowI1ocg";

    public static final String REST_URL =
            SUPABASE_URL + "/rest/v1";

    public static final String AUTH_URL =
            SUPABASE_URL + "/auth/v1";

    public static final String STORAGE_URL =
            SUPABASE_URL + "/storage/v1";

    public static final String REPORT_PHOTOS_BUCKET =
            "reports";

    public static final MediaType JSON =
            MediaType.get(
                    "application/json; charset=utf-8"
            );

    private static OkHttpClient httpClient;

    private SupabaseClient() {
        // Utility class
    }

    public static synchronized OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient =
                    new OkHttpClient.Builder()
                            .connectTimeout(
                                    30,
                                    TimeUnit.SECONDS
                            )
                            .readTimeout(
                                    30,
                                    TimeUnit.SECONDS
                            )
                            .writeTimeout(
                                    30,
                                    TimeUnit.SECONDS
                            )
                            .build();
        }

        return httpClient;
    }

    /**
     * Creates a request using the anonymous Supabase key.
     */
    public static Request.Builder baseRequest(
            String url
    ) {
        return new Request.Builder()
                .url(url)
                .addHeader(
                        "apikey",
                        SUPABASE_ANON_KEY
                )
                .addHeader(
                        "Authorization",
                        "Bearer " + SUPABASE_ANON_KEY
                )
                .addHeader(
                        "Content-Type",
                        "application/json"
                );
    }

    /**
     * Creates a request using the logged-in user's
     * access token.
     */
    public static Request.Builder userRequest(
            String url,
            String accessToken
    ) throws IOException {

        if (accessToken == null
                || accessToken.trim().isEmpty()) {

            throw new IOException(
                    "No authenticated access token is available"
            );
        }

        return new Request.Builder()
                .url(url)
                .addHeader(
                        "apikey",
                        SUPABASE_ANON_KEY
                )
                .addHeader(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .addHeader(
                        "Content-Type",
                        "application/json"
                );
    }

    // =================================================
    // Anonymous requests
    // =================================================

    public static String get(
            String path
    ) throws IOException {

        Request request = baseRequest(
                REST_URL + path
        )
                .get()
                .build();

        return executeRequest(
                request,
                "Anonymous GET " + path
        );
    }

    public static String post(
            String path,
            String jsonBody
    ) throws IOException {

        RequestBody body =
                RequestBody.create(
                        jsonBody,
                        JSON
                );

        Request request = baseRequest(
                REST_URL + path
        )
                .post(body)
                .addHeader(
                        "Prefer",
                        "return=representation"
                )
                .build();

        return executeRequest(
                request,
                "Anonymous POST " + path
        );
    }

    public static String patch(
            String path,
            String jsonBody
    ) throws IOException {

        RequestBody body =
                RequestBody.create(
                        jsonBody,
                        JSON
                );

        Request request = baseRequest(
                REST_URL + path
        )
                .patch(body)
                .addHeader(
                        "Prefer",
                        "return=representation"
                )
                .build();

        return executeRequest(
                request,
                "Anonymous PATCH " + path
        );
    }

    public static String delete(
            String path
    ) throws IOException {

        Request request = baseRequest(
                REST_URL + path
        )
                .delete()
                .build();

        return executeRequest(
                request,
                "Anonymous DELETE " + path
        );
    }

    // =================================================
    // Authenticated requests
    // =================================================

    public static String getWithAuth(
            String path,
            String accessToken
    ) throws IOException {

        Request request = userRequest(
                REST_URL + path,
                accessToken
        )
                .get()
                .build();

        return executeRequest(
                request,
                "Authenticated GET " + path
        );
    }

    public static String postWithAuth(
            String path,
            String jsonBody,
            String accessToken
    ) throws IOException {

        RequestBody body =
                RequestBody.create(
                        jsonBody,
                        JSON
                );

        Request request = userRequest(
                REST_URL + path,
                accessToken
        )
                .post(body)
                .addHeader(
                        "Prefer",
                        "return=representation"
                )
                .build();

        return executeRequest(
                request,
                "Authenticated POST " + path
        );
    }

    public static String patchWithAuth(
            String path,
            String jsonBody,
            String accessToken
    ) throws IOException {

        RequestBody body =
                RequestBody.create(
                        jsonBody,
                        JSON
                );

        Request request = userRequest(
                REST_URL + path,
                accessToken
        )
                .patch(body)
                .addHeader(
                        "Prefer",
                        "return=representation"
                )
                .build();

        return executeRequest(
                request,
                "Authenticated PATCH " + path
        );
    }

    public static String deleteWithAuth(
            String path,
            String accessToken
    ) throws IOException {

        Request request = userRequest(
                REST_URL + path,
                accessToken
        )
                .delete()
                .build();

        return executeRequest(
                request,
                "Authenticated DELETE " + path
        );
    }

    // =================================================
    // Supabase Storage
    // =================================================

    public static String uploadToStorage(
            String bucketName,
            String storagePath,
            byte[] imageBytes,
            String mimeType,
            String accessToken
    ) throws IOException {

        if (bucketName == null
                || bucketName.trim().isEmpty()) {

            throw new IOException(
                    "Storage bucket name is missing"
            );
        }

        if (storagePath == null
                || storagePath.trim().isEmpty()) {

            throw new IOException(
                    "Storage path is missing"
            );
        }

        if (imageBytes == null
                || imageBytes.length == 0) {

            throw new IOException(
                    "The selected image is empty"
            );
        }

        String url = STORAGE_URL
                + "/object/"
                + bucketName
                + "/"
                + storagePath;

        MediaType selectedMediaType =
                MediaType.get(
                        mimeType != null
                                && !mimeType.trim().isEmpty()
                                ? mimeType
                                : "image/jpeg"
                );

        RequestBody body =
                RequestBody.create(
                        imageBytes,
                        selectedMediaType
                );

        Request request = new Request.Builder()
                .url(url)
                .addHeader(
                        "apikey",
                        SUPABASE_ANON_KEY
                )
                .addHeader(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .addHeader(
                        "Content-Type",
                        selectedMediaType.toString()
                )
                .addHeader(
                        "x-upsert",
                        "true"
                )
                .post(body)
                .build();

        executeRequest(
                request,
                "Storage upload " + storagePath
        );

        return SUPABASE_URL
                + "/storage/v1/object/public/"
                + bucketName
                + "/"
                + storagePath;
    }

    // =================================================
    // Shared response handling
    // =================================================

    private static String executeRequest(
            Request request,
            String operation
    ) throws IOException {

        try (Response response =
                     getHttpClient()
                             .newCall(request)
                             .execute()) {

            String responseBody =
                    response.body() != null
                            ? response.body().string()
                            : "";

            // Paths can contain device tokens in query parameters; response bodies
            // can contain private profile data. Log only the route and status.
            String safeOperation = operation.split("\\?", 2)[0];
            Log.d(TAG, safeOperation + " | HTTP " + response.code());

            if (!response.isSuccessful()) {
                String errorCode = "";
                try {
                    errorCode = new JSONObject(responseBody).optString("code", "");
                } catch (Exception ignored) {
                    // A non-JSON error still has an HTTP status for diagnosis.
                }
                // The upvote screen uses this constraint name to explain repeat votes.
                String voteConstraint = responseBody.contains("report_votes_unique_per_citizen")
                        ? " (report_votes_unique_per_citizen)" : "";
                throw new IOException(
                        safeOperation
                                + " failed (HTTP "
                                + response.code()
                                + (errorCode.isEmpty() ? "" : ", code " + errorCode)
                                + ")" + voteConstraint
                );
            }

            return responseBody;
        }
    }
}
