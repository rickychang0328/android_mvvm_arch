package com.example.android_mvvm_arch.feature.notifications.domain.model

/**
 * 通知業務模型（Domain Layer）。
 * `createdAt` 為 epoch millis，UI 以相對時間呈現。
 */
data class Notification(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val isRead: Boolean,
    val createdAt: Long,
)
