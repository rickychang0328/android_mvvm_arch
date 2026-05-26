package com.example.android_mvvm_arch.core.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 模擬後端 API 回應，對應 [docs/api-spec.md]。
 * 敏感欄位（password、token）不寫入 Log。
 */
@Singleton
class MockApiInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method

        val (code, body) = when {
            method == "POST" && path.endsWith("/api/v1/auth/login") ->
                handleLogin(request.body?.let { readBody(it) }.orEmpty())
            method == "POST" && path.endsWith("/api/v1/auth/logout") ->
                204 to ""
            method == "POST" && path.endsWith("/api/v1/auth/refresh") ->
                handleRefreshToken(request.body?.let { readBody(it) }.orEmpty())
            method == "POST" && path.endsWith("/api/v1/auth/register") ->
                handleRegister(request.body?.let { readBody(it) }.orEmpty())
            method == "POST" && path.endsWith("/api/v1/auth/forgot-password") ->
                handleForgotPassword(request.body?.let { readBody(it) }.orEmpty())
            method == "POST" && path.endsWith("/api/v1/auth/reset-password") ->
                handleResetPassword(request.body?.let { readBody(it) }.orEmpty())
            method == "GET" && path.endsWith("/api/v1/users/me") ->
                handleGetProfile(request.header("Authorization"))
            method == "PUT" && path.endsWith("/api/v1/users/me") ->
                handleUpdateProfile(
                    authHeader = request.header("Authorization"),
                    body = request.body?.let { readBody(it) }.orEmpty(),
                )
            method == "PUT" && path.endsWith("/api/v1/users/me/avatar") ->
                handleUploadAvatar(request.header("Authorization"))
            method == "GET" && path.endsWith("/api/v1/notifications") ->
                handleListNotifications(
                    authHeader = request.header("Authorization"),
                    pageParam = request.url.queryParameter("page"),
                    pageSizeParam = request.url.queryParameter("pageSize"),
                )
            method == "PATCH" && path.matches(NOTIFICATIONS_READ_REGEX) ->
                handleMarkNotificationRead(
                    authHeader = request.header("Authorization"),
                    path = path,
                )
            method == "POST" && path.endsWith("/api/v1/notifications/read-all") ->
                handleMarkAllNotificationsRead(request.header("Authorization"))
            else -> 404 to """{"error":"not_found","message":"Endpoint not found."}"""
        }

        if (code == 204) {
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(204)
                .message("No Content")
                .body("".toResponseBody(null))
                .build()
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun handleLogin(body: String): Pair<Int, String> {
        val email = extractJsonField(body, "email")
        val password = extractJsonField(body, "password")
        return if (email == DEMO_EMAIL && password == DEMO_PASSWORD) {
            200 to """
                {
                  "access_token": "$MOCK_ACCESS_TOKEN",
                  "refresh_token": "$MOCK_REFRESH_TOKEN",
                  "expires_in": 3600,
                  "token_type": "Bearer"
                }
            """.trimIndent()
        } else {
            401 to """{"error":"invalid_credentials","message":"Email or password is incorrect."}"""
        }
    }

    private fun handleRefreshToken(body: String): Pair<Int, String> {
        val refreshToken = extractJsonField(body, "refresh_token")
        return if (refreshToken == MOCK_REFRESH_TOKEN) {
            200 to """
                {
                  "access_token": "$MOCK_ACCESS_TOKEN",
                  "refresh_token": "$MOCK_REFRESH_TOKEN",
                  "expires_in": 3600,
                  "token_type": "Bearer"
                }
            """.trimIndent()
        } else {
            401 to """{"error":"invalid_token","message":"Refresh token is invalid or expired."}"""
        }
    }

    private fun handleRegister(body: String): Pair<Int, String> {
        val email = extractJsonField(body, "email")
        val displayName = extractJsonField(body, "display_name").ifBlank { "New User" }
        return when {
            email == DEMO_EMAIL -> {
                409 to """{"error":"email_exists","message":"An account with this email already exists."}"""
            }
            email.isBlank() -> {
                400 to """{"error":"invalid_input","message":"Email is required."}"""
            }
            else -> {
                201 to """
                    {
                      "access_token": "${MOCK_ACCESS_TOKEN}_new",
                      "refresh_token": "${MOCK_REFRESH_TOKEN}_new",
                      "expires_in": 3600,
                      "token_type": "Bearer"
                    }
                """.trimIndent()
            }
        }
    }

    private fun handleForgotPassword(body: String): Pair<Int, String> {
        val email = extractJsonField(body, "email")
        return if (email.isBlank()) {
            400 to """{"error":"invalid_input","message":"Email is required."}"""
        } else {
            200 to """{"message":"If this email is registered, a password reset link has been sent."}"""
        }
    }

    private fun handleResetPassword(body: String): Pair<Int, String> {
        val token = extractJsonField(body, "token")
        return if (token == DEMO_RESET_TOKEN) {
            200 to """{"message":"Password has been reset successfully."}"""
        } else {
            400 to """{"error":"invalid_token","message":"Reset token is invalid or expired."}"""
        }
    }

    private fun handleGetProfile(authHeader: String?): Pair<Int, String> {
        if (!isValidToken(authHeader)) {
            return 401 to """{"error":"unauthorized","message":"Invalid or expired token."}"""
        }
        return 200 to defaultProfileJson()
    }

    private fun handleUpdateProfile(authHeader: String?, body: String): Pair<Int, String> {
        if (!isValidToken(authHeader)) {
            return 401 to """{"error":"unauthorized","message":"Invalid or expired token."}"""
        }
        val displayName = extractJsonField(body, "display_name").ifBlank { "Demo User" }
        val phone = extractJsonField(body, "phone").ifBlank { "+886912345678" }
        val bio = extractJsonField(body, "bio").ifBlank { "Android MVVM sample user." }
        return 200 to """
            {
              "id": "usr_001",
              "email": "$DEMO_EMAIL",
              "display_name": "$displayName",
              "avatar_url": "https://api.example.com/avatars/usr_001.png",
              "phone": "$phone",
              "bio": "$bio",
              "created_at": "2024-01-15T08:30:00Z",
              "updated_at": "2025-03-01T12:00:00Z"
            }
        """.trimIndent()
    }

    private fun handleUploadAvatar(authHeader: String?): Pair<Int, String> {
        if (!isValidToken(authHeader)) {
            return 401 to """{"error":"unauthorized","message":"Invalid or expired token."}"""
        }
        val timestamp = System.currentTimeMillis()
        return 200 to """
            {
              "id": "usr_001",
              "email": "$DEMO_EMAIL",
              "display_name": "Demo User",
              "avatar_url": "https://api.example.com/avatars/usr_001_$timestamp.png",
              "phone": "+886912345678",
              "bio": "Android MVVM sample user.",
              "created_at": "2024-01-15T08:30:00Z",
              "updated_at": "2025-05-26T12:00:00Z"
            }
        """.trimIndent()
    }

    private fun handleListNotifications(
        authHeader: String?,
        pageParam: String?,
        pageSizeParam: String?,
    ): Pair<Int, String> {
        if (!isValidToken(authHeader)) {
            return 401 to """{"error":"unauthorized","message":"Invalid or expired token."}"""
        }
        val page = pageParam?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = pageSizeParam?.toIntOrNull()?.coerceAtLeast(1) ?: DEFAULT_NOTIFICATION_PAGE_SIZE
        val startIndex = (page - 1) * pageSize
        val (pageItems, nextPage, hasMore) = synchronized(notificationsLock) {
            ensureNotificationsSeeded()
            val boundedStart = startIndex.coerceAtMost(notificationsStore.size)
            val boundedEnd = (boundedStart + pageSize).coerceAtMost(notificationsStore.size)
            val localHasMore = boundedEnd < notificationsStore.size
            val localNextPage = if (localHasMore) page + 1 else null
            Triple(
                notificationsStore.subList(boundedStart, boundedEnd).toList(),
                localNextPage,
                localHasMore,
            )
        }
        val joined = pageItems.joinToString(separator = ",\n") { item ->
            mockNotification(
                id = item.id,
                title = item.title,
                body = item.body,
                type = item.type,
                isRead = item.isRead,
                createdAt = item.createdAt,
            )
        }
        return 200 to """
            {
              "items": [
$joined
              ],
              "next_page": ${nextPage?.toString() ?: "null"},
              "has_more": $hasMore
            }
        """.trimIndent()
    }

    private fun handleMarkNotificationRead(authHeader: String?, path: String): Pair<Int, String> {
        if (!isValidToken(authHeader)) {
            return 401 to """{"error":"unauthorized","message":"Invalid or expired token."}"""
        }
        val id = path.substringAfter("/api/v1/notifications/").substringBefore("/read")
        synchronized(notificationsLock) {
            ensureNotificationsSeeded()
            val index = notificationsStore.indexOfFirst { it.id == id }
            if (index >= 0) {
                val item = notificationsStore[index]
                notificationsStore[index] = item.copy(isRead = true)
            }
        }
        return 204 to ""
    }

    private fun handleMarkAllNotificationsRead(authHeader: String?): Pair<Int, String> {
        if (!isValidToken(authHeader)) {
            return 401 to """{"error":"unauthorized","message":"Invalid or expired token."}"""
        }
        synchronized(notificationsLock) {
            ensureNotificationsSeeded()
            for (index in notificationsStore.indices) {
                val item = notificationsStore[index]
                if (!item.isRead) {
                    notificationsStore[index] = item.copy(isRead = true)
                }
            }
        }
        return 204 to ""
    }

    private fun mockNotification(
        id: String,
        title: String,
        body: String,
        type: String,
        isRead: Boolean,
        createdAt: Long,
    ): String = """
        {
          "id": "$id",
          "title": "$title",
          "body": "$body",
          "type": "$type",
          "is_read": $isRead,
          "created_at": $createdAt
        }
    """.trimIndent()

    private fun isValidToken(authHeader: String?): Boolean {
        val token = authHeader?.removePrefix("Bearer ")?.trim()
        return token == MOCK_ACCESS_TOKEN || token == "${MOCK_ACCESS_TOKEN}_new"
    }

    private fun defaultProfileJson(): String = """
        {
          "id": "usr_001",
          "email": "$DEMO_EMAIL",
          "display_name": "Demo User",
          "avatar_url": "https://api.example.com/avatars/usr_001.png",
          "phone": "+886912345678",
          "bio": "Android MVVM sample user.",
          "created_at": "2024-01-15T08:30:00Z",
          "updated_at": "2025-03-01T12:00:00Z"
        }
    """.trimIndent()

    private fun extractJsonField(json: String, field: String): String {
        val regex = """"$field"\s*:\s*"([^"]*)"""".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun readBody(body: okhttp3.RequestBody): String {
        val buffer = okio.Buffer()
        body.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun ensureNotificationsSeeded() {
        if (notificationsStore.isNotEmpty()) return
        val now = System.currentTimeMillis()
        notificationsStore.addAll(
            listOf(
                NotificationSeed(
                    id = "ntf_001",
                    title = "歡迎使用本應用",
                    body = "感謝您下載使用，點擊查看新手導覽，快速熟悉所有功能。",
                    type = "SYSTEM",
                    isRead = false,
                    createdAt = now - 5 * 60_000L,
                ),
                NotificationSeed(
                    id = "ntf_002",
                    title = "限時優惠",
                    body = "升級會員享有 8 折優惠，並可解鎖進階主題與雲端同步。",
                    type = "PROMOTION",
                    isRead = false,
                    createdAt = now - 35 * 60_000L,
                ),
                NotificationSeed(
                    id = "ntf_003",
                    title = "系統維護通知",
                    body = "本系統將於本週日凌晨 02:00–04:00 進行維護，期間可能無法登入。",
                    type = "SYSTEM",
                    isRead = true,
                    createdAt = now - 3 * 60 * 60_000L,
                ),
                NotificationSeed(
                    id = "ntf_004",
                    title = "新活動上線",
                    body = "週末打卡活動已開放報名，完成任務可獲得限定徽章。",
                    type = "ACTIVITY",
                    isRead = false,
                    createdAt = now - 8 * 60 * 60_000L,
                ),
                NotificationSeed(
                    id = "ntf_005",
                    title = "個人資料同步成功",
                    body = "您的個人資料已自雲端成功同步，最後同步時間已更新。",
                    type = "SYSTEM",
                    isRead = true,
                    createdAt = now - 26 * 60 * 60_000L,
                ),
                NotificationSeed(
                    id = "ntf_006",
                    title = "推薦給您的內容",
                    body = "根據您的偏好，為您挑選了 5 篇精選內容，立即查看。",
                    type = "PROMOTION",
                    isRead = false,
                    createdAt = now - 2 * 24 * 60 * 60_000L,
                ),
                NotificationSeed(
                    id = "ntf_007",
                    title = "週末工作坊",
                    body = "本週六線上工作坊「Compose 實戰」名額有限，報名從速。",
                    type = "ACTIVITY",
                    isRead = true,
                    createdAt = now - 3 * 24 * 60 * 60_000L,
                ),
            ),
        )
    }

    private data class NotificationSeed(
        val id: String,
        val title: String,
        val body: String,
        val type: String,
        val isRead: Boolean,
        val createdAt: Long,
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val NOTIFICATIONS_READ_REGEX =
            """^/api/v1/notifications/[^/]+/read$""".toRegex()
        private const val DEFAULT_NOTIFICATION_PAGE_SIZE = 20
        const val DEMO_EMAIL = "demo@example.com"
        const val DEMO_PASSWORD = "password123"
        const val MOCK_ACCESS_TOKEN = "mock_access_token_demo"
        const val MOCK_REFRESH_TOKEN = "mock_refresh_token_demo"
        const val DEMO_RESET_TOKEN = "demo_reset_token_123"
    }

    private val notificationsLock = Any()
    private val notificationsStore = mutableListOf<NotificationSeed>()
}
