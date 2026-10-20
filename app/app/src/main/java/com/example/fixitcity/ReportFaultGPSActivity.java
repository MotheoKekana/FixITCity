package com.example.fixitcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ReportFaultGPSActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private TextView tvAddress;
    private ReportData reportData;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_fault_gps);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hideNavForGuests();
        AppNavigation.bindNotificationBell(this);

        reportData = ((FixITCityApp) getApplication()).getReportData();
        tvAddress = findViewById(R.id.tv_location_address);
        tvAddress.setText("Finding your location...");
        reportData.setLocationAddress(null);
        reportData.setLatitude(0);
        reportData.setLongitude(0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (checkLocationPermission()) {
            getCurrentLocation();
        } else {
            requestLocationPermission();
        }

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (reportData.getLocationAddress() == null
                    || reportData.getLocationAddress().isEmpty()
                    || (reportData.getLatitude() == 0 && reportData.getLongitude() == 0)) {
                Toast.makeText(this, "Waiting for your GPS location", Toast.LENGTH_SHORT).show();
                if (checkLocationPermission()) getCurrentLocation();
            } else {
                startNextActivity(new SessionManager(this).isGuest()
                        ? ReportFaultPhotoActivity.class : ProximityCheckActivity.class);
            }
        });

        findViewById(R.id.navHome).setOnClickListener(v -> AppNavigation.goHome(this));
        findViewById(R.id.navReport).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportFaultCategoryActivity.class));
            finish();
        });
        findViewById(R.id.navTrack).setOnClickListener(v ->
                startActivity(new Intent(this, TrackReportSearchActivity.class)));
        findViewById(R.id.navSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileSettingsActivity.class)));
    }

    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
    }

    private void getCurrentLocation() {
        if (!checkLocationPermission()) return;

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                android.location.Location location = locationResult.getLastLocation();
                if (location != null) {
                    reverseGeocode(location.getLatitude(), location.getLongitude());
                } else {
                    tvAddress.setText("Location unavailable. Tap Next Step to retry.");
                    Toast.makeText(ReportFaultGPSActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void reverseGeocode(double lat, double lng) {
        String coordinates = String.format(Locale.US, "%.6f, %.6f", lat, lng);
        reportData.setLatitude(lat);
        reportData.setLongitude(lng);
        reportData.setLocationAddress(coordinates);
        tvAddress.setText(coordinates);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                if (fullAddress != null && !fullAddress.trim().isEmpty()) {
                    tvAddress.setText(fullAddress);
                    reportData.setLocationAddress(fullAddress);
                }
            }
        } catch (IOException e) {
            // Coordinates are enough to submit.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startNextActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
