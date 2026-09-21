package com.example.fixitcity;

public class FaultCategory {

    // Existing fields
    private int id;
    private String title;
    private String description;
    private int severity;
    private int iconResId;

    // New fields from Supabase
    private long categoryId;
    private double severityWeight;
    private int estimatedFixTime;

    // Constructor for hardcoded categories
    public FaultCategory(int id, String title, String description, int severity, int iconResId) {
        this.id = id;
        this.categoryId = id;              // ← link the two so fallback categories also have a categoryId
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.severityWeight = severity;    // ← NEW: mirror severity into severityWeight so priority scoring works
        this.iconResId = iconResId;
    }

    // Constructor for Supabase-loaded categories
    public FaultCategory(long categoryId, String name, String description, double severityWeight, int estimatedFixTime, int iconResId) {
        this.categoryId = categoryId;
        this.id = (int) categoryId;
        this.title = name;
        this.description = description;
        this.severityWeight = severityWeight;
        this.estimatedFixTime = estimatedFixTime;
        this.iconResId = iconResId;
    }

    // Existing getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getSeverity() { return severity; }
    public int getIconResId() { return iconResId; }

    // New getters
    public long getCategoryId() { return categoryId; }
    public double getSeverityWeight() { return severityWeight; }
    public int getEstimatedFixTime() { return estimatedFixTime; }
}