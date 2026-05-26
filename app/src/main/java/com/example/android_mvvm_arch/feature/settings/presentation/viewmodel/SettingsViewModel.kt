package com.example.android_mvvm_arch.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_mvvm_arch.feature.auth.domain.usecase.LogoutUseCase
import com.example.android_mvvm_arch.feature.settings.domain.usecase.ClearCacheUseCase
import com.example.android_mvvm_arch.feature.settings.domain.usecase.GetAppSettingsUseCase
import com.example.android_mvvm_arch.feature.settings.domain.usecase.UpdateAnalyticsUseCase
import com.example.android_mvvm_arch.feature.settings.domain.usecase.UpdateBiometricLoginUseCase
import com.example.android_mvvm_arch.feature.settings.domain.usecase.UpdateCrashReportingUseCase
import com.example.android_mvvm_arch.feature.settings.domain.usecase.UpdateDarkModeUseCase
import com.example.android_mvvm_arch.feature.settings.domain.usecase.UpdateLanguageUseCase
import com.example.android_mvvm_arch.feature.settings.domain.usecase.UpdateNotificationsUseCase
import com.example.android_mvvm_arch.feature.settings.domain.usecase.UpdatePersonalizedAdsUseCase
import com.example.android_mvvm_arch.feature.settings.presentation.state.SettingsIntent
import com.example.android_mvvm_arch.feature.settings.presentation.state.SettingsUiEvent
import com.example.android_mvvm_arch.feature.settings.presentation.state.SettingsUiState
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
class SettingsViewModel @Inject constructor(
    private val getAppSettingsUseCase: GetAppSettingsUseCase,
    private val updateDarkModeUseCase: UpdateDarkModeUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase,
    private val updateNotificationsUseCase: UpdateNotificationsUseCase,
    private val updateAnalyticsUseCase: UpdateAnalyticsUseCase,
    private val updateCrashReportingUseCase: UpdateCrashReportingUseCase,
    private val updatePersonalizedAdsUseCase: UpdatePersonalizedAdsUseCase,
    private val updateBiometricLoginUseCase: UpdateBiometricLoginUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SettingsUiEvent>()
    val uiEvent: SharedFlow<SettingsUiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            getAppSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        isDarkMode = settings.isDarkMode,
                        language = settings.language,
                        notificationsEnabled = settings.notificationsEnabled,
                        analyticsEnabled = settings.analyticsEnabled,
                        crashReportingEnabled = settings.crashReportingEnabled,
                        personalizedAdsEnabled = settings.personalizedAdsEnabled,
                        biometricLoginEnabled = settings.biometricLoginEnabled,
                    )
                }
            }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.DarkModeChanged -> updateDarkMode(intent.enabled)
            is SettingsIntent.LanguageChanged -> updateLanguage(intent.language)
            is SettingsIntent.NotificationsChanged -> updateNotifications(intent.enabled)
            is SettingsIntent.UpdateAnalytics -> updateAnalytics(intent.enabled)
            is SettingsIntent.UpdateCrashReporting -> updateCrashReporting(intent.enabled)
            is SettingsIntent.UpdatePersonalizedAds -> updatePersonalizedAds(intent.enabled)
            is SettingsIntent.UpdateBiometricLogin -> updateBiometricLogin(intent.enabled)
            SettingsIntent.ClearCache -> clearCache()
            SettingsIntent.Logout -> logout()
        }
    }

    private fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            updateDarkModeUseCase(enabled)
                .onFailure { error ->
                    val message = when (error) {
                        is IllegalArgumentException -> error.message ?: "Invalid input."
                        else -> "Failed to update dark mode."
                    }
                    _uiState.update { it.copy(errorMessage = message) }
                }
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                }
        }
    }

    private fun updateLanguage(language: String) {
        viewModelScope.launch {
            updateLanguageUseCase(language)
                .onFailure { error ->
                    val message = when (error) {
                        is IllegalArgumentException -> error.message ?: "Invalid language."
                        else -> "Failed to update language."
                    }
                    _uiState.update { it.copy(errorMessage = message) }
                }
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                }
        }
    }

    private fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            updateNotificationsUseCase(enabled)
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "Failed to update notifications.") }
                }
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                }
        }
    }

    private fun updateAnalytics(enabled: Boolean) {
        viewModelScope.launch {
            updateAnalyticsUseCase(enabled)
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "更新使用分析設定失敗。") }
                }
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                }
        }
    }

    private fun updateCrashReporting(enabled: Boolean) {
        viewModelScope.launch {
            updateCrashReportingUseCase(enabled)
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "更新當機回報設定失敗。") }
                }
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                }
        }
    }

    private fun updatePersonalizedAds(enabled: Boolean) {
        viewModelScope.launch {
            updatePersonalizedAdsUseCase(enabled)
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "更新個人化廣告設定失敗。") }
                }
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                }
        }
    }

    private fun updateBiometricLogin(enabled: Boolean) {
        viewModelScope.launch {
            updateBiometricLoginUseCase(enabled)
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "更新生物辨識登入設定失敗。") }
                }
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                }
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            clearCacheUseCase()
                .onSuccess {
                    _uiState.update { it.copy(errorMessage = null) }
                    _uiEvent.emit(SettingsUiEvent.CacheCleared)
                }
                .onFailure {
                    _uiEvent.emit(SettingsUiEvent.ShowError("清除快取失敗，請稍後再試。"))
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiEvent.emit(SettingsUiEvent.NavigateToLogin)
        }
    }
}
