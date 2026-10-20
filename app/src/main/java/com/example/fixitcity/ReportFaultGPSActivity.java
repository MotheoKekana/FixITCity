package com.example.fixitcity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.fixitcity.data.SessionManager;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;
import java.util.Locale;

public class ReportFaultGPSActivity extends AppCompatActivity {
    private static final String TAG = "ReportGPS";
    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private static final long LOCATION_DURATION_MS = 20000L;
    private static final long LOCATION_DEADLINE_MS = 25000L;
    private enum Action { RETRY, APP_SETTINGS, LOCATION_SETTINGS }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView tvAddress, tvLocationState, tvGpsBadge;
    private TextView previewCoordinates, previewCaption;
    private ImageView previewIllustration;
    private CircularProgressIndicator previewProgress;
    private MaterialButton nextButton, locationAction;
    private ReportData reportData;
    private String draftOwner;
    private long draftCategoryId;
    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource cancellation;
    private Runnable deadline;
    private int generation;
    private boolean resumed, capturing, captured, permissionRequested, permissionPrompt;
    private Action action = Action.RETRY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || (session.isGuest() && !session.hasGuestAuthSession())) {
            AppNavigation.goHome(this);
            return;
        }
        reportData = ((FixITCityApp) getApplication()).getReportData();
        draftOwner = session.getUserId();
        draftCategoryId = reportData.getCategoryId();
        if (reportData.getCategoryId() <= 0) {
            Toast.makeText(this, "Please select a fault category to start your report.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, ReportFaultCategoryActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_report_fault_gps);
        ScreenInsets.apply(this);
        if (session.isGuest()) findViewById(R.id.bottomNav).setVisibility(View.GONE);
        AppNavigation.bindNotificationBell(this);
        tvAddress = findViewById(R.id.tv_location_address);
        tvLocationState = findViewById(R.id.tv_location_state);
        tvGpsBadge = findViewById(R.id.tv_gps_badge);
        previewCoordinates = findViewById(R.id.tv_gps_preview_coordinates);
        previewCaption = findViewById(R.id.tv_gps_preview_caption);
        previewIllustration = findViewById(R.id.iv_gps_preview);
        previewProgress = findViewById(R.id.gps_preview_progress);
        nextButton = findViewById(R.id.btn_next);
        locationAction = findViewById(R.id.btn_location_action);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        permissionRequested = savedInstanceState != null
                && savedInstanceState.getBoolean("gps_permission_requested", false);
        captured = savedInstanceState != null
                && savedInstanceState.getBoolean("gps_captured", false)
                && savedInstanceState.getLong("gps_category_id", -1) == reportData.getCategoryId()
                && draftOwner != null && draftOwner.equals(savedInstanceState.getString("gps_owner"));
        if (captured) {
            reportData.setLatitude(savedInstanceState.getDouble("gps_lat"));
            reportData.setLongitude(savedInstanceState.getDouble("gps_lng"));
            reportData.setLocationAddress(savedInstanceState.getString("gps_address"));
            captured = validCoordinates(reportData.getLatitude(), reportData.getLongitude())
                    && reportData.getLocationAddress() != null;
        }
        if (captured) {
            tvAddress.setText(reportData.getLocationAddress());
            showState("Location captured. Continue, or retry to get a new reading.", "Retry location", Action.RETRY);
        } else {
            clearCapturedLocation();
            tvAddress.setText("Finding your location…");
            nextButton.setEnabled(false);
        }
        locationAction.setOnClickListener(v -> {
            if (capturing) return;
            if (action == Action.APP_SETTINGS) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } else if (action == Action.LOCATION_SETTINGS) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else {
                prepareLocation();
            }
        });
        nextButton.setOnClickListener(v -> {
            if (!captured || !sameDraft()) return;
            nextButton.setEnabled(false);
            startActivity(new Intent(this, new SessionManager(this).isGuest()
                    ? ReportFaultPhotoActivity.class : ProximityCheckActivity.class));
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

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing() || reportData == null) return;
        if (!sameDraft()) {
            AppNavigation.goHome(this);
            return;
        }
        resumed = true;
        nextButton.setEnabled(captured);
        if (!captured) prepareLocation();
    }

    @Override
    protected void onPause() {
        resumed = false;
        generation++;
        cancelLocationRequest();
        super.onPause();
    }

    private boolean sameDraft() {
        SessionManager current = new SessionManager(this);
        return current.isLoggedIn() && draftOwner != null && draftOwner.equals(current.getUserId())
                && ((FixITCityApp) getApplication()).getReportData() == reportData
                && reportData.getCategoryId() == draftCategoryId;
    }

    private boolean currentRequest(int request) {
        return resumed && !isFinishing() && !isDestroyed() && request == generation && sameDraft();
    }

    private boolean hasPrecisePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void prepareLocation() {
        if (!resumed || capturing || permissionPrompt) return;
        if (!hasPrecisePermission()) {
            if (!permissionRequested) {
                permissionRequested = true;
                permissionPrompt = true;
                tvAddress.setText("Location permission needed");
                tvLocationState.setText("Allow precise location to place this fault accurately.");
                tvGpsBadge.setText("Permission needed");
                locationAction.setEnabled(false);
                // Android 12+ expects coarse and fine to be requested together.
                ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION }, LOCATION_PERMISSION_REQUEST);
            } else {
                tvAddress.setText("Location permission needed");
                showState("Allow precise location in app settings to place this fault accurately.",
                        "Open app settings", Action.APP_SETTINGS);
            }
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null || !manager.isLocationEnabled()) {
            tvAddress.setText("Phone location is switched off");
            showState("Turn on your phone's location, then return to FixITCity.",
                    "Open location settings", Action.LOCATION_SETTINGS);
            return;
        }
        requestLocation();
    }

    @SuppressLint("MissingPermission")
    private void requestLocation() {
        if (!hasPrecisePermission() || capturing || !resumed) return;
        cancelLocationRequest();
        int request = ++generation;
        clearCapturedLocation();
        capturing = true;
        tvAddress.setText("Finding your location…");
        tvGpsBadge.setText("Finding location…");
        tvLocationState.setText("This can take up to 25 seconds. Move near a window or outdoors if needed.");
        nextButton.setEnabled(false);
        locationAction.setEnabled(false);
        updateGpsPreview();
        cancellation = new CancellationTokenSource();
        deadline = () -> {
            if (!currentRequest(request) || !capturing) return;
            generation++;
            cancelLocationRequest();
            tvAddress.setText("Location unavailable");
            showState("Could not get a location yet. Move to a clearer area and retry.",
                    "Retry location", Action.RETRY);
        };
        handler.postDelayed(deadline, LOCATION_DEADLINE_MS);
        CurrentLocationRequest locationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(15000L)
                .setDurationMillis(LOCATION_DURATION_MS)
                .build();
        try {
            fusedLocationClient.getCurrentLocation(locationRequest, cancellation.getToken())
                    .addOnSuccessListener(location -> {
                        if (!currentRequest(request)) return;
                        cancelLocationRequest();
                        if (location == null || !validCoordinates(location.getLatitude(), location.getLongitude())) {
                            tvAddress.setText("Location unavailable");
                            showState("Could not get a location yet. Move to a clearer area and retry.",
                                    "Retry location", Action.RETRY);
                            return;
                        }
                        acceptLocation(location, request);
                    })
                    .addOnFailureListener(error -> {
                        if (!currentRequest(request)) return;
                        cancelLocationRequest();
                        Log.w(TAG, "Location request failed", error);
                        tvAddress.setText("Location unavailable");
                        showState("Check your location permission and phone location settings, then retry.",
                                "Retry location", Action.RETRY);
                    });
        } catch (RuntimeException error) {
            cancelLocationRequest();
            Log.w(TAG, "Could not start location request", error);
            tvAddress.setText("Location unavailable");
            showState("Could not start location. Check your phone's settings and retry.",
                    "Retry location", Action.RETRY);
        }
    }

    private boolean validCoordinates(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng)
                && Math.abs(lat) <= 90 && Math.abs(lng) <= 180;
    }

    private void acceptLocation(Location location, int request) {
        double lat = location.getLatitude(), lng = location.getLongitude();
        String coordinates = String.format(Locale.US, "%.6f, %.6f", lat, lng);
        captured = true;
        reportData.setLatitude(lat);
        reportData.setLongitude(lng);
        reportData.setLocationAddress(coordinates);
        tvAddress.setText(coordinates);
        String accuracy = location.hasAccuracy()
                ? String.format(Locale.getDefault(), "Estimated accuracy: about %.0f m. ", location.getAccuracy()) : "";
        showState(accuracy + "Location captured. You can continue.",
                "Retry location", Action.RETRY);
        nextButton.setEnabled(true);
        reverseGeocode(lat, lng, request);
    }

    private void reverseGeocode(double lat, double lng, int request) {
        if (!Geocoder.isPresent()) return;
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                geocoder.getFromLocation(lat, lng, 1, new Geocoder.GeocodeListener() {
                    @Override
                    public void onGeocode(@NonNull List<Address> addresses) {
                        applyAddress(addresses, request);
                    }
                    @Override
                    public void onError(String message) {
                        // Keep usable coordinates when address lookup is unavailable.
                    }
                });
            } catch (RuntimeException error) {
                Log.d(TAG, "Address unavailable; keeping coordinates");
            }
        } else {
            new Thread(() -> {
                try {
                    applyAddress(geocoder.getFromLocation(lat, lng, 1), request);
                } catch (Exception error) {
                    Log.d(TAG, "Address unavailable; keeping coordinates");
                }
            }, "report-address-lookup").start();
        }
    }

    private void applyAddress(List<Address> addresses, int request) {
        if (addresses == null || addresses.isEmpty()) return;
        String address = addresses.get(0).getAddressLine(0);
        if (address == null || address.trim().isEmpty()) return;
        runOnUiThread(() -> {
            if (!currentRequest(request) || !captured) return;
            tvAddress.setText(address);
            reportData.setLocationAddress(address);
        });
    }

    private void clearCapturedLocation() {
        captured = false;
        reportData.setLocationAddress(null);
        reportData.setLatitude(0);
        reportData.setLongitude(0);
        updateGpsPreview();
    }

    private void showState(String text, String buttonText, Action nextAction) {
        tvLocationState.setText(text);
        tvGpsBadge.setText(captured ? "Location captured"
                : nextAction == Action.APP_SETTINGS ? "Permission needed"
                  : nextAction == Action.LOCATION_SETTINGS ? "Location off" : "Location unavailable");
        action = nextAction;
        locationAction.setText(buttonText);
        locationAction.setEnabled(true);
        updateGpsPreview();
    }

    private void updateGpsPreview() {
        if (previewCoordinates == null) return;
        previewProgress.setVisibility(capturing ? View.VISIBLE : View.GONE);
        previewIllustration.setAlpha(captured ? 1f : capturing ? 0.85f : 0.45f);
        tvGpsBadge.setTextColor(ContextCompat.getColor(this,
                captured ? R.color.primary_green : R.color.secondary_blue));
        if (captured) {
            previewCoordinates.setText(String.format(Locale.US, "%.5f  •  %.5f",
                    reportData.getLatitude(), reportData.getLongitude()));
            previewCaption.setText("Coordinates captured from your phone");
        } else {
            previewCoordinates.setText(capturing ? "Finding your position…" : "Waiting for GPS…");
            previewCaption.setText(capturing ? "Getting a precise location reading"
                    : "Location not captured yet");
        }
    }

    private void cancelLocationRequest() {
        if (deadline != null) handler.removeCallbacks(deadline);
        deadline = null;
        if (cancellation != null) cancellation.cancel();
        cancellation = null;
        capturing = false;
        updateGpsPreview();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            permissionPrompt = false;
            if (resumed) prepareLocation();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("gps_permission_requested", permissionRequested);
        outState.putBoolean("gps_captured", captured);
        if (reportData != null) {
            outState.putLong("gps_category_id", reportData.getCategoryId());
            outState.putString("gps_owner", draftOwner);
            outState.putDouble("gps_lat", reportData.getLatitude());
            outState.putDouble("gps_lng", reportData.getLongitude());
            outState.putString("gps_address", reportData.getLocationAddress());
        }
    }

    @Override
    protected void onDestroy() {
        cancelLocationRequest();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
