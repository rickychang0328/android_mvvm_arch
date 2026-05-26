package com.example.android_mvvm_arch.feature.settings.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.settings.domain.repo.SettingsRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateDarkModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = withContext(dispatcherProvider.io) {
        settingsRepository.updateDarkMode(enabled)
        Result.success(Unit)
    }
}
