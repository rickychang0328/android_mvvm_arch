package com.example.android_mvvm_arch.feature.notifications.data.remote

import com.example.android_mvvm_arch.feature.notifications.data.remote.dto.NotificationsResponseDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 通知 API 抽象介面。
 *
 * Mock 階段透過 `MockApiInterceptor` 在本地產生假資料；
 * 未來接上真實 FCM / 後端時，可保留路徑與 DTO，不必動上層程式碼。
 */
interface NotificationsApi {

    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = DEFAULT_PAGE_SIZE,
    ): NotificationsResponseDto

    @PATCH("api/v1/notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: String)

    @POST("api/v1/notifications/read-all")
    suspend fun markAllAsRead()

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
