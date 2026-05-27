package com.example.android_mvvm_arch.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.android_mvvm_arch.core.sync.SyncManager
import com.example.android_mvvm_arch.core.sync.SyncTarget
import com.example.android_mvvm_arch.feature.home.presentation.state.HomeUiState
import com.example.android_mvvm_arch.feature.home.presentation.state.QuickAction
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.GetNotificationsPagingUseCase
import com.example.android_mvvm_arch.feature.profile.domain.usecase.GetUserProfileUseCase
import com.example.android_mvvm_arch.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    getNotificationsPagingUseCase: GetNotificationsPagingUseCase,
    private val syncManager: SyncManager,
) : ViewModel() {

    val recentNotificationsPagingDataFlow: Flow<PagingData<Notification>> =
        getNotificationsPagingUseCase().cachedIn(viewModelScope)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val actions = listOf(
            QuickAction("個人資料", null, Routes.PROFILE),
            QuickAction("設定", null, Routes.SETTINGS),
            QuickAction("通知", null, Routes.NOTIFICATIONS)
        )
        _uiState.update { it.copy(quickActions = actions) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Observe profile
            getUserProfileUseCase.observeProfile().collect { profile ->
                _uiState.update {
                    it.copy(
                        userProfile = profile,
                        isLoading = false,
                    )
                }
            }
        }

        syncManager.requestImmediateSync(setOf(SyncTarget.PROFILE, SyncTarget.NOTIFICATIONS))
    }
    
    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }
            syncManager.requestImmediateSync(setOf(SyncTarget.PROFILE))
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = null,
                )
            }
        }
    }
}
