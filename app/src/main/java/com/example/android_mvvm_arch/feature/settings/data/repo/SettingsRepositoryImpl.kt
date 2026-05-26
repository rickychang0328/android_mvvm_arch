package com.example.android_mvvm_arch.feature.settings.data.repo

import com.example.android_mvvm_arch.core.datastore.AppSettings
import com.example.android_mvvm_arch.core.datastore.SettingsDataStore
import com.example.android_mvvm_arch.feature.settings.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow

    override suspend fun updateDarkMode(enabled: Boolean) {
        settingsDataStore.updateDarkMode(enabled)
    }

    override suspend fun updateLanguage(language: String) {
        settingsDataStore.updateLanguage(language)
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        settingsDataStore.updateNotificationsEnabled(enabled)
    }
}
