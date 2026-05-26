package com.example.android_mvvm_arch.feature.settings.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.profile.domain.repo.ProfileRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 清除本地快取資料：清空 Profile Room 快取，但保留 Token 與 Settings。
 */
class ClearCacheUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(): Result<Unit> = withContext(dispatcherProvider.io) {
        runCatching { profileRepository.clearProfileCache() }
    }
}
