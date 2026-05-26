package com.example.android_mvvm_arch.feature.notifications.presentation.state

sealed interface NotificationsUiEvent {
    data class ShowError(val message: String) : NotificationsUiEvent
    data object AllMarkedRead : NotificationsUiEvent
    data object RefreshList : NotificationsUiEvent
    data object RetryList : NotificationsUiEvent
}
