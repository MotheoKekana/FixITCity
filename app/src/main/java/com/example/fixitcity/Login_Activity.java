package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.text.InputType;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseAuth;

import org.json.JSONObject;

public class Login_Activity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnSignIn, btnGuest, btnRegisterLink;
    private TextView tvForgotPassword;
    private ImageView ivTogglePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnGuest = findViewById(R.id.btnGuest);
        btnRegisterLink = findViewById(R.id.btnRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        // ===== Password visibility toggle =====
        final boolean[] isPasswordVisible = {false};

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

        // Sign In
        btnSignIn.setOnClickListener(v -> attemptLogin());

        // Guest mode
        btnGuest.setOnClickListener(v -> {
            startActivity(new Intent(this, Guest_Dashboard_Activity.class));
            finish();
        });

        // Register link
        btnRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(Login_Activity.this, Register_Activity.class));
        });

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot password coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignIn.setEnabled(false);
        btnSignIn.setText("Signing in...");

        new Thread(() -> {
            try {
                String response = SupabaseAuth.login(email, password);
                JSONObject json = new JSONObject(response);
                String accessToken = json.getString("access_token");
                String userId = json.getJSONObject("user").getString("id");
                String userEmail = json.getJSONObject("user").getString("email");

                SessionManager session = new SessionManager(this);
                session.saveSession(accessToken, userId, userEmail, "citizen");

                runOnUiThread(() -> {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, Guest_Dashboard_Activity.class));
                    finish();
                });

            } catch (Exception e) {
                Log.e(TAG, "Login error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    btnSignIn.setEnabled(true);
                    btnSignIn.setText("SIGN IN");
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}