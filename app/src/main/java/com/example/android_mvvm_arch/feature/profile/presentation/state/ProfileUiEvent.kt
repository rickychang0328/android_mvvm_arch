package com.example.android_mvvm_arch.feature.profile.presentation.state

sealed interface ProfileUiEvent {
    data object NavigateToLogin : ProfileUiEvent
}
