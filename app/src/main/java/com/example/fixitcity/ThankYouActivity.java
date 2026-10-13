package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;

public class ThankYouActivity extends AppCompatActivity {

    private static final String TAG = "ThankYouActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thank_you);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Optional report name (from ProximityCheckActivity)
        String reportName = getIntent().getStringExtra("report_name");
        TextView tvReportName = findViewById(R.id.tv_report_name);
        if (tvReportName != null) {
            if (reportName != null && !reportName.isEmpty()) {
                tvReportName.setText(reportName);
            } else {
                tvReportName.setText("this issue");
            }
        }

        // Route to the right dashboard based on real login state
        View btnGoDashboard = findViewById(R.id.btn_go_dashboard);
        if (btnGoDashboard != null) {
            btnGoDashboard.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);

                boolean realLogin = session.isLoggedIn()
                        && session.getCitizenProfileId() > 0;

                Log.d(TAG, "Go Home tapped — isLoggedIn=" + session.isLoggedIn()
                        + " citizenProfileId=" + session.getCitizenProfileId()
                        + " isGuest=" + session.isGuest()
                        + " → routing to "
                        + (realLogin ? "Dashboard_Activity" : "Guest_Dashboard_Activity"));

                Class<?> target = realLogin
                        ? Dashboard_Activity.class
                        : Guest_Dashboard_Activity.class;

                Intent intent = new Intent(this, target);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}