package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ReportSuccessActivity extends AppCompatActivity {

    private TextView tvReference, tvPriority, tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_success);

        tvReference = findViewById(R.id.tv_reference_number);
        tvPriority = findViewById(R.id.tv_priority_score);
        tvStatus = findViewById(R.id.tv_status);

        // Generate reference
        String ref = "FIX-" + new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date())
                + "-" + String.format("%03d", new Random().nextInt(1000));
        tvReference.setText(ref);

        // Priority from API (dummy)
        int priority = 92;
        tvPriority.setText(priority + "/100");
        tvStatus.setText("Pending");

        findViewById(R.id.btn_go_dashboard).setOnClickListener(v -> finishAffinity());
        findViewById(R.id.btn_report_another).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportFaultCategoryActivity.class));
            finishAffinity();
        });
        findViewById(R.id.btn_track_report).setOnClickListener(v -> {
            Toast.makeText(this, "Track report: " + ref, Toast.LENGTH_SHORT).show();
        });
    }
}