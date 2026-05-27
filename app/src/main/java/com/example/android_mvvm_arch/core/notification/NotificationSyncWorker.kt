package com.example.android_mvvm_arch.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.android_mvvm_arch.core.sync.SyncManager
import com.example.android_mvvm_arch.core.sync.SyncTarget
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.GetNotificationsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 每 15 分鐘背景檢查通知，並針對「未讀新通知」彈出系統通知。
 *
 * - 同步資料來源已委派至 [SyncManager]，此 Worker 保留「新通知偵測 + 系統通知」能力。
 * - 若 `NOTIFICATIONS` 目標被設定關閉（例如 notificationsEnabled = false），同步結果會標記 skipped。
 * - 失敗策略：可重試錯誤回傳 [Result.retry]，由 WorkManager 套用退避策略。
 */
@HiltWorker
class NotificationSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager,
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val previousUnreadIds: Set<String> = getNotificationsUseCase()
            .first()
            .filter { !it.isRead }
            .map(Notification::id)
            .toSet()

        val syncResult = syncManager.runSync(setOf(SyncTarget.NOTIFICATIONS))
        if (syncResult.failed.isNotEmpty()) {
            return if (syncResult.shouldRetry && runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.success()
            }
        }
        if (SyncTarget.NOTIFICATIONS !in syncResult.succeeded) {
            return Result.success()
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
