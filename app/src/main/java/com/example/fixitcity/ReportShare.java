package com.example.fixitcity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Generates a report card locally and opens Android's share chooser. */
public final class ReportShare {
    private static final String TAG = "ReportShare";
    private static final long CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;

    private ReportShare() { }

    public static void share(Activity activity, View button, ReportShareData data) {
        if (!data.hasReference()) {
            Toast.makeText(activity, "No report reference is available to share.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!button.isEnabled()) return;
        button.setEnabled(false);

        final Bitmap bitmap;
        try {
            // Inflating/measuring Android views stays on the UI thread.
            bitmap = render(activity, data);
        } catch (RuntimeException | OutOfMemoryError error) {
            button.setEnabled(true);
            shareText(activity, data);
            return;
        }

        Context app = activity.getApplicationContext();
        new Thread(() -> {
            Uri image = null;
            try {
                File directory = new File(app.getCacheDir(), "report_shares");
                if (!directory.isDirectory() && !directory.mkdirs()) {
                    throw new IOException("Could not create the share directory");
                }
                removeExpiredCards(directory);
                String safeReference = data.reference.replaceAll("[^A-Za-z0-9_-]", "_");
                File file = File.createTempFile("fixitcity-" + safeReference + "-", ".png", directory);
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw new IOException("Could not create the report image");
                    }
                } catch (IOException failure) {
                    // Only this newly created incomplete file is removed.
                    if (!file.delete()) Log.d(TAG, "Incomplete card retained in temporary cache");
                    throw failure;
                }
                image = FileProvider.getUriForFile(app, app.getPackageName() + ".fileprovider", file);
            } catch (IOException | RuntimeException failure) {
                Log.w(TAG, "Could not prepare report card (" + failure.getClass().getSimpleName() + ")");
            } finally {
                bitmap.recycle();
            }

            final Uri imageUri = image;
            activity.runOnUiThread(() -> {
                button.setEnabled(true);
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (imageUri == null) {
                    shareText(activity, data);
                    return;
                }
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/png");
                share.putExtra(Intent.EXTRA_STREAM, imageUri);
                share.putExtra(Intent.EXTRA_TEXT, data.caption());
                share.putExtra(Intent.EXTRA_TITLE, "FixITCity report: " + data.reference);
                share.setClipData(ClipData.newRawUri("FixITCity report card", imageUri));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    Intent chooser = Intent.createChooser(share, "Share report card");
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    activity.startActivity(chooser);
                } catch (RuntimeException unavailable) {
                    shareText(activity, data);
                }
            });
        }, "report-share").start();
    }

    private static Bitmap render(Activity activity, ReportShareData data) {
        // A fixed export palette/density makes the image consistent in both app
        // themes and independent of the sender's font-size/display-size settings.
        Configuration configuration = new Configuration(activity.getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | Configuration.UI_MODE_NIGHT_NO;
        configuration.densityDpi = DisplayMetrics.DENSITY_DEFAULT;
        configuration.fontScale = 1.0f;
        Context context = new ContextThemeWrapper(
                activity.createConfigurationContext(configuration), R.style.Theme_FixITCity);
        View card = LayoutInflater.from(context).inflate(R.layout.view_share_report_card, null, false);
        ((TextView) card.findViewById(R.id.share_report_title)).setText(data.title);
        TextView status = card.findViewById(R.id.share_report_status);
        status.setText(data.status);
        ReportStatusColors.apply(status, data.status);
        ((TextView) card.findViewById(R.id.share_report_reference)).setText(data.reference);
        TextView priority = card.findViewById(R.id.share_report_priority);
        if (data.priority == null) priority.setVisibility(View.GONE);
        else priority.setText("Priority score: " + data.priority + "/100");
        ((TextView) card.findViewById(R.id.share_report_instructions)).setText(data.instructions());
        ((TextView) card.findViewById(R.id.share_report_timestamp)).setText("Shared " + data.sharedAt);

        card.measure(View.MeasureSpec.makeMeasureSpec(540, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        card.layout(0, 0, card.getMeasuredWidth(), card.getMeasuredHeight());
        int border = 16;
        int scale = 2;
        Bitmap image = Bitmap.createBitmap((card.getMeasuredWidth() + 2 * border) * scale,
                (card.getMeasuredHeight() + 2 * border) * scale, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(image);
            canvas.drawColor(context.getColor(R.color.background_light));
            canvas.scale(scale, scale);
            canvas.translate(border, border);
            Path outline = new Path();
            outline.addRoundRect(new RectF(0, 0, card.getMeasuredWidth(), card.getMeasuredHeight()),
                    22, 22, Path.Direction.CW);
            canvas.clipPath(outline);
            card.draw(canvas);
            return image;
        } catch (RuntimeException | OutOfMemoryError failure) {
            image.recycle();
            throw failure;
        }
    }

    private static void shareText(Activity activity, ReportShareData data) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Toast.makeText(activity, "Could not prepare the image. Sharing report text instead.",
                Toast.LENGTH_SHORT).show();
        Intent text = new Intent(Intent.ACTION_SEND);
        text.setType("text/plain");
        text.putExtra(Intent.EXTRA_TEXT, data.caption());
        try {
            activity.startActivity(Intent.createChooser(text, "Share report"));
        } catch (RuntimeException unavailable) {
            Toast.makeText(activity, "No sharing app is available. You can copy the reference number.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static void removeExpiredCards(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - CACHE_MAX_AGE_MS;
        for (File file : files) {
            if (file.isFile() && file.getName().startsWith("fixitcity-")
                    && file.getName().endsWith(".png") && file.lastModified() < cutoff) {
                if (!file.delete()) Log.d(TAG, "Expired card retained in temporary cache");
            }
        }
    }
}
