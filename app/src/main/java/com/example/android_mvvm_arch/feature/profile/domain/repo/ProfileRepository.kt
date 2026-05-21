package com.example.android_mvvm_arch.feature.profile.domain.repo

import com.example.android_mvvm_arch.feature.profile.domain.model.ProfileUpdate
import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun refreshProfile(): Result<UserProfile>
    suspend fun updateProfile(update: ProfileUpdate): Result<UserProfile>
    suspend fun clearProfileCache()
}
