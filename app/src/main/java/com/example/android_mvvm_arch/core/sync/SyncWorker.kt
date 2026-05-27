package com.example.android_mvvm_arch.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val targets = parseTargets(inputData)
        val result = syncManager.runSync(targets)
        if (result.isSuccess) {
            return Result.success()
        }
        return if (result.shouldRetry && runAttemptCount < MAX_RETRY_COUNT) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "app_periodic_sync"
        const val IMMEDIATE_WORK_NAME = "app_immediate_sync"
        const val TARGETS_KEY = "sync_targets"

        private const val PERIODIC_INTERVAL_MINUTES = 15L
        private const val BACKOFF_DELAY_SECONDS = 30L
        private const val MAX_RETRY_COUNT = 3

        fun enqueuePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                PERIODIC_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            )
                .setConstraints(defaultConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_SECONDS,
                    TimeUnit.SECONDS,
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueueImmediate(
            workManager: WorkManager,
            targets: Set<SyncTarget>,
        ) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(buildTargetsData(targets))
                .setConstraints(defaultConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_SECONDS,
                    TimeUnit.SECONDS,
                )
                .build()

            workManager.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }

        fun buildTargetsData(targets: Set<SyncTarget>): Data = Data.Builder()
            .putString(TARGETS_KEY, targets.joinToString(",") { it.name })
            .build()

        fun parseTargets(inputData: Data): Set<SyncTarget> {
            val raw = inputData.getString(TARGETS_KEY).orEmpty()
            if (raw.isBlank()) {
                return SyncTarget.defaultTargets
            }
            val parsed = raw.split(",")
                .mapNotNull(SyncTarget::fromToken)
                .toSet()
            return if (parsed.isEmpty()) SyncTarget.defaultTargets else parsed
        }

        private fun defaultConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
