package com.example.android_mvvm_arch.feature.notifications.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.lifecycle.ViewModel
import com.example.android_mvvm_arch.core.network.ApiException
import com.example.android_mvvm_arch.core.sync.SyncManager
import com.example.android_mvvm_arch.core.sync.SyncTarget
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.GetNotificationsPagingUseCase
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.GetUnreadCountUseCase
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.MarkAllNotificationsReadUseCase
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.MarkNotificationReadUseCase
import com.example.android_mvvm_arch.feature.notifications.presentation.state.NotificationsIntent
import com.example.android_mvvm_arch.feature.notifications.presentation.state.NotificationsUiEvent
import com.example.android_mvvm_arch.feature.notifications.presentation.state.NotificationsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    getNotificationsPagingUseCase: GetNotificationsPagingUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
    private val syncManager: SyncManager,
) : ViewModel() {

    val pagingDataFlow: Flow<PagingData<Notification>> =
        getNotificationsPagingUseCase().cachedIn(viewModelScope)

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<NotificationsUiEvent>()
    val uiEvent: SharedFlow<NotificationsUiEvent> = _uiEvent.asSharedFlow()

    private val unreadCountFlow = getUnreadCountUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            unreadCountFlow.collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    fun onIntent(intent: NotificationsIntent) {
        when (intent) {
            NotificationsIntent.Load,
            NotificationsIntent.Refresh -> emitRefreshList()
            is NotificationsIntent.MarkRead -> markRead(intent.id)
            NotificationsIntent.MarkAllRead -> markAllRead()
            NotificationsIntent.Retry -> emitRetryList()
        }
    }

    private fun emitRefreshList() {
        viewModelScope.launch {
            syncManager.requestImmediateSync(setOf(SyncTarget.NOTIFICATIONS))
            _uiEvent.emit(NotificationsUiEvent.RefreshList)
        }
    }

    private fun emitRetryList() {
        viewModelScope.launch {
            _uiEvent.emit(NotificationsUiEvent.RetryList)
        }
    }

    private fun markRead(id: String) {
        viewModelScope.launch {
            markNotificationReadUseCase(id)
                .onSuccess {
                    _uiEvent.emit(NotificationsUiEvent.RefreshList)
                }
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
                    _uiEvent.emit(NotificationsUiEvent.RefreshList)
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
