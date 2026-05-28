package com.example.android_mvvm_arch.feature.auth.domain.repo

import com.example.android_mvvm_arch.feature.auth.domain.model.AuthTokens
import com.example.android_mvvm_arch.feature.auth.domain.model.LoginCredentials
import com.example.android_mvvm_arch.feature.auth.domain.model.RegisterCredentials

interface AuthRepository {
    suspend fun login(credentials: LoginCredentials): Result<AuthTokens>
    suspend fun logout(): Result<Unit>
    suspend fun isLoggedIn(): Boolean
    suspend fun refreshToken(refreshToken: String): Result<AuthTokens>
    suspend fun register(credentials: RegisterCredentials): Result<AuthTokens>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun resetPassword(token: String, newPassword: String): Result<Unit>
    suspend fun registerFcmToken(token: String): Result<Unit>
}
