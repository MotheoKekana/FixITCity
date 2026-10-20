package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseAuth;
import com.example.fixitcity.data.SupabaseUsers;

import org.json.JSONObject;

import java.io.IOException;

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

    private ImageView ivTogglePassword;
    private ImageView ivToggleConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

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
        String fullName =
                etFullName.getText().toString().trim();

        String email =
                etEmail.getText().toString().trim();

        String phone =
                etPhone.getText().toString().trim();

        String password =
                etPassword.getText().toString();

        String confirmPassword =
                etConfirmPassword
                        .getText()
                        .toString();

        if (fullName.isEmpty()) {
            showMessage("Please enter your full name");
            return;
        }

        if (email.isEmpty()) {
            showMessage("Please enter your email address");
            return;
        }

        if (phone.isEmpty()) {
            showMessage("Please enter your phone number");
            return;
        }

        if (password.isEmpty()) {
            showMessage("Please enter a password");
            return;
        }

        if (password.length() < 6) {
            showMessage(
                    "Your password must contain "
                            + "at least 6 characters"
            );
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Passwords do not match");
            return;
        }

        if (!cbTerms.isChecked()) {
            showMessage(
                    "Please accept the terms and conditions"
            );
            return;
        }

        NameParts nameParts =
                splitFullName(fullName);

        setLoading(true);

        new Thread(() -> {
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

                runOnUiThread(() -> {
                    setLoading(false);

                    Toast.makeText(
                            this,
                            getReadableError(exception),
                            Toast.LENGTH_LONG
                    ).show();
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
            Toast.makeText(
                    this,
                    "Account created successfully",
                    Toast.LENGTH_LONG
            ).show();

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
        btnRegister.setEnabled(!loading);

        btnRegister.setText(
                loading
                        ? "Creating account..."
                        : "REGISTER"
        );
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private String getReadableError(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return "Registration failed. Please try again.";
        }

        String lower = message.toLowerCase();

        if (lower.contains("already registered")
                || lower.contains("user already exists")) {

            return "An account already exists "
                    + "for this email address.";
        }

        if (lower.contains("password")) {
            return "The password does not meet "
                    + "the security requirements.";
        }

        if (lower.contains("unable to resolve host")
                || lower.contains("failed to connect")
                || lower.contains("timeout")) {

            return "Could not connect. Check your "
                    + "internet connection.";
        }

        return "Registration failed: " + message;
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