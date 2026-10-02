package com.landlord.android.core.sync;

import android.content.Context;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Constraints;
import java.util.concurrent.TimeUnit;

public class SyncScheduler {

    private static final String UNIQUE_SYNC_WORK = "landlord-sync";
    private static final String PERIODIC_SYNC_WORK = "landlord-sync-periodic";

    public static void ensurePeriodicSyncScheduled(Context appContext) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void requestImmediateSync(Context appContext) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();

        WorkManager.getInstance(appContext).enqueueUniqueWork(
                UNIQUE_SYNC_WORK, ExistingWorkPolicy.KEEP, request);
    }
}
