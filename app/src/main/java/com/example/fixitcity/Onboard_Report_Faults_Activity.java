package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;

/** Shows all three onboarding pages within one activity. */
public class Onboard_Report_Faults_Activity extends AppCompatActivity {

    private static final String STATE_PAGE = "onboarding_page";
    private static final int PAGE_COUNT = 3;
    private static final long FADE_DURATION_MS = 220;

    private ViewFlipper pages;
    private int currentPage;
    private float touchStartX;
    private float touchStartY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pages = new ViewFlipper(this);
        pages.setId(View.generateViewId());
        setContentView(pages);

        ScreenInsets.apply(this);

        View reportPage = getLayoutInflater().inflate(
                R.layout.activity_onboard_report_faults, pages, false);
        View trackingPage = getLayoutInflater().inflate(
                R.layout.activity_onboard_track_reports, pages, false);
        View impactPage = getLayoutInflater().inflate(
                R.layout.activity_onboard_community_impact, pages, false);

        pages.addView(reportPage);
        pages.addView(trackingPage);
        pages.addView(impactPage);

        reportPage.findViewById(R.id.btn_next).setOnClickListener(v -> showPage(1));
        trackingPage.findViewById(R.id.btn_next).setOnClickListener(v -> showPage(2));
        reportPage.findViewById(R.id.btn_skip).setOnClickListener(v -> finishOnboarding());
        trackingPage.findViewById(R.id.btn_skip).setOnClickListener(v -> finishOnboarding());
        impactPage.findViewById(R.id.btn_get_started).setOnClickListener(v -> finishOnboarding());

        if (savedInstanceState != null) {
            currentPage = Math.max(0, Math.min(PAGE_COUNT - 1,
                    savedInstanceState.getInt(STATE_PAGE, 0)));
            pages.setDisplayedChild(currentPage);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            touchStartX = event.getX();
            touchStartY = event.getY();
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - touchStartX;
            float dy = event.getY() - touchStartY;
            float minSwipe = 72 * getResources().getDisplayMetrics().density;
            if (Math.abs(dx) >= minSwipe && Math.abs(dx) > Math.abs(dy)) {
                showPage(currentPage + (dx < 0 ? 1 : -1));
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void showPage(int index) {
        if (index < 0 || index >= PAGE_COUNT || index == currentPage) return;

        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(FADE_DURATION_MS);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(FADE_DURATION_MS);
        pages.setOutAnimation(fadeOut);
        pages.setInAnimation(fadeIn);
        currentPage = index;
        pages.setDisplayedChild(index);
    }

    private void finishOnboarding() {
        new SessionManager(this).setSeenOnboarding(true);
        startActivity(new Intent(this, Login_Activity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (currentPage > 0) {
            showPage(currentPage - 1);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_PAGE, currentPage);
        super.onSaveInstanceState(outState);
    }
}
