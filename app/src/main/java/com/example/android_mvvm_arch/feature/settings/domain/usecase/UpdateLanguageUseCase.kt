package com.example.android_mvvm_arch.feature.settings.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.settings.domain.repo.SettingsRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateLanguageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(language: String): Result<Unit> = withContext(dispatcherProvider.io) {
        if (language !in SUPPORTED_LANGUAGES) {
            return@withContext Result.failure(
                IllegalArgumentException("Unsupported language. Allowed values: zh-TW, en."),
            )
        }
        settingsRepository.updateLanguage(language)
        Result.success(Unit)
    }

    private companion object {
        val SUPPORTED_LANGUAGES = setOf("zh-TW", "en")
    }
}
