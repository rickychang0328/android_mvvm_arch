package com.example.android_mvvm_arch.feature.auth.presentation.state

sealed interface RegisterIntent {
    data class EmailChanged(val email: String) : RegisterIntent
    data class PasswordChanged(val password: String) : RegisterIntent
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterIntent
    data class DisplayNameChanged(val displayName: String) : RegisterIntent
    data object SubmitRegister : RegisterIntent
}
