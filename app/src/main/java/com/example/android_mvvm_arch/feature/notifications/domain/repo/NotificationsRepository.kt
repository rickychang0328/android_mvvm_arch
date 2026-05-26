package com.example.android_mvvm_arch.feature.notifications.domain.repo

import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * 通知資料來源抽象介面（Clean Architecture Domain Layer）。
 *
 * Offline-first：UI 透過 [observeNotifications] / [observeUnreadCount] 訂閱 Room；
 * [refresh] 會拉取遠端最新通知後寫入本地快取。
 *
 * Mock 階段以 [com.example.android_mvvm_arch.feature.notifications.data.remote.NotificationsApi]
 * + `MockApiInterceptor` 模擬伺服器，未來可替換為 FCM / 真實後端而不影響上層。
 */
interface NotificationsRepository {
    fun getNotificationsPagingData(pageSize: Int = 20): Flow<PagingData<Notification>>
    fun observeNotifications(): Flow<List<Notification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun refresh(): Result<Unit>
    suspend fun markAsRead(id: String): Result<Unit>
    suspend fun markAllAsRead(): Result<Unit>
    suspend fun clearAll()
}
