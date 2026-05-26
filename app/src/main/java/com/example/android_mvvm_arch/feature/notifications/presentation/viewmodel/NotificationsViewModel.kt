package com.example.android_mvvm_arch.feature.notifications.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_mvvm_arch.core.network.ApiException
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.GetNotificationsUseCase
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.MarkAllNotificationsReadUseCase
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.MarkNotificationReadUseCase
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.RefreshNotificationsUseCase
import com.example.android_mvvm_arch.feature.notifications.presentation.state.NotificationsIntent
import com.example.android_mvvm_arch.feature.notifications.presentation.state.NotificationsUiEvent
import com.example.android_mvvm_arch.feature.notifications.presentation.state.NotificationsUiState
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
class NotificationsViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val refreshNotificationsUseCase: RefreshNotificationsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<NotificationsUiEvent>()
    val uiEvent: SharedFlow<NotificationsUiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            getNotificationsUseCase().collect { items ->
                _uiState.update { state ->
                    state.copy(
                        items = items,
                        isLoading = if (items.isNotEmpty()) false else state.isLoading,
                    )
                }
            }
        }
        refresh(initial = true)
    }

    fun onIntent(intent: NotificationsIntent) {
        when (intent) {
            NotificationsIntent.Load -> refresh(initial = true)
            NotificationsIntent.Refresh -> refresh(initial = false)
            is NotificationsIntent.MarkRead -> markRead(intent.id)
            NotificationsIntent.MarkAllRead -> markAllRead()
            NotificationsIntent.Retry -> refresh(initial = true)
        }
    }

    private fun refresh(initial: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = initial && state.items.isEmpty(),
                    isRefreshing = !initial,
                    errorMessage = null,
                )
            }
            refreshNotificationsUseCase()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                }
                .onFailure { error ->
                    val message = mapError(error, fallback = "載入通知失敗，請稍後再試。")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = message,
                        )
                    }
                    _uiEvent.emit(NotificationsUiEvent.ShowError(message))
                }
        }
    }

    private fun markRead(id: String) {
        viewModelScope.launch {
            markNotificationReadUseCase(id)
                .onFailure { error ->
                    val message = mapError(error, fallback = "標記為已讀失敗。")
                    _uiEvent.emit(NotificationsUiEvent.ShowError(message))
                }
        }
    }

    private fun markAllRead() {
        viewModelScope.launch {
            markAllNotificationsReadUseCase()
                .onSuccess {
                    _uiEvent.emit(NotificationsUiEvent.AllMarkedRead)
                }
                .onFailure { error ->
                    val message = mapError(error, fallback = "全部標記已讀失敗。")
                    _uiEvent.emit(NotificationsUiEvent.ShowError(message))
                }
        }
    }

    private fun mapError(error: Throwable, fallback: String): String = when (error) {
        is ApiException -> error.message
        is IllegalArgumentException -> error.message ?: fallback
        else -> fallback
    }
}
