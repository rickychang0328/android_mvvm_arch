package com.example.android_mvvm_arch.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_mvvm_arch.core.network.ApiException
import com.example.android_mvvm_arch.feature.auth.domain.usecase.RegisterUseCase
import com.example.android_mvvm_arch.feature.auth.presentation.state.RegisterIntent
import com.example.android_mvvm_arch.feature.auth.presentation.state.RegisterUiEvent
import com.example.android_mvvm_arch.feature.auth.presentation.state.RegisterUiState
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
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RegisterUiEvent>()
    val uiEvent: SharedFlow<RegisterUiEvent> = _uiEvent.asSharedFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.EmailChanged -> _uiState.update {
                it.copy(email = intent.email, errorMessage = null)
            }
            is RegisterIntent.PasswordChanged -> _uiState.update {
                it.copy(password = intent.password, errorMessage = null)
            }
            is RegisterIntent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = intent.confirmPassword, errorMessage = null)
            }
            is RegisterIntent.DisplayNameChanged -> _uiState.update {
                it.copy(displayName = intent.displayName, errorMessage = null)
            }
            RegisterIntent.SubmitRegister -> submitRegister()
        }
    }

    private fun submitRegister() {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            registerUseCase(
                email = state.email,
                password = state.password,
                confirmPassword = state.confirmPassword,
                displayName = state.displayName,
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(RegisterUiEvent.NavigateToProfile)
                }
                .onFailure { error ->
                    val message = when (error) {
                        is ApiException -> error.message
                        is IllegalArgumentException -> error.message ?: "Invalid input."
                        else -> "Registration failed. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }
}
