package com.example.android_mvvm_arch.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_mvvm_arch.feature.home.presentation.state.HomeUiState
import com.example.android_mvvm_arch.feature.home.presentation.state.QuickAction
import com.example.android_mvvm_arch.feature.profile.domain.usecase.GetUserProfileUseCase
import com.example.android_mvvm_arch.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Setup quick actions
            val actions = listOf(
                QuickAction("Profile", null, Routes.PROFILE),
                QuickAction("Settings", null, Routes.SETTINGS),
                QuickAction("Notifications", null, Routes.NOTIFICATIONS)
            )
            _uiState.update { it.copy(quickActions = actions) }

            // Observe profile
            getUserProfileUseCase.observeProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile, isLoading = false) }
            }
        }
        
        // Refresh profile data
        viewModelScope.launch {
            getUserProfileUseCase.refresh()
        }
    }
    
    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getUserProfileUseCase.refresh().onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false) }
            }
        }
    }
}
