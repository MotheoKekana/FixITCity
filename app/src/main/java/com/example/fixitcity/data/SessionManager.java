package com.example.fixitcity.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "FixITCitySession";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ID_BIGINT = "user_id_bigint";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_CITIZEN_PROFILE_ID = "citizen_profile_id";
    private static final String KEY_HAS_CITIZEN_PROFILE = "has_citizen_profile";
    private static final String KEY_SEEN_ONBOARDING = "seen_onboarding";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_PHONE = "phone";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(String accessToken, String authUserId, String email, String role) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_USER_ID, authUserId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ROLE, role);
        editor.apply();
    }

    public void saveRefreshToken(String refreshToken) {
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    public void saveAccessToken(String accessToken) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.apply();
    }

    /**
     * Save a refreshed Supabase token pair in one preferences transaction.
     * Supabase can rotate the refresh token, so both values must be persisted
     * together before another authenticated request starts.
     */
    public void saveAuthTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor tokenEditor = prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken);

        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            tokenEditor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }

        tokenEditor.apply();
    }

    public void saveUserIdBigint(long userIdBigint) {
        editor.putLong(KEY_USER_ID_BIGINT, userIdBigint);
        editor.apply();
    }

    public void saveCitizenProfileId(long citizenProfileId) {
        editor.putLong(KEY_CITIZEN_PROFILE_ID, citizenProfileId);
        editor.apply();
    }

    public void setHasCitizenProfile(boolean has) {
        editor.putBoolean(KEY_HAS_CITIZEN_PROFILE, has);
        editor.apply();
    }

    public boolean hasSeenOnboarding() {
        return prefs.getBoolean(KEY_SEEN_ONBOARDING, false);
    }

    public void setSeenOnboarding(boolean value) {
        editor.putBoolean(KEY_SEEN_ONBOARDING, value);
        editor.apply();
    }

    public void saveProfileInfo(String firstName, String lastName, String phone) {
        editor.putString(KEY_FIRST_NAME, firstName);
        editor.putString(KEY_LAST_NAME, lastName);
        editor.putString(KEY_PHONE, phone);
        editor.apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public long getUserIdBigint() {
        return prefs.getLong(KEY_USER_ID_BIGINT, -1);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public long getCitizenProfileId() {
        return prefs.getLong(KEY_CITIZEN_PROFILE_ID, -1);
    }

    public boolean hasCitizenProfile() {
        return prefs.getBoolean(KEY_HAS_CITIZEN_PROFILE, false);
    }

    public boolean isGuest() {
        return "guest".equalsIgnoreCase(getRole());
    }

    public String getFirstName() {
        return prefs.getString(KEY_FIRST_NAME, "");
    }

    public String getLastName() {
        return prefs.getString(KEY_LAST_NAME, "");
    }

    public String getPhone() {
        return prefs.getString(KEY_PHONE, "");
    }

    public boolean isLoggedIn() {
        String accessToken = getAccessToken();
        return accessToken != null && !accessToken.trim().isEmpty();
    }

    public void logout() {
        // Onboarding is an app-level preference, not account data. Preserve it
        // when clearing a signed-in or guest session so logout/login does not
        // send the user through onboarding again.
        boolean seenOnboarding = hasSeenOnboarding();
        editor.clear();
        editor.putBoolean(KEY_SEEN_ONBOARDING, seenOnboarding);
        editor.apply();
    }
}
