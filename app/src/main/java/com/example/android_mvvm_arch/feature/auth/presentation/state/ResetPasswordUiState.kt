package com.example.android_mvvm_arch.feature.auth.presentation.state

data class ResetPasswordUiState(
    val token: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)
