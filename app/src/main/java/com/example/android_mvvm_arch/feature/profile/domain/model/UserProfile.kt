package com.example.android_mvvm_arch.feature.profile.domain.model

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val phone: String?,
    val bio: String?,
    val createdAt: String,
    val updatedAt: String,
)
