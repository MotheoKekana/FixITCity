package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import android.text.InputType;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseAuth;

import org.json.JSONObject;

public class Register_Activity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnRegister, btnSignIn;
    private CheckBox cbTerms;
    private ImageView ivTogglePassword, ivToggleConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Find views
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnSignIn = findViewById(R.id.btnSignIn);
        cbTerms = findViewById(R.id.cbTerms);

        // Password toggle icons
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);

        // ===== Set up password visibility toggles =====
        final boolean[] isPasswordVisible = {false};
        final boolean[] isConfirmVisible = {false};

        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible[0]) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
                isPasswordVisible[0] = false;
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                isPasswordVisible[0] = true;
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        ivToggleConfirmPassword.setOnClickListener(v -> {
            if (isConfirmVisible[0]) {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivToggleConfirmPassword.setImageResource(android.R.drawable.ic_menu_view);
                isConfirmVisible[0] = false;
            } else {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivToggleConfirmPassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                isConfirmVisible[0] = true;
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        // Button listeners
        btnRegister.setOnClickListener(v -> attemptRegister());
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, Login_Activity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the terms", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating account...");

        new Thread(() -> {
            try {
                String response = SupabaseAuth.register(email, password);
                Log.d(TAG, "Register response: " + response);

                // Save profile to users table + citizen_profiles table
                // (We'll do this in Phase 3 — for now, just show success)

                runOnUiThread(() -> {
                    Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, Login_Activity.class));
                    finish();
                });

            } catch (Exception e) {
                Log.e(TAG, "Register error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("REGISTER");
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}