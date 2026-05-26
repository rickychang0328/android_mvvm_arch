package com.example.android_mvvm_arch.feature.auth.presentation.state

sealed interface LoginUiEvent {
    data object NavigateToHome : LoginUiEvent
    data class ShowMessage(val message: String) : LoginUiEvent
}
