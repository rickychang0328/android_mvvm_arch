package com.example.android_mvvm_arch.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_mvvm_arch.core.network.ApiException
import com.example.android_mvvm_arch.feature.auth.domain.usecase.ForgotPasswordUseCase
import com.example.android_mvvm_arch.feature.auth.presentation.state.ForgotPasswordIntent
import com.example.android_mvvm_arch.feature.auth.presentation.state.ForgotPasswordUiEvent
import com.example.android_mvvm_arch.feature.auth.presentation.state.ForgotPasswordUiState
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
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ForgotPasswordUiEvent>()
    val uiEvent: SharedFlow<ForgotPasswordUiEvent> = _uiEvent.asSharedFlow()

    fun onIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.EmailChanged -> _uiState.update {
                it.copy(email = intent.email, errorMessage = null)
            }
            ForgotPasswordIntent.SubmitForgotPassword -> submitForgotPassword()
        }
    }

    private fun submitForgotPassword() {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            forgotPasswordUseCase(state.email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    val message = when (error) {
                        is ApiException -> error.message
                        is IllegalArgumentException -> error.message ?: "Invalid input."
                        else -> "Request failed. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }
}
