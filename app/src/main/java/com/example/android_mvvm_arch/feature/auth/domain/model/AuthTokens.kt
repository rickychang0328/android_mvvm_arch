package com.example.android_mvvm_arch.feature.auth.domain.model

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val tokenType: String,
)
