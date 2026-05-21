package com.example.android_mvvm_arch.feature.auth.presentation.state

sealed interface ResetPasswordIntent {
    data class TokenChanged(val token: String) : ResetPasswordIntent
    data class NewPasswordChanged(val newPassword: String) : ResetPasswordIntent
    data class ConfirmNewPasswordChanged(val confirmNewPassword: String) : ResetPasswordIntent
    data object SubmitResetPassword : ResetPasswordIntent
}
