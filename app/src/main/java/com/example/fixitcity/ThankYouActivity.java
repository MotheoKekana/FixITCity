package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ThankYouActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thank_you);

        // Read the report name passed from ProximityCheckActivity
        String reportName = getIntent().getStringExtra("report_name");
        TextView tvReportName = findViewById(R.id.tv_report_name);
        if (tvReportName != null) {
            if (reportName != null && !reportName.isEmpty()) {
                tvReportName.setText(reportName);
            } else {
                tvReportName.setText("this issue");
            }
        }

        // Go to Dashboard
        View btnGoDashboard = findViewById(R.id.btn_go_dashboard);
        if (btnGoDashboard != null) {
            btnGoDashboard.setOnClickListener(v -> {
                Intent intent = new Intent(this, Guest_Dashboard_Activity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
    }
}