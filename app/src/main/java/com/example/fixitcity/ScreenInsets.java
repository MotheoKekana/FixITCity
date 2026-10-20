package com.example.fixitcity;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Keeps each activity's content inside the system bars, cutouts and keyboard. */
public final class ScreenInsets {
    private ScreenInsets() { }

    /** Call after setContentView, in place of an activity's own inset listener. */
    @SuppressWarnings("deprecation")
    public static void apply(Activity activity) {
        Window window = activity.getWindow();
        // Use the same inset handling on Android 11+ and Android 15/16.
        WindowCompat.setDecorFitsSystemWindows(window, false);
        int inputMode = window.getAttributes().softInputMode;
        window.setSoftInputMode((inputMode & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        // The padded area also provides a matching backdrop on Android 15+,
        // where status/navigation bars can be transparent.
        int barColor = ContextCompat.getColor(activity, R.color.system_bar_background);
        content.setBackgroundColor(barColor);
        window.setStatusBarColor(barColor);
        window.setNavigationBarColor(barColor);
        boolean lightBars = activity.getResources().getBoolean(R.bool.light_system_bars);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                window, window.getDecorView());
        controller.setAppearanceLightStatusBars(lightBars);
        controller.setAppearanceLightNavigationBars(lightBars);

        // Repeated calls must never count already-applied padding as original padding.
        if (content.getTag(R.id.screen_insets_state) instanceof PaddingState) {
            ViewCompat.requestApplyInsets(content);
            return;
        }
        PaddingState state = new PaddingState(content);
        content.setTag(R.id.screen_insets_state, state);
        if (content instanceof ViewGroup) {
            ((ViewGroup) content).setClipToPadding(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            // A union takes the largest inset on each edge: don't add the navigation
            // bar height to the keyboard height, since they occupy the same space.
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            int left = state.left + safe.left;
            int top = state.top + safe.top;
            int right = state.right + safe.right;
            int bottom = state.bottom + safe.bottom;
            if (view.getPaddingLeft() != left || view.getPaddingTop() != top
                    || view.getPaddingRight() != right || view.getPaddingBottom() != bottom) {
                state.revealFocusedInput = insets.isVisible(WindowInsetsCompat.Type.ime());
                view.setPadding(left, top, right, bottom);
            }
            // Content has handled these insets. Descendants must not add them again.
            return WindowInsetsCompat.CONSUMED;
        });

        // Run after the smaller viewport has been laid out. ScrollView can then
        // bring the focused field (and a little space around it) above the IME.
        content.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!state.revealFocusedInput) return;
            state.revealFocusedInput = false;
            View focused = activity.getCurrentFocus();
            if (!(focused instanceof EditText) || !focused.isShown()) return;
            int gap = Math.round(12 * focused.getResources().getDisplayMetrics().density);
            Rect rectangle = new Rect(0, -gap, focused.getWidth(), focused.getHeight() + gap);
            focused.requestRectangleOnScreen(rectangle, true);
        });
        content.post(() -> ViewCompat.requestApplyInsets(content));
    }

    private static final class PaddingState {
        final int left;
        final int top;
        final int right;
        final int bottom;
        boolean revealFocusedInput;

        PaddingState(View view) {
            left = view.getPaddingLeft();
            top = view.getPaddingTop();
            right = view.getPaddingRight();
            bottom = view.getPaddingBottom();
        }
    }
}
