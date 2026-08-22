package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ReportFaultPhotoActivity extends AppCompatActivity {

    private ReportData reportData;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_fault_photo);

        reportData = ((FixITCityApp) getApplication()).getReportData();

        // Camera placeholder click
        findViewById(R.id.camera_placeholder).setOnClickListener(v -> {
            // Launch camera intent
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        findViewById(R.id.btn_gallery).setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, REQUEST_IMAGE_GALLERY);
        });

        findViewById(R.id.btn_retake).setOnClickListener(v -> {
            // Clear photo – implement later
            Toast.makeText(this, "Photo cleared", Toast.LENGTH_SHORT).show();
            reportData.setPhotoCount(0);
        });

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (reportData.getPhotoCount() == 0) {
                Toast.makeText(this, "Please take or select a photo", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(ReportFaultSummaryActivity.class);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Photo taken or selected – increment count
            reportData.setPhotoCount(reportData.getPhotoCount() + 1);
            Toast.makeText(this, "Photo captured (" + reportData.getPhotoCount() + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void startActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }
}