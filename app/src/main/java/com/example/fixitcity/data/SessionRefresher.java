package com.example.fixitcity.data;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Refreshes a saved Supabase session before the app makes authenticated calls.
 */
public final class SessionRefresher {

    public enum Result {
        REFRESHED,
        OFFLINE,
        INVALID_SESSION
    }

    private SessionRefresher() {
    }

    /**
     * Synchronized so the splash screen and WorkManager cannot rotate the same
     * refresh token at the same time inside this app process.
     */
    public static synchronized Result refresh(SessionManager session) {
        String refreshToken = session.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Result.INVALID_SESSION;
        }

        try {
            String response = SupabaseAuth.refreshToken(refreshToken);
            if (response == null || response.trim().isEmpty()) {
                return Result.OFFLINE;
            }

            JSONObject json = new JSONObject(response);
            String newAccessToken = json.optString("access_token", "").trim();
            String newRefreshToken = json.optString("refresh_token", "").trim();

            if (newAccessToken.isEmpty()) {
                return Result.INVALID_SESSION;
            }

            // Some responses may omit a replacement refresh token.
            if (newRefreshToken.isEmpty()) {
                newRefreshToken = refreshToken;
            }

            session.saveAuthTokens(newAccessToken, newRefreshToken);
            return Result.REFRESHED;
        } catch (IOException e) {
            // SupabaseAuth uses this prefix only after receiving an HTTP error.
            // Other IOExceptions are connection/timeout failures and must not
            // sign an offline user out.
            String message = e.getMessage();
            if (message != null && message.startsWith("Token refresh failed:")) {
                return Result.INVALID_SESSION;
            }
            return Result.OFFLINE;
        } catch (Exception e) {
            // A successful refresh must contain valid session JSON.
            return Result.INVALID_SESSION;
        }
    }
}