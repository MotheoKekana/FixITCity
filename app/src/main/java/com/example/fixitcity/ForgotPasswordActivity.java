package com.example.fixitcity; // use your actual package name

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SupabaseAuth;
import com.example.fixitcity.data.SupabaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private int currentStep = 1;
    private String userEmail = "";
    private String accessToken = "";

    // Containers
    private View stepEmail, stepCode, stepPassword, stepSuccess;
    private CircularProgressIndicator progressIndicator;
    private TextView tvSubtitle, tvBackToSignIn;

    // Step 1
    private TextInputLayout tilEmail;
    private TextInputEditText inputEmail;
    private MaterialButton btnSendCode;

    // Step 2
    private TextInputLayout tilCode;
    private TextInputEditText inputCode;
    private MaterialButton btnVerifyCode;

    // Step 3
    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextInputEditText inputNewPassword, inputConfirmPassword;
    private MaterialButton btnResetPassword;

    // Step 4
    private MaterialButton btnBackToSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        ScreenInsets.apply(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentStep == 2 || currentStep == 3) {
                    showStep(1);
                } else {
                    finish();
                }
            }
        });

        bindViews();
        setupListeners();
        showStep(1);
    }

    private void bindViews() {
        stepEmail = findViewById(R.id.stepEmail);
        stepCode = findViewById(R.id.stepCode);
        stepPassword = findViewById(R.id.stepPassword);
        stepSuccess = findViewById(R.id.stepSuccess);

        progressIndicator = findViewById(R.id.progressIndicator);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvBackToSignIn = findViewById(R.id.tvBackToSignIn);

        tilEmail = findViewById(R.id.tilEmail);
        inputEmail = findViewById(R.id.inputEmail);
        btnSendCode = findViewById(R.id.btnSendCode);

        tilCode = findViewById(R.id.tilCode);
        inputCode = findViewById(R.id.inputCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);

        tilNewPassword = findViewById(R.id.tilNewPassword);
        inputNewPassword = findViewById(R.id.inputNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnBackToSignIn = findViewById(R.id.btnBackToSignIn);
    }

    private void setupListeners() {
        btnSendCode.setOnClickListener(v -> sendResetCode());
        btnVerifyCode.setOnClickListener(v -> verifyCode());
        btnResetPassword.setOnClickListener(v -> resetPassword());
        btnBackToSignIn.setOnClickListener(v -> finish());
        tvBackToSignIn.setOnClickListener(v -> finish());

        inputEmail.addTextChangedListener(clearError(tilEmail));
        inputCode.addTextChangedListener(clearError(tilCode));
        inputNewPassword.addTextChangedListener(clearError(tilNewPassword));
        inputConfirmPassword.addTextChangedListener(clearError(tilConfirmPassword));
    }

    private void showStep(int step) {
        currentStep = step;
        stepEmail.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        stepCode.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        stepPassword.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        stepSuccess.setVisibility(step == 4 ? View.VISIBLE : View.GONE);

        int subtitleRes;
        switch (step) {
            case 2: subtitleRes = R.string.forgot_password_subtitle_code; break;
            case 3: subtitleRes = R.string.forgot_password_subtitle_password; break;
            case 4: subtitleRes = R.string.forgot_password_subtitle_success; break;
            default: subtitleRes = R.string.forgot_password_subtitle_email; break;
        }
        tvSubtitle.setText(subtitleRes);
        tvBackToSignIn.setVisibility(step == 4 ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSendCode.setEnabled(!isLoading);
        btnVerifyCode.setEnabled(!isLoading);
        btnResetPassword.setEnabled(!isLoading);
    }

    // ─── STEP 1 ─────────────────────────────────────────────────────────
    // ─── STEP 1 ─────────────────────────────────────────────────────────
    private void sendResetCode() {
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email");
            return;
        }

        userEmail = email;
        setLoading(true);

        new Thread(() -> {
            try {
                // Call your Auth class (no return statement here!)
                SupabaseAuth.sendRecoveryEmail(userEmail);

                runOnUiThread(() -> {
                    setLoading(false);
                    showStep(2);
                    Toast.makeText(ForgotPasswordActivity.this, "If an account exists, a code was sent.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("FORGOT_PASS", "Error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    setLoading(false);
                    // Shows the error from Supabase (e.g., 500)
                    Toast.makeText(ForgotPasswordActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ─── STEP 2 ─────────────────────────────────────────────────────────
    private void verifyCode() {
        String code = inputCode.getText() != null ? inputCode.getText().toString().trim() : "";
        if (code.length() != 6) {
            tilCode.setError("Enter the 6-digit code");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                String responseStr = SupabaseAuth.verifyRecoveryCode(userEmail, code);

                // Extract access_token from JSON
                JSONObject json = new JSONObject(responseStr);
                accessToken = json.getString("access_token");

                runOnUiThread(() -> {
                    setLoading(false);
                    showStep(3);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    tilCode.setError("Invalid or expired code");
                });
            }
        }).start();
    }

    // ─── STEP 3 ─────────────────────────────────────────────────────────
    private void resetPassword() {
        String newPass = inputNewPassword.getText() != null ? inputNewPassword.getText().toString() : "";
        String confirmPass = inputConfirmPassword.getText() != null ? inputConfirmPassword.getText().toString() : "";

        String passwordError = RegistrationValidation.passwordError(newPass);
        if (passwordError != null) {
            tilNewPassword.setError(passwordError);
            return;
        }
        if (!newPass.equals(confirmPass)) {
            tilConfirmPassword.setError("Passwords do not match");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                SupabaseAuth.updatePassword(accessToken, newPass);
                runOnUiThread(() -> {
                    setLoading(false);
                    showStep(4);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this, "Failed to reset password. Please try again.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ─── Helpers ────────────────────────────────────────────────────────
    private TextWatcher clearError(TextInputLayout til) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { til.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        };
    }
}
