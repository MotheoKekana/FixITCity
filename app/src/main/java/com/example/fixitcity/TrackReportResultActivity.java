package com.example.fixitcity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;

import java.util.ArrayList;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class TrackReportResultActivity extends AppCompatActivity {

    private static final String TAG = "TrackReportResult";

    private RecyclerView rvStatusHistory;
    private TextView tvHistoryEmpty;
    private StatusHistoryAdapter historyAdapter;
    private final List<StatusHistoryItem> historyItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_report_result);

        ScreenInsets.apply(this);

        hideNavForGuests();

        // ---------- Intent extras ----------
        long   reportId      = getIntent().getLongExtra("report_id", -1);
        String reference     = getIntent().getStringExtra("reference_number");
        String title         = getIntent().getStringExtra("title");
        String description   = getIntent().getStringExtra("description");
        String addressLine   = getIntent().getStringExtra("address_line");
        String status        = getIntent().getStringExtra("status");
        int    priorityScore = getIntent().getIntExtra("priority_score", 0);
        String createdAt     = getIntent().getStringExtra("created_at");

        // ---------- Report detail card ----------
        setTextSafe(R.id.tv_report_title,
                title != null && !title.isEmpty() ? title : "Report");

        setTextSafe(R.id.tv_report_location,
                (addressLine != null && !addressLine.isEmpty()
                        ? addressLine : "Unknown location"));

        setTextSafe(R.id.tv_report_date,
                "Reported: " + (createdAt != null ? createdAt : "—"));

        setTextSafe(R.id.tv_current_report_reference,
                "Reference: " + (reference != null ? reference : "Unavailable"));
        setTextSafe(R.id.tv_current_report_status,
                "Status: " + (status != null ? status : "Unavailable"));
        setTextSafe(R.id.tv_report_priority, getIntent().hasExtra("priority_score")
                ? "Priority Score: " + priorityScore + "/100" : "Priority Score: Not available yet");
        if (new SessionManager(this).isGuest()) {
            findViewById(R.id.tv_report_location).setVisibility(View.GONE);
        }

        TextView tvDesc = findViewById(R.id.tv_report_description);
        if (tvDesc != null) {
            if (description != null && !description.isEmpty()) {
                tvDesc.setText(description);
                tvDesc.setVisibility(View.VISIBLE);
            } else {
                tvDesc.setVisibility(View.GONE);
            }
        }

        // ---------- Share button ----------
        View btnShare = findViewById(R.id.btn_share_status);
        if (btnShare != null) {
            final String shareRef    = reference;
            final String shareStatus = status;
            final String shareTitle  = title;
            btnShare.setOnClickListener(v -> {
                String msg = "FixITCity Report\n"
                        + "Reference: " + (shareRef != null ? shareRef : "—") + "\n"
                        + "Title: " + (shareTitle != null ? shareTitle : "Report") + "\n"
                        + "Status: " + (shareStatus != null ? shareStatus : "Pending");
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, msg);
                startActivity(Intent.createChooser(shareIntent, "Share report status"));
            });
        }

        // Open the real notifications screen; alert delivery is managed server-side.
        View btnNotifications = findViewById(R.id.btn_view_notifications);
        if (btnNotifications != null) {
            if (new SessionManager(this).isGuest()) {
                btnNotifications.setVisibility(View.GONE);
            } else {
                btnNotifications.setOnClickListener(v ->
                        startActivity(new Intent(this, Notifications_Activity.class)));
            }
        }

        // ---------- Status history timeline ----------
        rvStatusHistory = findViewById(R.id.rv_status_history);
        tvHistoryEmpty  = findViewById(R.id.tv_history_empty);

        historyAdapter = new StatusHistoryAdapter(historyItems);

        if (rvStatusHistory != null) {
            rvStatusHistory.setLayoutManager(new LinearLayoutManager(this));
            rvStatusHistory.setNestedScrollingEnabled(false);
            rvStatusHistory.setAdapter(historyAdapter);
        }

        // Show loading state while we fetch
        if (tvHistoryEmpty != null) {
            tvHistoryEmpty.setText("Loading history…");
            tvHistoryEmpty.setVisibility(View.VISIBLE);
        }
        if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.GONE);

        boolean externalGuest = new SessionManager(this).isGuest();
        boolean summaryOnly = getIntent().getBooleanExtra("summary_only", false);
        if (getIntent().getBooleanExtra("pending_sync", false)) {
            showHistoryEmpty("Saved on this phone. Keep this guest session open until syncing finishes, then search the reference again.");
        } else if (externalGuest || summaryOnly) {
            showHistoryEmpty("Current status is shown above. Detailed history is available with a citizen account for your own reports.");
        } else if (reportId > 0) {
            loadStatusHistory(reportId);
            loadReportPhotos(reportId);
        } else {
            showHistoryEmpty("No status history available.");
        }

        // ---------- Bottom navigation ----------
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);
                Class<?> target = session.isGuest()
                        ? Guest_Dashboard_Activity.class
                        : Dashboard_Activity.class;
                Intent intent = new Intent(this, target);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View navReport = findViewById(R.id.navReport);
        if (navReport != null) {
            navReport.setOnClickListener(v ->
                    startActivity(new Intent(this, ReportFaultCategoryActivity.class)));
        }

        View navTrack = findViewById(R.id.navTrack);
        if (navTrack != null) {
            navTrack.setOnClickListener(v ->
                    Toast.makeText(this, "You're on Track", Toast.LENGTH_SHORT).show());
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileSettingsActivity.class)));
        }

        AppNavigation.bindNotificationBell(this);
    }

    private void loadReportPhotos(long reportId) {
        TextView label = findViewById(R.id.tv_report_photos);
        LinearLayout strip = findViewById(R.id.report_photo_strip);
        String token = new SessionManager(this).getAccessToken();
        if (token == null || token.isEmpty()) return;
        label.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                List<String> urls = SupabaseReports.getPhotoUrls(reportId, token);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (urls.isEmpty()) { label.setVisibility(View.GONE); return; }
                    label.setText("Report photos (" + urls.size() + ")");
                });
                for (int i = 0; i < urls.size(); i++) {
                    String url = urls.get(i);
                    if (!url.startsWith("https://")) continue;
                    try {
                        Bitmap bitmap = downloadPhoto(url);
                        int position = i;
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            ImageView thumb = new ImageView(this);
                            int size = Math.round(96 * getResources().getDisplayMetrics().density);
                            int gap = Math.round(8 * getResources().getDisplayMetrics().density);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                            params.setMargins(0, 0, gap, 0);
                            thumb.setLayoutParams(params);
                            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            thumb.setContentDescription("Report photo " + (position + 1));
                            thumb.setImageBitmap(bitmap);
                            thumb.setOnClickListener(v -> {
                                ImageView full = new ImageView(this);
                                full.setImageBitmap(bitmap);
                                full.setAdjustViewBounds(true);
                                new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setView(full).setPositiveButton("Close", null).show();
                            });
                            strip.addView(thumb);
                        });
                    } catch (Exception imageError) {
                        Log.w(TAG, "Could not load report photo", imageError);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not load report photo list", e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) label.setText("Photos unavailable");
                });
            }
        }, "load-report-photos").start();
    }

    private Bitmap downloadPhoto(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        try {
            if (connection.getResponseCode() != 200) throw new java.io.IOException("Photo unavailable");
            try (InputStream in = connection.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (out.size() + read > 8 * 1024 * 1024) throw new java.io.IOException("Photo too large");
                    out.write(buffer, 0, read);
                }
                byte[] bytes = out.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                options.inJustDecodeBounds = false;
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 1600
                        || options.outHeight / options.inSampleSize > 1600) options.inSampleSize *= 2;
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                if (bitmap == null) throw new java.io.IOException("Invalid photo");
                return bitmap;
            }
        } finally {
            connection.disconnect();
        }
    }

    /** Fetch the report's status history and show a distinct error if it fails. */
    private void loadStatusHistory(long reportId) {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        new Thread(() -> {
            List<StatusHistoryItem> fetched = new ArrayList<>();
            boolean loadFailed = false;
            try {
                List<SupabaseReports.StatusHistoryDTO> dtos =
                        SupabaseReports.getStatusHistory(reportId, accessToken);

                if (dtos != null) {
                    for (SupabaseReports.StatusHistoryDTO dto : dtos) {
                        fetched.add(new StatusHistoryItem(
                                dto.old_status,
                                dto.new_status,
                                dto.changed_at,
                                dto.notes
                        ));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load status history", e);
                loadFailed = true;
            }

            // Sort newest first
            fetched.sort((a, b) -> {
                String da = a.changedAt != null ? a.changedAt : "";
                String db = b.changedAt != null ? b.changedAt : "";
                return db.compareTo(da);
            });

            final List<StatusHistoryItem> finalList = fetched;
            final boolean failedFinal = loadFailed;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                historyItems.clear();
                historyItems.addAll(finalList);
                historyAdapter.notifyDataSetChanged();

                if (failedFinal) {
                    showHistoryEmpty("Could not load status history. Check your connection and reopen this report.");
                } else if (historyItems.isEmpty()) {
                    showHistoryEmpty("No status changes yet.");
                } else {
                    if (tvHistoryEmpty != null) tvHistoryEmpty.setVisibility(View.GONE);
                    if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void showHistoryEmpty(String message) {
        if (tvHistoryEmpty != null) {
            tvHistoryEmpty.setText(message);
            tvHistoryEmpty.setVisibility(View.VISIBLE);
        }
        if (rvStatusHistory != null) rvStatusHistory.setVisibility(View.GONE);
    }

    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    private void setTextSafe(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(text);
    }
}
