package com.example.android_mvvm_arch.feature.settings.domain.repo

import com.example.android_mvvm_arch.core.datastore.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun updateLanguage(language: String)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateAnalyticsEnabled(enabled: Boolean)
    suspend fun updateCrashReportingEnabled(enabled: Boolean)
    suspend fun updatePersonalizedAdsEnabled(enabled: Boolean)
    suspend fun updateBiometricLoginEnabled(enabled: Boolean)
}
