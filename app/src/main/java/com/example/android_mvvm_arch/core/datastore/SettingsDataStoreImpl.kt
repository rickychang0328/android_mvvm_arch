package com.example.android_mvvm_arch.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

@Singleton
class SettingsDataStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsDataStore {

    private val dataStore = context.settingsDataStore

    override val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            isDarkMode = preferences[IS_DARK_MODE] ?: false,
            language = preferences[LANGUAGE] ?: "zh-TW",
            notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
            analyticsEnabled = preferences[ANALYTICS_ENABLED] ?: true,
            crashReportingEnabled = preferences[CRASH_REPORTING_ENABLED] ?: true,
            personalizedAdsEnabled = preferences[PERSONALIZED_ADS_ENABLED] ?: false,
            biometricLoginEnabled = preferences[BIOMETRIC_LOGIN_ENABLED] ?: false,
        )
    }

    override suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = enabled
        }
    }

    override suspend fun updateLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun updateAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ANALYTICS_ENABLED] = enabled
        }
    }

    override suspend fun updateCrashReportingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CRASH_REPORTING_ENABLED] = enabled
        }
    }

    override suspend fun updatePersonalizedAdsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PERSONALIZED_ADS_ENABLED] = enabled
        }
    }

    override suspend fun updateBiometricLoginEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BIOMETRIC_LOGIN_ENABLED] = enabled
        }
    }

    private companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
        val PERSONALIZED_ADS_ENABLED = booleanPreferencesKey("personalized_ads_enabled")
        val BIOMETRIC_LOGIN_ENABLED = booleanPreferencesKey("biometric_login_enabled")
    }
}
