package com.example.android_mvvm_arch.core.network

import com.example.android_mvvm_arch.core.auth.AuthEventBus
import com.example.android_mvvm_arch.core.security.TokenStorage
import com.example.android_mvvm_arch.feature.auth.data.remote.AuthApi
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.RefreshTokenRequestDto
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val authApi: Lazy<AuthApi>,
    private val authEventBus: AuthEventBus,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val url = response.request.url.toString()
        if (url.contains("auth/refresh") || url.contains("auth/login")) return null

        synchronized(this) {
            val refreshToken = runBlocking { tokenStorage.getRefreshToken() }
                ?: run {
                    authEventBus.sendForceLogout()
                    return null
                }

            // 若另一個執行緒已刷新 token，直接使用新 token 重試
            val currentToken = runBlocking { tokenStorage.getAccessToken() }
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")?.trim()
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val newTokens = try {
                runBlocking {
                    authApi.get().refreshToken(RefreshTokenRequestDto(refreshToken))
                }
            } catch (e: Exception) {
                null
            }

            if (newTokens == null) {
                runBlocking { tokenStorage.clearTokens() }
                authEventBus.sendForceLogout()
                return null
            }

            runBlocking {
                tokenStorage.saveAccessToken(newTokens.accessToken)
                tokenStorage.saveRefreshToken(newTokens.refreshToken)
            }

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        }
    }
}
