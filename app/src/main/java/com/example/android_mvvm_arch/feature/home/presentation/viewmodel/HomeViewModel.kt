package com.example.android_mvvm_arch.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
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
            QuickAction("Profile", null, Routes.PROFILE),
            QuickAction("Settings", null, Routes.SETTINGS),
            QuickAction("Notifications", null, Routes.NOTIFICATIONS)
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

        // Refresh profile data
        viewModelScope.launch {
            getUserProfileUseCase.refresh()
        }
    }
    
    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }
            getUserProfileUseCase.refresh()
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "更新首頁資料失敗，請稍後再試。",
                        )
                    }
                }
        }
    }
}
