package com.example.android_mvvm_arch.feature.settings.presentation.state

sealed interface SettingsIntent {
    data class DarkModeChanged(val enabled: Boolean) : SettingsIntent
    data class LanguageChanged(val language: String) : SettingsIntent
    data class NotificationsChanged(val enabled: Boolean) : SettingsIntent
    data class UpdateAnalytics(val enabled: Boolean) : SettingsIntent
    data class UpdateCrashReporting(val enabled: Boolean) : SettingsIntent
    data class UpdatePersonalizedAds(val enabled: Boolean) : SettingsIntent
    data class UpdateBiometricLogin(val enabled: Boolean) : SettingsIntent
    data object ClearCache : SettingsIntent
    data object Logout : SettingsIntent
}
