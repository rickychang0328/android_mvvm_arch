package com.example.android_mvvm_arch.feature.notifications.presentation.state

sealed interface NotificationsIntent {
    data object Load : NotificationsIntent
    data object Refresh : NotificationsIntent
    data class MarkRead(val id: String) : NotificationsIntent
    data object MarkAllRead : NotificationsIntent
    data object Retry : NotificationsIntent
}
