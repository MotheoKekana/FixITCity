package com.example.fixitcity.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReportDao {

    @Insert
    void insert(ReportEntity report);

    @Update
    void update(ReportEntity report);

    @Delete
    void delete(ReportEntity report);

    @Query("SELECT * FROM report_outbox ORDER BY created_at_local ASC")
    List<ReportEntity> getAll();

    @Query("SELECT * FROM report_outbox WHERE sync_status = 'Pending' OR sync_status = 'Failed' ORDER BY created_at_local ASC")
    List<ReportEntity> getPending();

    @Query("SELECT COUNT(*) FROM report_outbox WHERE sync_status = 'Pending' OR sync_status = 'Failed'")
    int getPendingCount();

    @Query("SELECT * FROM report_outbox WHERE offline_id = :offlineId LIMIT 1")
    ReportEntity getById(String offlineId);
}