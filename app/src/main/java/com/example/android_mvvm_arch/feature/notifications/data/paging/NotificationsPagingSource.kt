package com.example.android_mvvm_arch.feature.notifications.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.android_mvvm_arch.feature.notifications.data.local.NotificationDao
import com.example.android_mvvm_arch.feature.notifications.data.mapper.NotificationMapper
import com.example.android_mvvm_arch.feature.notifications.data.remote.NotificationsApi
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification

/**
 * Notifications 的 PagingSource，作為後續 ActivityLog 等列表的範本：
 * - API 以 page/pageSize 查詢
 * - load() 回傳 LoadResult.Page / LoadResult.Error
 * - 同步將已載入頁寫入 Room，以維持 unread badge 與背景同步邏輯一致
 */
class NotificationsPagingSource(
    private val notificationsApi: NotificationsApi,
    private val notificationMapper: NotificationMapper,
    private val notificationDao: NotificationDao,
) : PagingSource<Int, Notification>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Notification> {
        return try {
            val page = params.key ?: 1
            val response = notificationsApi.getNotifications(page = page, pageSize = params.loadSize)
            val entities = response.items.map(notificationMapper::toEntity)
            if (entities.isNotEmpty()) {
                notificationDao.upsertAll(entities)
            }
            val items = response.items.map(notificationMapper::toDomain)
            val nextPage = if (response.hasMore) response.nextPage ?: (page + 1) else null
            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = nextPage,
            )
        } catch (error: Throwable) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Notification>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition)
        return anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
    }
}
