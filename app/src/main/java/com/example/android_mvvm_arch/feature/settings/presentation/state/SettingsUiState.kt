package com.example.android_mvvm_arch.feature.settings.presentation.state

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val language: String = "zh-TW",
    val notificationsEnabled: Boolean = true,
    val errorMessage: String? = null,
)
