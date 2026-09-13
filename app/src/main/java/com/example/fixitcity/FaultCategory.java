package com.example.fixitcity;

public class FaultCategory {

    private int id;
    private String title;
    private String description;
    private int severity;  // from algorithm

    // Optionally, an icon resource id can be determined by category type
    private int iconResId;


    public FaultCategory(int id, String title, String description, int severity, int iconResId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.iconResId = iconResId;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getSeverity() { return severity; }
    public int getIconResId() { return iconResId; }
}