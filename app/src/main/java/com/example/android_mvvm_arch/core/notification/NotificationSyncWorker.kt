package com.example.android_mvvm_arch.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.android_mvvm_arch.core.datastore.SettingsDataStore
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.GetNotificationsUseCase
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.RefreshNotificationsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 每 15 分鐘背景拉取最新通知，並針對「未讀新通知」彈出系統通知。
 *
 * - Hilt + WorkManager：使用 [HiltWorker] + [AssistedInject]，由 `HiltWorkerFactory` 注入。
 * - 尊重使用者偏好：若 `SettingsDataStore.notificationsEnabled == false`，直接 `Result.success()` 跳過。
 * - 失敗策略：API 失敗回傳 [Result.retry]，由 WorkManager 套用指數退避。
 */
@HiltWorker
class NotificationSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshNotificationsUseCase: RefreshNotificationsUseCase,
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val notificationsEnabled = settingsDataStore.settingsFlow.first().notificationsEnabled
        if (!notificationsEnabled) {
            return Result.success()
        }

        val previousUnreadIds: Set<String> = getNotificationsUseCase()
            .first()
            .filter { !it.isRead }
            .map(Notification::id)
            .toSet()

        val refreshResult = refreshNotificationsUseCase()
        if (refreshResult.isFailure) {
            return if (runAttemptCount < MAX_RETRY_COUNT) Result.retry() else Result.success()
        }

        val latestUnread = getNotificationsUseCase()
            .first()
            .filter { !it.isRead }

        val newUnread = latestUnread.filterNot { it.id in previousUnreadIds }
        newUnread.take(MAX_SYSTEM_NOTIFICATIONS_PER_SYNC).forEach(notificationHelper::showNotification)

        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "notification_sync"
        private const val MAX_RETRY_COUNT = 3
        private const val MAX_SYSTEM_NOTIFICATIONS_PER_SYNC = 3
        private const val PERIODIC_INTERVAL_MINUTES = 15L

        /**
         * 排程週期性同步（最短 15 分鐘）。
         * 使用 [ExistingPeriodicWorkPolicy.KEEP]：若已存在排程則保留，避免重複建立。
         */
        fun enqueuePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                PERIODIC_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
