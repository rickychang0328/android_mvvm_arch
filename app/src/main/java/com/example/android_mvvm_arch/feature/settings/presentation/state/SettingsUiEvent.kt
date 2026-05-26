package com.example.android_mvvm_arch.feature.settings.presentation.state

sealed interface SettingsUiEvent {
    data object NavigateBack : SettingsUiEvent
    data object NavigateToLogin : SettingsUiEvent
}
