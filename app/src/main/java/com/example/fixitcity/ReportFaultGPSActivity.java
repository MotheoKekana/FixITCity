package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ReportFaultGPSActivity extends AppCompatActivity {

    private TextView tvAddress, tvLat, tvLng;
    private ReportData reportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_fault_gps);

        reportData = ((FixITCityApp) getApplication()).getReportData();

        tvAddress = findViewById(R.id.tv_location_address);
        tvLat = findViewById(R.id.tv_latitude);
        tvLng = findViewById(R.id.tv_longitude);

        // Simulate getting GPS location
        fetchLocation();

        findViewById(R.id.btn_adjust_pin).setOnClickListener(v -> {
            // Open map picker or adjust pin – implement later
            Toast.makeText(this, "Adjust pin clicked", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (reportData.getLocationAddress() == null) {
                Toast.makeText(this, "Please confirm your location", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(ReportFaultPhotoActivity.class);
            }
        });
    }

    private void fetchLocation() {
        // Dummy location – replace with actual GPS / API
        String address = "123 Main Street, Gqeberha";
        double lat = -33.9608;
        double lng = 25.6022;

        reportData.setLocationAddress(address);
        reportData.setLatitude(lat);
        reportData.setLongitude(lng);

        tvAddress.setText(address);
        tvLat.setText("Latitude: " + lat);
        tvLng.setText("Longitude: " + lng);
    }

    private void startActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }
}