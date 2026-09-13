package com.handsign.poc.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.handsign.poc.data.firebase.FirebaseSyncService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Firebase sync worker — STUB.
 * Currently returns [Result.success] immediately without doing anything.
 * Activate FirebaseSyncService to make this functional.
 */
@HiltWorker
class FirebaseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncService: FirebaseSyncService
) : CoroutineWorker(appContext, params) {

    companion object {
        fun buildRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<FirebaseSyncWorker>(1, java.util.concurrent.TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("firebase_sync")
                .build()
    }

    override suspend fun doWork(): Result {
        return when (syncService.syncPendingSessions()) {
            is FirebaseSyncService.SyncResult.Success     -> Result.success()
            is FirebaseSyncService.SyncResult.NotConfigured -> Result.success() // no-op until Firebase enabled
            is FirebaseSyncService.SyncResult.Error       -> Result.retry()
            else                                           -> Result.success()
        }
    }
}
