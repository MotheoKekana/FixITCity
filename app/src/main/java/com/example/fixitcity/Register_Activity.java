package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseAuth;
import com.example.fixitcity.data.SupabaseUsers;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

public class Register_Activity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etPassword;
    private EditText etConfirmPassword;

    private Button btnRegister;
    private Button btnSignIn;

    private CheckBox cbTerms;
    private boolean registrationInProgress;

    private ImageView ivTogglePassword;
    private ImageView ivToggleConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ScreenInsets.apply(this);

        initialiseViews();
        setupPasswordVisibility();
        setupButtons();
    }

    private void initialiseViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);

        etConfirmPassword =
                findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        btnSignIn = findViewById(R.id.btnSignIn);

        cbTerms = findViewById(R.id.cbTerms);

        ivTogglePassword =
                findViewById(R.id.ivTogglePassword);

        ivToggleConfirmPassword =
                findViewById(
                        R.id.ivToggleConfirmPassword
                );
    }

    private void setupPasswordVisibility() {
        final boolean[] passwordVisible = {false};
        final boolean[] confirmVisible = {false};

        ivTogglePassword.setOnClickListener(view -> {
            passwordVisible[0] =
                    !passwordVisible[0];

            setPasswordVisibility(
                    etPassword,
                    ivTogglePassword,
                    passwordVisible[0]
            );
        });

        ivToggleConfirmPassword.setOnClickListener(
                view -> {
                    confirmVisible[0] =
                            !confirmVisible[0];

                    setPasswordVisibility(
                            etConfirmPassword,
                            ivToggleConfirmPassword,
                            confirmVisible[0]
                    );
                }
        );
    }

    private void setPasswordVisibility(
            EditText editText,
            ImageView icon,
            boolean visible
    ) {
        if (visible) {
            editText.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType
                            .TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );

            icon.setImageResource(
                    android.R.drawable
                            .ic_menu_close_clear_cancel
            );

        } else {
            editText.setInputType(
                    InputType.TYPE_CLASS_TEXT
                            | InputType
                            .TYPE_TEXT_VARIATION_PASSWORD
            );

            icon.setImageResource(
                    android.R.drawable.ic_menu_view
            );
        }

        editText.setSelection(
                editText.getText().length()
        );
    }

    private void setupButtons() {
        btnRegister.setOnClickListener(
                view -> attemptRegister()
        );

        btnSignIn.setOnClickListener(view -> {
            startActivity(
                    new Intent(
                            this,
                            Login_Activity.class
                    )
            );

            finish();
        });
    }

    private void attemptRegister() {
        if (registrationInProgress) return;

        etFullName.setError(null);
        etEmail.setError(null);
        etPhone.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (fullName.isEmpty()) {
            showFieldError(etFullName, "Please enter your full name.");
            return;
        }
        if (email.isEmpty()) {
            showFieldError(etEmail, "Please enter your email address.");
            return;
        }
        if (email.length() > RegistrationValidation.MAX_EMAIL_CHARACTERS
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                || email.contains("..") || email.startsWith(".")
                || email.substring(0, email.indexOf('@')).endsWith(".")) {
            showFieldError(etEmail, "Enter a valid email address, such as name@example.com.");
            return;
        }

        final String phone;
        try {
            phone = RegistrationValidation.normalizeOptionalPhone(
                    etPhone.getText().toString());
        } catch (IllegalArgumentException invalidPhone) {
            showFieldError(etPhone, invalidPhone.getMessage());
            return;
        }

        String passwordError = RegistrationValidation.passwordError(password);
        if (passwordError != null) {
            showFieldError(etPassword, passwordError);
            return;
        }
        if (confirmPassword.isEmpty()) {
            showFieldError(etConfirmPassword, "Please enter your password again.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showFieldError(etConfirmPassword, "Passwords do not match.");
            return;
        }
        if (!cbTerms.isChecked()) {
            showMessage("Please accept the terms and conditions.");
            return;
        }

        NameParts nameParts = splitFullName(fullName);
        if (nameParts.firstName.codePointCount(0, nameParts.firstName.length()) > 100
                || nameParts.lastName.codePointCount(0, nameParts.lastName.length()) > 100) {
            showFieldError(etFullName, "Your first name and surname must each be 100 characters or fewer.");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            boolean accountCreated = false;
            try {
                String signupResponse =
                        SupabaseAuth.register(
                                email,
                                password,
                                nameParts.firstName,
                                nameParts.lastName,
                                phone
                        );

                if (signupResponse == null
                        || signupResponse.trim().isEmpty()) {

                    throw new IOException(
                            "The authentication server "
                                    + "returned an empty response"
                    );
                }

                JSONObject signupJson =
                        new JSONObject(signupResponse);

                JSONObject userJson =
                        signupJson.optJSONObject("user");

                // Some Auth responses return the user
                // directly instead of inside "user".
                if (userJson == null
                        && signupJson.has("id")) {

                    userJson = signupJson;
                }

                if (userJson == null) {
                    throw new IOException(
                            "Could not read the new account"
                    );
                }

                String authUserId =
                        userJson.optString("id", null);

                if (authUserId == null
                        || authUserId.isEmpty()) {

                    throw new IOException(
                            "Could not read the authentication ID"
                    );
                }

                accountCreated = true;

                String accessToken =
                        signupJson.optString(
                                "access_token",
                                null
                        );

                String refreshToken =
                        signupJson.optString(
                                "refresh_token",
                                null
                        );

                /*
                 * If email confirmation is enabled,
                 * signup succeeds but no session is returned.
                 */
                if (accessToken == null
                        || accessToken.isEmpty()) {

                    showEmailConfirmationRequired();
                    return;
                }

                loadProfileAndFinishRegistration(
                        authUserId,
                        email,
                        accessToken,
                        refreshToken
                );

            } catch (Exception exception) {
                Log.e(
                        TAG,
                        "Registration error: "
                                + exception.getMessage(),
                        exception
                );

                final boolean created = accountCreated;
                final String error = created
                        ? "Your account was created, but we could not finish signing you in. "
                          + "Please sign in instead of registering again."
                        : getReadableError(exception);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    setLoading(false);
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    if (created) {
                        Intent intent = new Intent(this, Login_Activity.class);
                        intent.putExtra("registered_email", email);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        }).start();
    }

    private void loadProfileAndFinishRegistration(
            String authUserId,
            String email,
            String accessToken,
            String refreshToken
    ) throws IOException {

        /*
         * The database trigger should already have created
         * public.users and citizen_profiles before the
         * signup request returns.
         */
        long userId =
                SupabaseUsers.findUserIdByAuthUuid(
                        authUserId,
                        accessToken
                );

        if (userId <= 0) {
            throw new IOException(
                    "The database user record "
                            + "was not created"
            );
        }

        SupabaseUsers.CitizenProfileDTO profile =
                SupabaseUsers.findCitizenProfileByUserId(
                        userId,
                        accessToken
                );

        if (profile == null
                || profile.citizen_profile_id <= 0) {

            throw new IOException(
                    "The citizen profile was not created"
            );
        }

        SessionManager session =
                new SessionManager(this);

        session.logout();

        session.saveSession(
                accessToken,
                authUserId,
                email,
                "citizen"
        );

        if (refreshToken != null
                && !refreshToken.isEmpty()) {

            session.saveRefreshToken(refreshToken);
        }

        session.saveUserIdBigint(userId);

        session.saveCitizenProfileId(
                profile.citizen_profile_id
        );

        session.saveProfileInfo(
                profile.first_name,
                profile.last_name,
                profile.phone_number
        );

        session.setHasCitizenProfile(true);

        runOnUiThread(() -> {
            startActivity(
                    new Intent(
                            this,
                            Dashboard_Activity.class
                    )
            );

            finishAffinity();
        });
    }

    private void showEmailConfirmationRequired() {
        runOnUiThread(() -> {
            setLoading(false);

            Toast.makeText(
                    this,
                    "Account created. Check your email "
                            + "to confirm your account, "
                            + "then sign in.",
                    Toast.LENGTH_LONG
            ).show();

            Intent intent =
                    new Intent(
                            this,
                            Login_Activity.class
                    );

            intent.putExtra(
                    "registered_email",
                    etEmail.getText()
                            .toString()
                            .trim()
            );

            startActivity(intent);
            finish();
        });
    }

    private NameParts splitFullName(String fullName) {
        String cleanedName =
                fullName.trim()
                        .replaceAll("\\s+", " ");

        int firstSpace =
                cleanedName.indexOf(' ');

        if (firstSpace < 0) {
            return new NameParts(
                    cleanedName,
                    ""
            );
        }

        String firstName =
                cleanedName
                        .substring(0, firstSpace)
                        .trim();

        String lastName =
                cleanedName
                        .substring(firstSpace + 1)
                        .trim();

        return new NameParts(
                firstName,
                lastName
        );
    }

    private void setLoading(boolean loading) {
        registrationInProgress = loading;
        btnRegister.setEnabled(!loading);
        btnSignIn.setEnabled(!loading);
        etFullName.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPhone.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        etConfirmPassword.setEnabled(!loading);
        cbTerms.setEnabled(!loading);
        ivTogglePassword.setEnabled(!loading);
        ivToggleConfirmPassword.setEnabled(!loading);
        btnRegister.setText(loading ? "Creating account..." : "REGISTER");
    }

    private void showFieldError(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private String getReadableError(Exception exception) {
        String raw = exception.getMessage();
        if (raw == null || raw.trim().isEmpty()) {
            return "Registration failed. Please try again.";
        }

        String code = "";
        String serverMessage = null;
        int jsonStart = raw.indexOf('{');
        if (jsonStart >= 0) {
            try {
                JSONObject error = new JSONObject(raw.substring(jsonStart));
                code = error.optString("error_code", error.optString("code", ""));
                serverMessage = error.optString("msg", error.optString("message", ""));
            } catch (Exception ignored) {
                // Network exceptions do not have an Auth JSON response.
            }
        }
        String lower = (code + " " + raw).toLowerCase(Locale.ROOT);
        if (lower.contains("already registered") || lower.contains("user already exists")
                || lower.contains("user_already_exists") || lower.contains("email_exists")) {
            return "An account already exists for this email address. Please sign in.";
        }
        if (lower.contains("rate_limit") || lower.contains("rate limit")
                || raw.contains("(429)")) {
            return "Too many attempts. Please wait a while before trying again.";
        }
        if (lower.contains("email_address_invalid") || lower.contains("email address is invalid")) {
            return "Please use a valid email address that can receive email.";
        }
        if (lower.contains("password")) {
            // Show Supabase's actual rule when the project requires a stronger
            // password than the app's baseline. Never change the entered password.
            if (serverMessage != null && !serverMessage.trim().isEmpty()
                    && serverMessage.length() <= 400) return serverMessage;
            return "Supabase rejected this password. Try a longer, unique password "
                    + "with uppercase and lowercase letters, numbers and a symbol.";
        }
        if (lower.contains("unable to resolve host") || lower.contains("failed to connect")
                || lower.contains("timeout") || lower.contains("timed out")) {
            return "Could not connect. Check your internet connection.";
        }
        return "Registration could not be completed. Please try again. "
                + "If this continues, contact support.";
    }

    private static class NameParts {

        final String firstName;
        final String lastName;

        NameParts(
                String firstName,
                String lastName
        ) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}
