package com.example.fixitcity.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "report_outbox")
public class ReportEntity {

    @PrimaryKey
    @NonNull
    public String offline_id;         // UUID

    public long citizen_id;
    public long category_id;
    public double severity_weight;

    public String title;
    public String description;
    public double latitude;
    public double longitude;
    public String address_line;

    public String reference_number;
    public int priority_score;

    public String photo_filename;      // e.g. "12e8..9f.jpg" — may be null
    public String photo_mime_type;     // e.g. "image/jpeg" — may be null

    public String sync_status;         // "Pending" | "Syncing" | "Failed"
    public long created_at_local;      // epoch millis
    public Long last_sync_attempt;     // epoch millis, nullable
    public String sync_error;          // nullable

    public ReportEntity() {}
}