package com.example.android_mvvm_arch.feature.auth.data.mapper

import com.example.android_mvvm_arch.feature.auth.data.remote.dto.LoginRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.LoginResponseDto
import com.example.android_mvvm_arch.feature.auth.domain.model.AuthTokens
import com.example.android_mvvm_arch.feature.auth.domain.model.LoginCredentials
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthMapper @Inject constructor() {
    fun toLoginRequestDto(credentials: LoginCredentials): LoginRequestDto =
        LoginRequestDto(email = credentials.email, password = credentials.password)

    fun toDomain(dto: LoginResponseDto): AuthTokens = AuthTokens(
        accessToken = dto.accessToken,
        refreshToken = dto.refreshToken,
        expiresInSeconds = dto.expiresIn,
        tokenType = dto.tokenType,
    )
}
