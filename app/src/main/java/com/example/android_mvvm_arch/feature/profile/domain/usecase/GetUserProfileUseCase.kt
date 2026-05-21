package com.example.android_mvvm_arch.feature.profile.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile
import com.example.android_mvvm_arch.feature.profile.domain.repo.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    fun observeProfile(): Flow<UserProfile?> = profileRepository.observeProfile()

    suspend fun refresh(): Result<UserProfile> = withContext(dispatcherProvider.io) {
        profileRepository.refreshProfile()
    }
}
