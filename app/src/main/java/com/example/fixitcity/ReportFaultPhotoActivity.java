package com.example.fixitcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fixitcity.data.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ReportFaultPhotoActivity extends AppCompatActivity {

    private static final String TAG = "ReportFaultPhoto";
    private ReportData reportData;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;
    private static final int REQUEST_PERMISSION_CAMERA = 100;
    private Uri photoUri;

    private ImageView ivPhotoPreview;
    private ImageView ivCameraIcon;
    private TextView tvTapHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_fault_photo);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        hideNavForGuests();

        reportData = ((FixITCityApp) getApplication()).getReportData();

        ivPhotoPreview = findViewById(R.id.iv_photo_preview);
        ivCameraIcon = findViewById(R.id.iv_camera_icon);
        tvTapHint = findViewById(R.id.tv_tap_hint);

        if (reportData.getPhotoUri() != null && !reportData.getPhotoUri().isEmpty()) {
            showPreviewFromUri(Uri.parse(reportData.getPhotoUri()));
        }

        findViewById(R.id.camera_placeholder).setOnClickListener(v -> {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent();
            } else {
                requestCameraPermission();
            }
        });

        findViewById(R.id.btn_gallery).setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openGallery();
            } else {
                requestStoragePermission();
            }
        });

        findViewById(R.id.btn_retake).setOnClickListener(v -> {
            photoUri = null;
            reportData.setPhotoUri(null);
            reportData.setPhotoBytes(null);
            reportData.setPhotoMimeType(null);
            reportData.setPhotoCount(0);
            clearPreview();
            Toast.makeText(this, "Photo cleared", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_skip).setOnClickListener(v -> {
            reportData.setPhotoCount(0);
            reportData.setPhotoUri(null);
            reportData.setPhotoBytes(null);
            reportData.setPhotoMimeType(null);
            startNextActivity(ReportFaultSummaryActivity.class);
        });

        findViewById(R.id.btn_continue).setOnClickListener(v -> {
            startNextActivity(ReportFaultSummaryActivity.class);
        });

        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);
                Class<?> target = session.isGuest()
                        ? Guest_Dashboard_Activity.class
                        : Dashboard_Activity.class;
                startActivity(new Intent(this, target));
                finish();
            });
        }

        View navReport = findViewById(R.id.navReport);
        if (navReport != null) {
            navReport.setOnClickListener(v -> {
                startActivity(new Intent(this, ReportFaultCategoryActivity.class));
                finish();
            });
        }

        View navTrack = findViewById(R.id.navTrack);
        if (navTrack != null) {
            navTrack.setOnClickListener(v ->
                    startActivity(new Intent(this, TrackReportSearchActivity.class)));
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileSettingsActivity.class)));
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_IMAGE_GALLERY);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        "com.example.fixitcity.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_IMAGE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (photoUri != null) {
                    reportData.setPhotoUri(photoUri.toString());
                    reportData.setPhotoCount(1);
                    loadBytesFromUri(photoUri);
                    showPreviewFromUri(photoUri);
                    Toast.makeText(this, "Photo taken", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMAGE_GALLERY && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    reportData.setPhotoUri(selectedImage.toString());
                    reportData.setPhotoCount(1);
                    loadBytesFromUri(selectedImage);
                    showPreviewFromUri(selectedImage);
                    Toast.makeText(this, "Photo selected", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showPreviewFromUri(Uri uri) {
        if (ivPhotoPreview == null) return;
        try {
            ivPhotoPreview.setImageURI(uri);
            ivPhotoPreview.setVisibility(View.VISIBLE);
            if (ivCameraIcon != null) ivCameraIcon.setVisibility(View.GONE);
            if (tvTapHint != null) tvTapHint.setText("Tap to retake");
        } catch (Exception e) {
            Log.e(TAG, "showPreviewFromUri failed: " + e.getMessage(), e);
        }
    }

    private void clearPreview() {
        if (ivPhotoPreview != null) {
            ivPhotoPreview.setImageDrawable(null);
            ivPhotoPreview.setVisibility(View.GONE);
        }
        if (ivCameraIcon != null) ivCameraIcon.setVisibility(View.VISIBLE);
        if (tvTapHint != null) tvTapHint.setText("Tap to take photo");
    }

    private void loadBytesFromUri(Uri uri) {
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap original = BitmapFactory.decodeStream(is);
                if (is != null) is.close();

                if (original == null) {
                    Log.e(TAG, "Could not decode bitmap from " + uri);
                    return;
                }

                int maxDim = 1200;
                int w = original.getWidth();
                int h = original.getHeight();
                if (w > maxDim || h > maxDim) {
                    float scale = Math.min((float) maxDim / w, (float) maxDim / h);
                    Bitmap scaled = Bitmap.createScaledBitmap(
                            original,
                            Math.round(w * scale),
                            Math.round(h * scale),
                            true);
                    original.recycle();
                    original = scaled;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                original.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] bytes = baos.toByteArray();
                original.recycle();

                reportData.setPhotoBytes(bytes);
                reportData.setPhotoMimeType("image/jpeg");

                Log.d(TAG, "Loaded " + bytes.length + " bytes from " + uri);

            } catch (Exception e) {
                Log.e(TAG, "loadBytesFromUri failed: " + e.getMessage(), e);
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    private void startNextActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }
}