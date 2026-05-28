package com.example.android_mvvm_arch.feature.auth.data.repo

import com.example.android_mvvm_arch.core.network.safeApiCall
import com.example.android_mvvm_arch.core.security.TokenStorage
import com.example.android_mvvm_arch.feature.auth.data.mapper.AuthMapper
import com.example.android_mvvm_arch.feature.auth.data.remote.AuthApi
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.ForgotPasswordRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.RefreshTokenRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.RegisterFcmTokenRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.ResetPasswordRequestDto
import com.example.android_mvvm_arch.feature.auth.domain.model.AuthTokens
import com.example.android_mvvm_arch.feature.auth.domain.model.LoginCredentials
import com.example.android_mvvm_arch.feature.auth.domain.model.RegisterCredentials
import com.example.android_mvvm_arch.feature.auth.domain.repo.AuthRepository
import com.example.android_mvvm_arch.feature.profile.domain.repo.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val authMapper: AuthMapper,
    private val tokenStorage: TokenStorage,
    private val profileRepository: ProfileRepository,
) : AuthRepository {

    override suspend fun login(credentials: LoginCredentials): Result<AuthTokens> {
        val result = safeApiCall {
            authApi.login(authMapper.toLoginRequestDto(credentials))
        }
        return result.map { dto ->
            val tokens = authMapper.toDomain(dto)
            tokenStorage.saveAccessToken(tokens.accessToken)
            tokenStorage.saveRefreshToken(tokens.refreshToken)
            profileRepository.refreshProfile()
            tokens
        }
    }

    override suspend fun logout(): Result<Unit> {
        val result = safeApiCall { authApi.logout() }
        tokenStorage.clearTokens()
        profileRepository.clearProfileCache()
        return result
    }

    override suspend fun isLoggedIn(): Boolean = tokenStorage.hasAccessToken()

    override suspend fun refreshToken(refreshToken: String): Result<AuthTokens> {
        val result = safeApiCall {
            authApi.refreshToken(RefreshTokenRequestDto(refreshToken))
        }
        return result.map { dto ->
            val tokens = authMapper.toDomain(dto)
            tokenStorage.saveAccessToken(tokens.accessToken)
            tokenStorage.saveRefreshToken(tokens.refreshToken)
            tokens
        }
    }

    override suspend fun register(credentials: RegisterCredentials): Result<AuthTokens> {
        val result = safeApiCall {
            authApi.register(authMapper.toRegisterRequestDto(credentials))
        }
        return result.map { dto ->
            val tokens = authMapper.toDomain(dto)
            tokenStorage.saveAccessToken(tokens.accessToken)
            tokenStorage.saveRefreshToken(tokens.refreshToken)
            profileRepository.refreshProfile()
            tokens
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> =
        safeApiCall { authApi.forgotPassword(ForgotPasswordRequestDto(email)) }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> =
        safeApiCall { authApi.resetPassword(ResetPasswordRequestDto(token, newPassword)) }

    override suspend fun registerFcmToken(token: String): Result<Unit> =
        safeApiCall { authApi.registerFcmToken(RegisterFcmTokenRequestDto(fcmToken = token)) }
}
