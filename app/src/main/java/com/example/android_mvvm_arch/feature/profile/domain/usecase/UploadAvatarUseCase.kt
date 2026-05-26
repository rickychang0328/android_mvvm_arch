package com.example.android_mvvm_arch.feature.profile.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile
import com.example.android_mvvm_arch.feature.profile.domain.repo.ProfileRepository
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class UploadAvatarUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(image: File): Result<UserProfile> = withContext(dispatcherProvider.io) {
        if (!image.exists() || image.length() <= 0) {
            return@withContext Result.failure(IllegalArgumentException("請選擇有效的圖片檔案。"))
        }
        profileRepository.uploadAvatar(image)
    }
}
