package com.example.android_mvvm_arch.feature.profile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val phone: String?,
    val bio: String?,
    val createdAt: String,
    val updatedAt: String,
)
