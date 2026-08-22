package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class ReportFaultSummaryActivity extends AppCompatActivity {

    private TextView tvCategory, tvPhoto, tvLocation;
    private TextInputEditText etDetails;
    private ReportData reportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_fault_summary);

        reportData = ((FixITCityApp) getApplication()).getReportData();

        tvCategory = findViewById(R.id.tv_category_value);
        tvPhoto = findViewById(R.id.tv_photo_value);
        tvLocation = findViewById(R.id.tv_location_value);
        etDetails = findViewById(R.id.et_details);

        tvCategory.setText(reportData.getCategory());
        tvPhoto.setText(reportData.getPhotoCount() + " Image" + (reportData.getPhotoCount() > 1 ? "s" : "") + " Attached");
        tvLocation.setText(reportData.getLocationAddress());

        findViewById(R.id.btn_submit).setOnClickListener(v -> {
            reportData.setAdditionalDetails(etDetails.getText().toString());
            // Call your API here
            startActivity(new Intent(this, ReportSuccessActivity.class));
            finishAffinity();
        });
    }
}