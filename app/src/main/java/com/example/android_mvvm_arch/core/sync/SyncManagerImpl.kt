package com.example.android_mvvm_arch.core.sync

import androidx.work.WorkManager
import com.example.android_mvvm_arch.core.datastore.SettingsDataStore
import com.example.android_mvvm_arch.core.network.ApiException
import com.example.android_mvvm_arch.feature.auth.domain.usecase.IsLoggedInUseCase
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.RefreshNotificationsUseCase
import com.example.android_mvvm_arch.feature.profile.domain.usecase.GetUserProfileUseCase
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val refreshNotificationsUseCase: RefreshNotificationsUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val isLoggedInUseCase: IsLoggedInUseCase,
) : SyncManager {

    override fun schedulePeriodicSync() {
        SyncWorker.enqueuePeriodic(workManager)
    }

    override fun requestImmediateSync(targets: Set<SyncTarget>) {
        SyncWorker.enqueueImmediate(workManager, targets)
    }

    override suspend fun runSync(targets: Set<SyncTarget>): SyncResult {
        if (!isLoggedInUseCase()) {
            return SyncResult(skipped = targets, shouldRetry = false)
        }

        val enabledTargets = filterTargetsBySettings(targets)
        val skippedTargets = targets - enabledTargets

        val succeeded = mutableSetOf<SyncTarget>()
        val failed = mutableMapOf<SyncTarget, Throwable>()
        var shouldRetry = false

        enabledTargets.forEach { target ->
            val result = when (target) {
                SyncTarget.PROFILE -> getUserProfileUseCase.refresh().map { Unit }
                SyncTarget.NOTIFICATIONS -> refreshNotificationsUseCase()
            }
            result
                .onSuccess { succeeded += target }
                .onFailure { error ->
                    failed[target] = error
                    shouldRetry = shouldRetry || isRetryable(error)
                }
        }

        return SyncResult(
            succeeded = succeeded,
            failed = failed,
            skipped = skippedTargets,
            shouldRetry = shouldRetry,
        )
    }

    private suspend fun filterTargetsBySettings(targets: Set<SyncTarget>): Set<SyncTarget> {
        val notificationsEnabled = settingsDataStore.settingsFlow.first().notificationsEnabled
        return targets.filterTo(mutableSetOf()) { target ->
            when (target) {
                SyncTarget.PROFILE -> true
                SyncTarget.NOTIFICATIONS -> notificationsEnabled
            }
        }
    }

    private fun isRetryable(error: Throwable): Boolean = when (error) {
        is IOException -> true
        is ApiException -> error.code >= 500
        else -> false
    }
}
