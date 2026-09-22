package com.example.fixitcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ReportFaultGPSActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private TextView tvAddress;
    private TextInputEditText etAddressSearch;
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

        reportData = ((FixITCityApp) getApplication()).getReportData();
        tvAddress = findViewById(R.id.tv_location_address);
        etAddressSearch = findViewById(R.id.et_address_search);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (checkLocationPermission()) {
            getCurrentLocation();
        } else {
            requestLocationPermission();
        }

        etAddressSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = etAddressSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    validateAndSetAddress(query);
                }
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_adjust_pin).setOnClickListener(v -> {
            String address = tvAddress.getText().toString();
            openMapPicker(address);
        });

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (reportData.getLocationAddress() == null || reportData.getLocationAddress().isEmpty()) {
                Toast.makeText(this, "Please confirm your location", Toast.LENGTH_SHORT).show();
            } else {
                if (reportData.getLatitude() == 0 && reportData.getLongitude() == 0) {
                    geocodeAddressForLocation(reportData.getLocationAddress());
                } else {
                    startNextActivity(ProximityCheckActivity.class);
                }
            }
        });

        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navReport).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportFaultCategoryActivity.class));
            finish();
        });
        findViewById(R.id.navTrack).setOnClickListener(v ->
                startActivity(new Intent(this, TrackReportSearchActivity.class)));
        findViewById(R.id.navSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileSettingsActivity.class)));
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
                    Toast.makeText(ReportFaultGPSActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void reverseGeocode(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                tvAddress.setText(fullAddress);
                reportData.setLocationAddress(fullAddress);
                reportData.setLatitude(lat);
                reportData.setLongitude(lng);
            } else {
                tvAddress.setText("Unknown location");
            }
        } catch (IOException e) {
            e.printStackTrace();
            tvAddress.setText("Reverse geocoding failed");
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

    private void validateAndSetAddress(String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                tvAddress.setText(fullAddress);
                reportData.setLocationAddress(fullAddress);
                reportData.setLatitude(address.getLatitude());
                reportData.setLongitude(address.getLongitude());
                Toast.makeText(this, "Address validated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Geocoding service failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void geocodeAddressForLocation(String address) {
        if (address == null || address.isEmpty()) {
            Toast.makeText(this, "Address is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                reportData.setLatitude(addr.getLatitude());
                reportData.setLongitude(addr.getLongitude());
                startNextActivity(ProximityCheckActivity.class);
            } else {
                Toast.makeText(this, "Could not get coordinates, but continuing", Toast.LENGTH_SHORT).show();
                startNextActivity(ProximityCheckActivity.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Geocoding error, but continuing", Toast.LENGTH_SHORT).show();
            startNextActivity(ProximityCheckActivity.class);
        }
    }

    private void openMapPicker(String address) {
        if (address == null || address.isEmpty()) {
            Toast.makeText(this, "Please enter an address first", Toast.LENGTH_SHORT).show();
            return;
        }

        String encodedAddress = Uri.encode(address);
        Uri geoUri = Uri.parse("geo:0,0?q=" + encodedAddress);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
            return;
        }

        String webUri = "https://www.google.com/maps/search/?api=1&query=" + encodedAddress;
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
        try {
            startActivity(webIntent);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Location")
                    .setMessage("Location: " + address + "\n\nNo map app found. Continue?")
                    .setPositiveButton("Continue", (dialog, which) -> {
                        reportData.setLocationAddress(address);
                        tvAddress.setText(address);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
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