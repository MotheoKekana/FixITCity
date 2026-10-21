package com.example.fixitcity.data;

import org.json.JSONObject;

import java.io.IOException;

public final class SessionRefresher {

    public enum Result {
        REFRESHED,
        OFFLINE,
        INVALID_SESSION
    }

    private SessionRefresher() {
    }

    // Serialize refreshes so two callers cannot rotate the same token.
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

            // Keep the existing refresh token if the response omits a replacement.
            if (newRefreshToken.isEmpty()) {
                newRefreshToken = refreshToken;
            }

            session.saveAuthTokens(newAccessToken, newRefreshToken);
            return Result.REFRESHED;
        } catch (IOException e) {
            // Connection errors must not sign out an offline user.
            String message = e.getMessage();
            if (message != null && message.startsWith("Token refresh failed:")) {
                return Result.INVALID_SESSION;
            }
            return Result.OFFLINE;
        } catch (Exception e) {
            return Result.INVALID_SESSION;
        }
    }
}