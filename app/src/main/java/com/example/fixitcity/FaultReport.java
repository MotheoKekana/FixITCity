package com.example.fixitcity;

public class FaultReport {

    public long report_id;
    public long citizen_id;
    public long category_id;
    public Long area_id;
    public String title;
    public String description;
    public double latitude;
    public double longitude;
    public String address_line;
    public String status;
    public Integer priority_score;
    public String reference_number;
    public String created_at;
    public String updated_at;

    // Phase 8: true when this report exists only in Room (not yet on server)
    private boolean pendingSync = false;

    public FaultReport() {}

    public long getReportId() { return report_id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddressLine() { return address_line; }
    public String getStatus() { return status; }
    public Integer getPriorityScore() { return priority_score; }
    public String getReferenceNumber() { return reference_number; }
    public String getCreatedAt() { return created_at; }

    public boolean isPendingSync() { return pendingSync; }
    public void setPendingSync(boolean pending) { this.pendingSync = pending; }
}