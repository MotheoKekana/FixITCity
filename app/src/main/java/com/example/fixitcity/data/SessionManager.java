package com.example.fixitcity.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "FixITCitySession";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";                  // Supabase auth UUID
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_CITIZEN_PROFILE_ID = "citizen_profile_id";
    private static final String KEY_HAS_CITIZEN_PROFILE = "has_citizen_profile";
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

    public void saveCitizenProfileId(long citizenProfileId) {
        editor.putLong(KEY_CITIZEN_PROFILE_ID, citizenProfileId);
        editor.apply();
    }

    public void setHasCitizenProfile(boolean has) {
        editor.putBoolean(KEY_HAS_CITIZEN_PROFILE, has);
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

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
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
        return getAccessToken() != null;
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}