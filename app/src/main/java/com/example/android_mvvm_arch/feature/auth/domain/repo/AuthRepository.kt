package com.example.android_mvvm_arch.feature.auth.domain.repo

import com.example.android_mvvm_arch.feature.auth.domain.model.AuthTokens
import com.example.android_mvvm_arch.feature.auth.domain.model.LoginCredentials

interface AuthRepository {
    suspend fun login(credentials: LoginCredentials): Result<AuthTokens>
    suspend fun logout(): Result<Unit>
    suspend fun isLoggedIn(): Boolean
}
