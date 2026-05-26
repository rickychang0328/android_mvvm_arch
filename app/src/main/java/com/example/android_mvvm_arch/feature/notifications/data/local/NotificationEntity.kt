package com.example.android_mvvm_arch.feature.notifications.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 通知 Room Entity。`type` 以 String 儲存（對應 [NotificationType] enum 名稱）。
 * `createdAt` 為 epoch millis；後續查詢以此欄位排序。
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: Long,
)
