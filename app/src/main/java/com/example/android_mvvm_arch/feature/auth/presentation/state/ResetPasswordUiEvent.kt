package com.example.android_mvvm_arch.feature.auth.presentation.state

sealed interface ResetPasswordUiEvent {
    data object NavigateToLogin : ResetPasswordUiEvent
    data class ShowMessage(val message: String) : ResetPasswordUiEvent
}
