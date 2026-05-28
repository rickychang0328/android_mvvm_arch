package com.example.android_mvvm_arch.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_mvvm_arch.core.network.ApiException
import com.example.android_mvvm_arch.core.sync.SyncManager
import com.example.android_mvvm_arch.core.sync.SyncTarget
import com.example.android_mvvm_arch.feature.auth.domain.usecase.LoginUseCase
import com.example.android_mvvm_arch.feature.auth.domain.usecase.RegisterFcmTokenUseCase
import com.example.android_mvvm_arch.feature.auth.presentation.state.LoginIntent
import com.example.android_mvvm_arch.feature.auth.presentation.state.LoginUiEvent
import com.example.android_mvvm_arch.feature.auth.presentation.state.LoginUiState
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
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent: SharedFlow<LoginUiEvent> = _uiEvent.asSharedFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> _uiState.update {
                it.copy(email = intent.email, errorMessage = null)
            }
            is LoginIntent.PasswordChanged -> _uiState.update {
                it.copy(password = intent.password, errorMessage = null)
            }
            LoginIntent.SubmitLogin -> submitLogin()
        }
    }

    private fun submitLogin() {
        val state = _uiState.value
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loginUseCase(state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    // FCM token 上報為 fire-and-forget，失敗不中斷登入流程
                    launch { registerFcmTokenUseCase() }
                    syncManager.requestImmediateSync(
                        setOf(
                            SyncTarget.PROFILE,
                            SyncTarget.NOTIFICATIONS,
                        ),
                    )
                    _uiEvent.emit(LoginUiEvent.NavigateToHome)
                }
                .onFailure { error ->
                    val message = when (error) {
                        is ApiException -> error.message
                        is IllegalArgumentException -> error.message ?: "Invalid input."
                        else -> "Login failed. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }
}
