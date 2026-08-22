package com.example.fixitcity;

import android.app.Application;

public class FixITCityApp extends Application {
    private ReportData reportData;

    @Override
    public void onCreate() {
        super.onCreate();
        reportData = new ReportData();
    }

    public ReportData getReportData() {
        return reportData;
    }

    public void clearReportData() {
        reportData = new ReportData();
    }
}