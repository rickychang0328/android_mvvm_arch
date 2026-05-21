package com.example.android_mvvm_arch.feature.auth.presentation.state

sealed interface ForgotPasswordUiEvent {
    data class ShowMessage(val message: String) : ForgotPasswordUiEvent
}
