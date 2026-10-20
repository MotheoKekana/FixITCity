package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;

public class ThankYouActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thank_you);

        ScreenInsets.apply(this);

        // Get the report name passed from ProximityCheckActivity
        String reportName = getIntent().getStringExtra("report_name");

        // Find the title TextView and update it dynamically
        TextView tvTitle = findViewById(R.id.tv_thank_you_title);
        if (tvTitle != null) {
            if (reportName != null && !reportName.isEmpty()) {
                // This will display: Thank you for upvoting "Burst Pipe"
                tvTitle.setText("Thank you for upvoting\n\"" + reportName + "\"");
            } else {
                tvTitle.setText("Thank you for upvoting!");
            }
        }

        // Route to the right dashboard based on real login state
        View btnGoDashboard = findViewById(R.id.btn_go_dashboard);
        if (btnGoDashboard != null) {
            btnGoDashboard.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);

                Class<?> target;
                if (session.isGuest() && session.hasGuestAuthSession()) {
                    target = Guest_Dashboard_Activity.class;
                } else if (session.isLoggedIn() && session.getCitizenProfileId() > 0) {
                    target = Dashboard_Activity.class;
                } else {
                    target = Login_Activity.class;
                }

                Intent intent = new Intent(this, target);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}