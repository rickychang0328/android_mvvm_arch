package com.example.android_mvvm_arch.feature.auth.presentation.state

sealed interface ForgotPasswordIntent {
    data class EmailChanged(val email: String) : ForgotPasswordIntent
    data object SubmitForgotPassword : ForgotPasswordIntent
}
