package com.example.fixitcity;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class FixITCityApp extends Application {
    private ReportData reportData;

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        reportData = new ReportData();
    }

    public ReportData getReportData() {
        return reportData;
    }

    public void clearReportData() {
        reportData = new ReportData();
    }
}