package com.example.android_mvvm_arch.feature.profile.presentation.state

import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile

data class ProfileUiState(
    val profile: UserProfile? = null,
    val displayName: String = "",
    val phone: String = "",
    val bio: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val unreadNotificationsCount: Int = 0,
)
