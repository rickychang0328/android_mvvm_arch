package com.example.android_mvvm_arch.feature.home.presentation.state

import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile

data class HomeUiState(
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val quickActions: List<QuickAction> = emptyList(),
)

data class QuickAction(
    val title: String,
    val icon: Int? = null, // Using vector icons instead of raw resource IDs if possible
    val route: String,
)
