package com.example.android_mvvm_arch.feature.profile.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.profile.domain.model.ProfileUpdate
import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile
import com.example.android_mvvm_arch.feature.profile.domain.repo.ProfileRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(
        displayName: String,
        phone: String,
        bio: String,
    ): Result<UserProfile> = withContext(dispatcherProvider.io) {
        if (displayName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Display name cannot be empty."))
        }
        if (displayName.length > 50) {
            return@withContext Result.failure(IllegalArgumentException("Display name must be 50 characters or less."))
        }
        if (bio.length > 200) {
            return@withContext Result.failure(IllegalArgumentException("Bio must be 200 characters or less."))
        }
        profileRepository.updateProfile(
            ProfileUpdate(
                displayName = displayName.trim(),
                phone = phone.trim().ifBlank { null },
                bio = bio.trim().ifBlank { null },
            ),
        )
    }
}
