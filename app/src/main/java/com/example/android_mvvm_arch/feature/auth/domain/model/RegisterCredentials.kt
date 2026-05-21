package com.example.android_mvvm_arch.feature.auth.domain.model

data class RegisterCredentials(
    val email: String,
    val password: String,
    val displayName: String,
)
