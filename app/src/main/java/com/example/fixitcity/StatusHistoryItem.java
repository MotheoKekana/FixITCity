package com.example.fixitcity;

public class StatusHistoryItem {
    public String oldStatus;
    public String newStatus;
    public String changedAt;
    public String notes;

    public StatusHistoryItem(String oldStatus, String newStatus, String changedAt, String notes) {
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
        this.notes = notes;
    }
}