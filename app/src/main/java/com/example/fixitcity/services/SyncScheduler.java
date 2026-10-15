package com.example.fixitcity.services;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.List;

public class SyncScheduler {

    private static final String UNIQUE_NAME = "fixitcity_sync_pending";

    /** Observe the same unique job that both the app and the manual button enqueue. */
    public static LiveData<List<WorkInfo>> observeSync(Context context) {
        return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(UNIQUE_NAME);
    }

    /** Enqueue a unique sync job with a network constraint. Safe to call repeatedly. */
    public static void scheduleSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request);
    }
}
