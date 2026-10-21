package com.example.fixitcity;

public class FaultCategory {

    private int id;
    private String title;
    private String description;
    private int severity;
    private int iconResId;

    private long categoryId;
    private double severityWeight;
    private int estimatedFixTime;

    public FaultCategory(int id, String title, String description, int severity, int iconResId) {
        this.id = id;
        this.categoryId = id;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.severityWeight = severity;
        this.iconResId = iconResId;
    }

    public FaultCategory(long categoryId, String name, String description, double severityWeight, int estimatedFixTime, int iconResId) {
        this.categoryId = categoryId;
        this.id = (int) categoryId;
        this.title = name;
        this.description = description;
        this.severityWeight = severityWeight;
        this.estimatedFixTime = estimatedFixTime;
        this.iconResId = iconResId;
    }

    public int getId() { return id; }
    public String getName() { return title; }
    public String getDescription() { return description; }
    public int getSeverity() { return severity; }
    public int getIconResId() { return iconResId; }

    public long getCategoryId() { return categoryId; }
    public double getSeverityWeight() { return severityWeight; }
    public int getEstimatedFixTime() { return estimatedFixTime; }
}