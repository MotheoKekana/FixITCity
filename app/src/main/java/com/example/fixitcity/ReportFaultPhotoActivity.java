package com.example.fixitcity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private View continueButton;
    private View cameraButton;
    private View galleryButton;
    private View removeButton;
    private TextView photoCountLabel;
    private LinearLayout photoStrip;
    private boolean processingPhoto;

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
        AppNavigation.bindNotificationBell(this);

        reportData = ((FixITCityApp) getApplication()).getReportData();

        ivPhotoPreview = findViewById(R.id.iv_photo_preview);
        ivCameraIcon = findViewById(R.id.iv_camera_icon);
        tvTapHint = findViewById(R.id.tv_tap_hint);
        continueButton = findViewById(R.id.btn_continue);
        cameraButton = findViewById(R.id.camera_placeholder);
        galleryButton = findViewById(R.id.btn_gallery);
        removeButton = findViewById(R.id.btn_retake);
        photoCountLabel = findViewById(R.id.tv_photo_count);
        photoStrip = findViewById(R.id.photo_strip);

        updatePhotos();

        cameraButton.setOnClickListener(v -> {
            if (processingPhoto || reportData.getPhotoCount() >= 3) return;
            if (checkCameraPermission()) {
                dispatchTakePictureIntent();
            } else {
                requestCameraPermission();
            }
        });

        galleryButton.setOnClickListener(v -> {
            if (processingPhoto || reportData.getPhotoCount() >= 3) return;
            if (checkStoragePermission()) {
                openGallery();
            } else {
                requestStoragePermission();
            }
        });

        removeButton.setOnClickListener(v -> {
            if (processingPhoto) return;
            reportData.removeLastPhoto();
            updatePhotos();
        });

        findViewById(R.id.btn_skip).setOnClickListener(v -> {
            if (processingPhoto) return;
            reportData.clearPhotos();
            if (!processingPhoto) startNextActivity(ReportFaultSummaryActivity.class);
        });

        continueButton.setOnClickListener(v -> {
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
                    selectPhoto(photoUri);
                }
            } else if (requestCode == REQUEST_IMAGE_GALLERY && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    photoUri = selectedImage;
                    selectPhoto(selectedImage);
                }
            }
        }
    }

    private void selectPhoto(Uri uri) {
        if (processingPhoto || reportData.getPhotoCount() >= 3) return;
        processingPhoto = true;
        reportData.setPhotoUri(uri.toString());
        updatePhotos();
        if (tvTapHint != null) tvTapHint.setText("Processing photo…");
        loadBytesFromUri(uri);
    }

    private void updatePhotos() {
        int count = reportData.getPhotoCount();
        if (count > 0) showPreviewFromBytes(reportData.getPhotos().get(count - 1));
        else clearPreview();
        photoCountLabel.setText(count + " of 3 photos added");
        continueButton.setEnabled(!processingPhoto);
        cameraButton.setEnabled(!processingPhoto && count < 3);
        galleryButton.setEnabled(!processingPhoto && count < 3);
        removeButton.setEnabled(!processingPhoto && count > 0);
        photoStrip.removeAllViews();
        for (int i = 0; i < count; i++) {
            byte[] photo = reportData.getPhotos().get(i);
            ImageView thumbnail = new ImageView(this);
            int side = Math.round(68 * getResources().getDisplayMetrics().density);
            int margin = Math.round(8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(side, side);
            params.setMargins(0, 0, margin, 0);
            thumbnail.setLayoutParams(params);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setContentDescription("Photo " + (i + 1) + " of " + count);
            thumbnail.setImageBitmap(BitmapFactory.decodeByteArray(photo, 0, photo.length));
            photoStrip.addView(thumbnail);
        }
        if (count == 3 && tvTapHint != null) tvTapHint.setText("Maximum 3 photos added");
    }

    private void showPreviewFromBytes(byte[] bytes) {
        if (ivPhotoPreview == null) return;
        Bitmap preview = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (preview == null) return;
        ivPhotoPreview.setImageBitmap(preview);
        ivPhotoPreview.setVisibility(View.VISIBLE);
        if (ivCameraIcon != null) ivCameraIcon.setVisibility(View.GONE);
        if (tvTapHint != null) tvTapHint.setText("Tap to add another photo");
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
                int orientation = ExifInterface.ORIENTATION_NORMAL;
                // The platform InputStream constructor is available from API 24.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try (InputStream exifStream = getContentResolver().openInputStream(uri)) {
                        if (exifStream == null) throw new IOException("Photo cannot be opened");
                        ExifInterface exif = new ExifInterface(exifStream);
                        orientation = exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    } catch (IOException exifError) {
                        // Files without readable EXIF can still be decoded normally.
                        Log.w(TAG, "Photo orientation unavailable", exifError);
                    }
                }

                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream sizeStream = getContentResolver().openInputStream(uri)) {
                    if (sizeStream == null) throw new IOException("Photo cannot be opened");
                    BitmapFactory.decodeStream(sizeStream, null, bounds);
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                while (bounds.outWidth / options.inSampleSize > 2400
                        || bounds.outHeight / options.inSampleSize > 2400) {
                    options.inSampleSize *= 2;
                }

                Bitmap bitmap;
                try (InputStream imageStream = getContentResolver().openInputStream(uri)) {
                    if (imageStream == null) throw new IOException("Photo cannot be opened");
                    bitmap = BitmapFactory.decodeStream(imageStream, null, options);
                }
                if (bitmap == null) throw new IOException("Photo could not be decoded");

                Matrix transform = orientationMatrix(orientation);
                if (transform != null) {
                    Bitmap corrected = Bitmap.createBitmap(bitmap, 0, 0,
                            bitmap.getWidth(), bitmap.getHeight(), transform, true);
                    if (corrected != bitmap) bitmap.recycle();
                    bitmap = corrected;
                }

                int maxDim = 1200;
                if (bitmap.getWidth() > maxDim || bitmap.getHeight() > maxDim) {
                    float scale = Math.min((float) maxDim / bitmap.getWidth(),
                            (float) maxDim / bitmap.getHeight());
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                            Math.max(1, Math.round(bitmap.getWidth() * scale)),
                            Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
                    if (scaled != bitmap) bitmap.recycle();
                    bitmap = scaled;
                }

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)) {
                    throw new IOException("Photo could not be saved as JPEG");
                }
                bitmap.recycle();
                byte[] bytes = output.toByteArray();
                if (bytes.length == 0) throw new IOException("Processed photo is empty");

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()
                            || !uri.toString().equals(reportData.getPhotoUri())) return;
                    reportData.addPhoto(bytes);
                    reportData.setPhotoMimeType("image/jpeg");
                    processingPhoto = false;
                    updatePhotos();
                });
            } catch (Exception e) {
                Log.e(TAG, "Photo processing failed", e);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()
                            || !uri.toString().equals(reportData.getPhotoUri())) return;
                    processingPhoto = false;
                    updatePhotos();
                    Toast.makeText(this, "Could not process this photo. Please try another one.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "process-report-photo").start();
    }

    private Matrix orientationMatrix(int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90); break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(270); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270); break;
            default:
                return null;
        }
        return matrix;
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
        } else if (requestCode == REQUEST_IMAGE_GALLERY) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Photo access is required to choose an image",
                        Toast.LENGTH_SHORT).show();
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
