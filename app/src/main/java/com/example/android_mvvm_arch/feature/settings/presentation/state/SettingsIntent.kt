package com.example.android_mvvm_arch.feature.settings.presentation.state

sealed interface SettingsIntent {
    data class DarkModeChanged(val enabled: Boolean) : SettingsIntent
    data class LanguageChanged(val language: String) : SettingsIntent
    data class NotificationsChanged(val enabled: Boolean) : SettingsIntent
    data object Logout : SettingsIntent
}
