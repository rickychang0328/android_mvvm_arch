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
            method == "GET" && path.endsWith("/api/v1/users/me") ->
                handleGetProfile(request.header("Authorization"))
            method == "PUT" && path.endsWith("/api/v1/users/me") ->
                handleUpdateProfile(
                    authHeader = request.header("Authorization"),
                    body = request.body?.let { readBody(it) }.orEmpty(),
                )
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
                  "access_token": "mock_access_token_demo",
                  "refresh_token": "mock_refresh_token_demo",
                  "expires_in": 3600,
                  "token_type": "Bearer"
                }
            """.trimIndent()
        } else {
            401 to """{"error":"invalid_credentials","message":"Email or password is incorrect."}"""
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

    private fun isValidToken(authHeader: String?): Boolean {
        val token = authHeader?.removePrefix("Bearer ")?.trim()
        return token == MOCK_ACCESS_TOKEN
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

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        const val DEMO_EMAIL = "demo@example.com"
        const val DEMO_PASSWORD = "password123"
        const val MOCK_ACCESS_TOKEN = "mock_access_token_demo"
    }
}
