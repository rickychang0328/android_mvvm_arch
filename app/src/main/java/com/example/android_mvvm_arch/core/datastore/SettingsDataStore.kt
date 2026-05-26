package com.example.android_mvvm_arch.core.datastore

import kotlinx.coroutines.flow.Flow

interface SettingsDataStore {
    val settingsFlow: Flow<AppSettings>
    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun updateLanguage(language: String)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
}
