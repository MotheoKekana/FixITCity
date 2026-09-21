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
import com.example.fixitcity.data.SupabaseUsers;

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

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnSignIn = findViewById(R.id.btnSignIn);
        cbTerms = findViewById(R.id.cbTerms);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);

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

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }
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

        // Split full name into first + last
        String firstName;
        String lastName = "";
        int spaceIdx = fullName.indexOf(' ');
        if (spaceIdx > 0) {
            firstName = fullName.substring(0, spaceIdx).trim();
            lastName = fullName.substring(spaceIdx + 1).trim();
        } else {
            firstName = fullName;
        }

        final String fFirstName = firstName;
        final String fLastName = lastName;
        final String fPhone = phone;

        new Thread(() -> {
            try {
                // STEP 1: Register with Supabase Auth
                String signupResponse = SupabaseAuth.register(email, password);
                Log.d(TAG, "Signup response: " + signupResponse);

                JSONObject signupJson = new JSONObject(signupResponse);
                String authUuid = signupJson.optString("id", null);
                if (authUuid == null || authUuid.isEmpty()) {
                    // Some Supabase versions return user under "user"
                    if (signupJson.has("user")) {
                        authUuid = signupJson.getJSONObject("user").optString("id", null);
                    }
                }
                if (authUuid == null || authUuid.isEmpty()) {
                    throw new Exception("Could not read auth user id from signup response");
                }

                // STEP 2: Log in to get an access token (needed for the next inserts)
                String loginResponse = SupabaseAuth.login(email, password);
                JSONObject loginJson = new JSONObject(loginResponse);
                String accessToken = loginJson.getString("access_token");

                // STEP 3: Insert into public.users
                long userId = SupabaseUsers.createUserRecord(authUuid, email, accessToken);
                Log.d(TAG, "Created user_id = " + userId);

                // STEP 4: Insert into public.citizen_profiles
                long citizenProfileId = SupabaseUsers.createCitizenProfile(
                        userId, fFirstName, fLastName, fPhone, accessToken);
                Log.d(TAG, "Created citizen_profile_id = " + citizenProfileId);

                // STEP 5: Save everything to session
                SessionManager session = new SessionManager(this);
                session.saveSession(accessToken, authUuid, email, "citizen");
                session.saveCitizenProfileId(citizenProfileId);
                session.saveProfileInfo(fFirstName, fLastName, fPhone);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Account created! Welcome.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, Dashboard_Activity.class));
                    finishAffinity();
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