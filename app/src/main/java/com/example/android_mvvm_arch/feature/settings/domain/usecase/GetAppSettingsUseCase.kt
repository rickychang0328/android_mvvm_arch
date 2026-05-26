package com.example.android_mvvm_arch.feature.settings.domain.usecase

import com.example.android_mvvm_arch.core.datastore.AppSettings
import com.example.android_mvvm_arch.feature.settings.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.settingsFlow
}
