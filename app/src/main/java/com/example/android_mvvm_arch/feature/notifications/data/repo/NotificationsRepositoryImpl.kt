package com.example.android_mvvm_arch.feature.notifications.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.android_mvvm_arch.core.network.safeApiCall
import com.example.android_mvvm_arch.feature.notifications.data.local.NotificationDao
import com.example.android_mvvm_arch.feature.notifications.data.local.NotificationEntity
import com.example.android_mvvm_arch.feature.notifications.data.mapper.NotificationMapper
import com.example.android_mvvm_arch.feature.notifications.data.paging.NotificationsPagingSource
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

    override fun getNotificationsPagingData(pageSize: Int): Flow<PagingData<Notification>> =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                NotificationsPagingSource(
                    notificationsApi = notificationsApi,
                    notificationMapper = notificationMapper,
                    notificationDao = notificationDao,
                )
            },
        ).flow

    override fun observeNotifications(): Flow<List<Notification>> =
        notificationDao.observeAll().map { list ->
            list.map(notificationMapper::toDomain)
        }

    override fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    override suspend fun refresh(): Result<Unit> {
        val allEntities = mutableListOf<NotificationEntity>()
        var page = 1
        while (true) {
            val result = safeApiCall {
                notificationsApi.getNotifications(page = page, pageSize = REFRESH_PAGE_SIZE)
            }
            val response = result.getOrElse { return Result.failure(it) }
            allEntities += response.items.map(notificationMapper::toEntity)
            if (!response.hasMore) {
                break
            }
            page = response.nextPage ?: (page + 1)
        }
        notificationDao.clearAll()
        if (allEntities.isNotEmpty()) {
            notificationDao.upsertAll(allEntities)
        }
        return Result.success(Unit)
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

    companion object {
        private const val REFRESH_PAGE_SIZE = 50
    }
}
