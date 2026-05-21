package com.example.android_mvvm_arch.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_mvvm_arch.core.network.ApiException
import com.example.android_mvvm_arch.feature.auth.domain.usecase.ResetPasswordUseCase
import com.example.android_mvvm_arch.feature.auth.presentation.state.ResetPasswordIntent
import com.example.android_mvvm_arch.feature.auth.presentation.state.ResetPasswordUiEvent
import com.example.android_mvvm_arch.feature.auth.presentation.state.ResetPasswordUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ResetPasswordUiEvent>()
    val uiEvent: SharedFlow<ResetPasswordUiEvent> = _uiEvent.asSharedFlow()

    fun onIntent(intent: ResetPasswordIntent) {
        when (intent) {
            is ResetPasswordIntent.TokenChanged -> _uiState.update {
                it.copy(token = intent.token, errorMessage = null)
            }
            is ResetPasswordIntent.NewPasswordChanged -> _uiState.update {
                it.copy(newPassword = intent.newPassword, errorMessage = null)
            }
            is ResetPasswordIntent.ConfirmNewPasswordChanged -> _uiState.update {
                it.copy(confirmNewPassword = intent.confirmNewPassword, errorMessage = null)
            }
            ResetPasswordIntent.SubmitResetPassword -> submitResetPassword()
        }
    }

    private fun submitResetPassword() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.newPassword != state.confirmNewPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            resetPasswordUseCase(state.token, state.newPassword)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _uiEvent.emit(ResetPasswordUiEvent.NavigateToLogin)
                }
                .onFailure { error ->
                    val message = when (error) {
                        is ApiException -> error.message
                        is IllegalArgumentException -> error.message ?: "Invalid input."
                        else -> "Reset failed. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }
}
