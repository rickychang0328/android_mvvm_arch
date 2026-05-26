package com.example.android_mvvm_arch.feature.notifications.presentation.state

import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<Notification> = emptyList(),
    val errorMessage: String? = null,
)
