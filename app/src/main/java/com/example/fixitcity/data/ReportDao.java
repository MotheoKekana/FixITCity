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

    // A process can stop after SyncWorker marks a row Syncing. Include it on
    // the next run so an interrupted upload is retried by reference number.
    @Query("SELECT * FROM report_outbox WHERE citizen_id = :citizenId AND sync_status IN ('Pending', 'Failed', 'Syncing') ORDER BY created_at_local ASC")
    List<ReportEntity> getPendingForCitizen(long citizenId);

    // Guest offline IDs begin with the Supabase anonymous auth UUID.
    // This scopes pending reports to the current guest session without a Room migration.
    @Query("SELECT * FROM report_outbox WHERE citizen_id = -1 AND offline_id LIKE :guestPrefix || '%' AND sync_status IN ('Pending', 'Failed', 'Syncing') ORDER BY created_at_local ASC")
    List<ReportEntity> getPendingForGuest(String guestPrefix);

    @Query("SELECT COUNT(*) FROM report_outbox WHERE citizen_id = :citizenId AND sync_status IN ('Pending', 'Failed', 'Syncing')")
    int getPendingCountForCitizen(long citizenId);

    @Query("SELECT * FROM report_outbox WHERE offline_id = :offlineId LIMIT 1")
    ReportEntity getById(String offlineId);
}
