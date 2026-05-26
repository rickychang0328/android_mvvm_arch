package com.example.android_mvvm_arch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_mvvm_arch.core.auth.AuthEvent
import com.example.android_mvvm_arch.core.auth.AuthEventBus
import com.example.android_mvvm_arch.feature.auth.domain.usecase.IsLoggedInUseCase
import com.example.android_mvvm_arch.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val isLoggedInUseCase: IsLoggedInUseCase,
    private val authEventBus: AuthEventBus,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val destination = if (isLoggedInUseCase()) Routes.HOME else Routes.LOGIN
            _startDestination.value = destination
        }
        viewModelScope.launch {
            authEventBus.events.collect { event ->
                when (event) {
                    AuthEvent.ForceLogout -> _startDestination.value = Routes.LOGIN
                }
            }
        }
    }
}
