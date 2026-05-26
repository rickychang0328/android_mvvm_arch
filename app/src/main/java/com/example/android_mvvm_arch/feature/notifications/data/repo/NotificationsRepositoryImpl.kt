package com.example.android_mvvm_arch.feature.notifications.data.repo

import com.example.android_mvvm_arch.core.network.safeApiCall
import com.example.android_mvvm_arch.feature.notifications.data.local.NotificationDao
import com.example.android_mvvm_arch.feature.notifications.data.mapper.NotificationMapper
import com.example.android_mvvm_arch.feature.notifications.data.remote.NotificationsApi
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.repo.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first 通知 Repository 實作：
 * - 讀取一律來自 Room（[observeNotifications] / [observeUnreadCount]）
 * - [refresh] 從 API 拉取後寫入 Room；UI 透過 Flow 自動更新
 * - [markAsRead] / [markAllAsRead] 先呼叫 API，成功後才更新本地，避免狀態不一致
 */
@Singleton
class NotificationsRepositoryImpl @Inject constructor(
    private val notificationsApi: NotificationsApi,
    private val notificationDao: NotificationDao,
    private val notificationMapper: NotificationMapper,
) : NotificationsRepository {

    override fun observeNotifications(): Flow<List<Notification>> =
        notificationDao.observeAll().map { list ->
            list.map(notificationMapper::toDomain)
        }

    override fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    override suspend fun refresh(): Result<Unit> = safeApiCall {
        notificationsApi.getNotifications()
    }.map { response ->
        val entities = response.items.map(notificationMapper::toEntity)
        notificationDao.upsertAll(entities)
    }

    override suspend fun markAsRead(id: String): Result<Unit> = safeApiCall {
        notificationsApi.markAsRead(id)
    }.onSuccess {
        notificationDao.markAsRead(id)
    }

    override suspend fun markAllAsRead(): Result<Unit> = safeApiCall {
        notificationsApi.markAllAsRead()
    }.onSuccess {
        notificationDao.markAllAsRead()
    }

    override suspend fun clearAll() {
        notificationDao.clearAll()
    }
}
