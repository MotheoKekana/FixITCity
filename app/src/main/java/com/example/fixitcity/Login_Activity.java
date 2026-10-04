package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

public class Login_Activity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail;
    private EditText etPassword;

    private Button btnSignIn;
    private Button btnGuest;
    private Button btnRegisterLink;

    private TextView tvForgotPassword;
    private ImageView ivTogglePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnSignIn = findViewById(R.id.btnSignIn);
        btnGuest = findViewById(R.id.btnGuest);
        btnRegisterLink =
                findViewById(R.id.btnRegisterLink);

        tvForgotPassword =
                findViewById(R.id.tvForgotPassword);

        ivTogglePassword =
                findViewById(R.id.ivTogglePassword);
    }

    private void setupPasswordVisibility() {
        final boolean[] isPasswordVisible = {false};

        ivTogglePassword.setOnClickListener(view -> {
            if (isPasswordVisible[0]) {
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT
                                | InputType
                                .TYPE_TEXT_VARIATION_PASSWORD
                );

                ivTogglePassword.setImageResource(
                        android.R.drawable.ic_menu_view
                );

                isPasswordVisible[0] = false;

            } else {
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT
                                | InputType
                                .TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );

                ivTogglePassword.setImageResource(
                        android.R.drawable
                                .ic_menu_close_clear_cancel
                );

                isPasswordVisible[0] = true;
            }

            etPassword.setSelection(
                    etPassword.getText().length()
            );
        });
    }

    private void setupButtons() {
        btnSignIn.setOnClickListener(
                view -> attemptLogin()
        );

        btnGuest.setOnClickListener(view -> {
            SessionManager guestSession =
                    new SessionManager(this);

            guestSession.logout();

            guestSession.saveSession(
                    "guest",
                    "guest",
                    "guest@local",
                    "guest"
            );

            guestSession.setHasCitizenProfile(false);

            startActivity(
                    new Intent(
                            this,
                            Guest_Dashboard_Activity.class
                    )
            );

            finish();
        });

        btnRegisterLink.setOnClickListener(view ->
                startActivity(
                        new Intent(
                                this,
                                Register_Activity.class
                        )
                )
        );

        tvForgotPassword.setOnClickListener(view ->
                Toast.makeText(
                        this,
                        "Forgot password coming soon",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    private void attemptLogin() {
        String email =
                etEmail.getText().toString().trim();

        String password =
                etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please enter your email and password",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                String response =
                        SupabaseAuth.login(
                                email,
                                password
                        );

                if (response == null
                        || response.trim().isEmpty()) {

                    throw new IOException(
                            "The authentication server "
                                    + "returned an empty response"
                    );
                }

                JSONObject json =
                        new JSONObject(response);

                String accessToken =
                        json.getString("access_token");

                String refreshToken =
                        json.optString(
                                "refresh_token",
                                null
                        );

                JSONObject userJson =
                        json.getJSONObject("user");

                String authUserId =
                        userJson.getString("id");

                String userEmail =
                        userJson.optString(
                                "email",
                                email
                        );

                long userId =
                        SupabaseUsers
                                .findUserIdByAuthUuid(
                                        authUserId,
                                        accessToken
                                );

                if (userId <= 0) {
                    throw new IOException(
                            "Your FixITCity user record "
                                    + "could not be found"
                    );
                }

                SupabaseUsers.CitizenProfileDTO profile =
                        SupabaseUsers
                                .findCitizenProfileByUserId(
                                        userId,
                                        accessToken
                                );

                if (profile == null
                        || profile.citizen_profile_id <= 0) {

                    throw new IOException(
                            "Your citizen profile "
                                    + "could not be found"
                    );
                }

                saveSession(
                        accessToken,
                        refreshToken,
                        authUserId,
                        userEmail,
                        userId,
                        profile
                );

                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            "Welcome back, "
                                    + profile.first_name,
                            Toast.LENGTH_SHORT
                    ).show();

                    startActivity(
                            new Intent(
                                    this,
                                    Dashboard_Activity.class
                            )
                    );

                    finish();
                });

            } catch (Exception exception) {
                Log.e(
                        TAG,
                        "Login error: "
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

    private void saveSession(
            String accessToken,
            String refreshToken,
            String authUserId,
            String email,
            long userId,
            SupabaseUsers.CitizenProfileDTO profile
    ) {
        SessionManager session =
                new SessionManager(this);

        // Remove data belonging to a previous account.
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
    }

    private void setLoading(boolean loading) {
        btnSignIn.setEnabled(!loading);

        btnSignIn.setText(
                loading
                        ? "Signing in..."
                        : "SIGN IN"
        );
    }

    private String getReadableError(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return "Login failed. Please try again.";
        }

        String lower = message.toLowerCase();

        if (lower.contains("invalid login credentials")) {
            return "Incorrect email or password.";
        }

        if (lower.contains("email not confirmed")) {
            return "Please confirm your email before signing in.";
        }

        if (lower.contains("unable to resolve host")
                || lower.contains("failed to connect")
                || lower.contains("timeout")) {

            return "Could not connect. Check your internet connection.";
        }

        return "Login failed: " + message;
    }
}