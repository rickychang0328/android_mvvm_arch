package com.example.android_mvvm_arch.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_mvvm_arch.core.network.ApiException
import com.example.android_mvvm_arch.feature.auth.domain.usecase.LogoutUseCase
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.GetUnreadCountUseCase
import com.example.android_mvvm_arch.feature.profile.domain.usecase.GetUserProfileUseCase
import com.example.android_mvvm_arch.feature.profile.domain.usecase.UpdateUserProfileUseCase
import com.example.android_mvvm_arch.feature.profile.domain.usecase.UploadAvatarUseCase
import com.example.android_mvvm_arch.feature.profile.presentation.state.ProfileIntent
import com.example.android_mvvm_arch.feature.profile.presentation.state.ProfileUiEvent
import com.example.android_mvvm_arch.feature.profile.presentation.state.ProfileUiState
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
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent: SharedFlow<ProfileUiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            getUserProfileUseCase.observeProfile().collect { profile ->
                _uiState.update { state ->
                    if (state.isEditing) {
                        state.copy(profile = profile)
                    } else {
                        state.copy(
                            profile = profile,
                            displayName = profile?.displayName.orEmpty(),
                            phone = profile?.phone.orEmpty(),
                            bio = profile?.bio.orEmpty(),
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            getUnreadCountUseCase().collect { count ->
                _uiState.update { it.copy(unreadNotificationsCount = count) }
            }
        }
        refreshProfile()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Refresh -> refreshProfile()
            ProfileIntent.StartEditing -> _uiState.update {
                it.copy(isEditing = true, errorMessage = null, successMessage = null)
            }
            ProfileIntent.CancelEditing -> _uiState.update { state ->
                state.copy(
                    isEditing = false,
                    displayName = state.profile?.displayName.orEmpty(),
                    phone = state.profile?.phone.orEmpty(),
                    bio = state.profile?.bio.orEmpty(),
                    errorMessage = null,
                )
            }
            is ProfileIntent.DisplayNameChanged -> _uiState.update {
                it.copy(displayName = intent.value, errorMessage = null)
            }
            is ProfileIntent.PhoneChanged -> _uiState.update {
                it.copy(phone = intent.value, errorMessage = null)
            }
            is ProfileIntent.BioChanged -> _uiState.update {
                it.copy(bio = intent.value, errorMessage = null)
            }
            ProfileIntent.SaveProfile -> saveProfile()
            ProfileIntent.Logout -> logout()
            is ProfileIntent.UploadAvatar -> uploadAvatar(intent.file)
        }
    }

    private fun refreshProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getUserProfileUseCase.refresh()
                .onFailure { error ->
                    val message = when (error) {
                        is ApiException -> error.message
                        else -> "Failed to load profile."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            updateUserProfileUseCase(state.displayName, state.phone, state.bio)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            profile = profile,
                            successMessage = "Profile updated.",
                        )
                    }
                }
                .onFailure { error ->
                    val message = when (error) {
                        is ApiException -> error.message
                        is IllegalArgumentException -> error.message ?: "Invalid input."
                        else -> "Failed to update profile."
                    }
                    _uiState.update { it.copy(isSaving = false, errorMessage = message) }
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiEvent.emit(ProfileUiEvent.NavigateToLogin)
        }
    }

    private fun uploadAvatar(file: java.io.File) {
        if (_uiState.value.isUploadingAvatar) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingAvatar = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }
            uploadAvatarUseCase(file)
                .onSuccess { profile ->
                    _uiState.update { state ->
                        val base = state.copy(
                            isUploadingAvatar = false,
                            profile = profile,
                            successMessage = "頭像已更新",
                        )
                        if (state.isEditing) {
                            base
                        } else {
                            base.copy(
                                displayName = profile.displayName,
                                phone = profile.phone.orEmpty(),
                                bio = profile.bio.orEmpty(),
                            )
                        }
                    }
                    _uiEvent.emit(ProfileUiEvent.ShowMessage("頭像已更新"))
                }
                .onFailure { error ->
                    val message = when (error) {
                        is ApiException -> error.message
                        is IllegalArgumentException -> error.message
                        else -> "頭像上傳失敗，請稍後再試。"
                    }
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            errorMessage = message,
                        )
                    }
                    _uiEvent.emit(
                        ProfileUiEvent.ShowMessage(
                            message ?: "頭像上傳失敗，請稍後再試。",
                        ),
                    )
                }
        }
    }
}
