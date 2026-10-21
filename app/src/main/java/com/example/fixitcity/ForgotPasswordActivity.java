package com.example.fixitcity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SupabaseAuth;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

public class ForgotPasswordActivity extends AppCompatActivity {

    private int currentStep = 1;
    private String userEmail = "";
    private String accessToken = "";
    private CountDownTimer countDownTimer;

    private View stepEmail, stepCode, stepPassword, stepSuccess;
    private View step1Bar, step2Bar, step3Bar;
    private TextView tvTitle, tvSubtitle, tvRedirect;

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSendCode;

    private TextInputLayout tilCode;
    private TextInputEditText etCode;
    private MaterialButton btnVerifyCode;

    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private MaterialButton btnResetPassword;

    private MaterialButton btnGoToSignIn, btnBackToSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

        step1Bar = findViewById(R.id.step1Bar);
        step2Bar = findViewById(R.id.step2Bar);
        step3Bar = findViewById(R.id.step3Bar);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvRedirect = findViewById(R.id.tvRedirect);

        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnSendCode = findViewById(R.id.btnSendCode);

        tilCode = findViewById(R.id.tilCode);
        etCode = findViewById(R.id.etCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);

        tilNewPassword = findViewById(R.id.tilNewPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnGoToSignIn = findViewById(R.id.btnGoToSignIn);
        btnBackToSignIn = findViewById(R.id.btnBackToSignIn);
    }

    private void setupListeners() {
        btnSendCode.setOnClickListener(v -> sendResetCode());
        btnVerifyCode.setOnClickListener(v -> verifyCode());
        btnResetPassword.setOnClickListener(v -> resetPassword());

        btnGoToSignIn.setOnClickListener(v -> finish());
        btnBackToSignIn.setOnClickListener(v -> finish());

        etEmail.addTextChangedListener(clearError(tilEmail));
        etCode.addTextChangedListener(clearError(tilCode));
        etNewPassword.addTextChangedListener(clearError(tilNewPassword));
        etConfirmPassword.addTextChangedListener(clearError(tilConfirmPassword));
    }

    private void showStep(int step) {
        currentStep = step;

        stepEmail.setVisibility(View.GONE);
        stepCode.setVisibility(View.GONE);
        stepPassword.setVisibility(View.GONE);
        stepSuccess.setVisibility(View.GONE);

        switch (step) {
            case 1:
                stepEmail.setVisibility(View.VISIBLE);
                tvTitle.setText(R.string.fp_step1_title);
                tvSubtitle.setText(R.string.fp_step1_subtitle);
                break;
            case 2:
                stepCode.setVisibility(View.VISIBLE);
                tvTitle.setText(R.string.fp_step2_title);
                tvSubtitle.setText(R.string.fp_step2_subtitle);
                break;
            case 3:
                stepPassword.setVisibility(View.VISIBLE);
                tvTitle.setText(R.string.fp_step3_title);
                tvSubtitle.setText(R.string.fp_step3_subtitle);
                break;
            case 4:
                stepSuccess.setVisibility(View.VISIBLE);
                tvTitle.setText(R.string.fp_step4_title);
                tvSubtitle.setText(R.string.fp_step4_subtitle);
                btnBackToSignIn.setVisibility(View.GONE);
                startSuccessCountdown();
                break;
        }

        step1Bar.setBackgroundResource(step >= 1 ? R.drawable.bg_step_active : R.drawable.bg_step_inactive);
        step2Bar.setBackgroundResource(step >= 2 ? R.drawable.bg_step_active : R.drawable.bg_step_inactive);
        step3Bar.setBackgroundResource(step >= 3 ? R.drawable.bg_step_active : R.drawable.bg_step_inactive);

        if (step < 4) {
            btnBackToSignIn.setVisibility(View.VISIBLE);
        }
    }

    private void setLoading(boolean isLoading) {
        btnSendCode.setEnabled(!isLoading);
        btnVerifyCode.setEnabled(!isLoading);
        btnResetPassword.setEnabled(!isLoading);

        if (isLoading) {
            btnSendCode.setText("Please wait...");
            btnVerifyCode.setText("Please wait...");
            btnResetPassword.setText("Please wait...");
        } else {
            btnSendCode.setText(R.string.btn_send_code);
            btnVerifyCode.setText(R.string.btn_verify_code);
            btnResetPassword.setText(R.string.btn_reset_password);
        }
    }

    private void sendResetCode() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return;
        }

        userEmail = email;
        setLoading(true);

        new Thread(() -> {
            try {
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
                    Toast.makeText(ForgotPasswordActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void verifyCode() {
        String code = etCode.getText() != null ? etCode.getText().toString().trim() : "";

        if (code.isEmpty()) {
            tilCode.setError(getString(R.string.error_code_required));
            return;
        }
        if (code.length() != 6) {
            tilCode.setError("Enter the 6-digit code");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                String responseStr = SupabaseAuth.verifyRecoveryCode(userEmail, code);
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

    private void resetPassword() {
        String newPass = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (newPass.length() < 8) {
            tilNewPassword.setError(getString(R.string.error_password_short));
            return;
        }
        if (!newPass.equals(confirmPass)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
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
                Log.e("FORGOT_PASS", "Update failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this, "Failed to reset password. Please try again.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void startSuccessCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                tvRedirect.setText(getString(R.string.success_redirect, secondsLeft));
            }

            @Override
            public void onFinish() {
                tvRedirect.setText(getString(R.string.success_redirect, 0));
                finish();
            }
        }.start();
    }

    private TextWatcher clearError(TextInputLayout til) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { til.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}