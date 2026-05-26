package com.example.android_mvvm_arch.feature.notifications.domain.model

/**
 * 通知分類：系統公告、行銷推播、活動提醒。
 * 對應後端 `type` 欄位字串；未知值在 Mapper 中視為 [SYSTEM]。
 */
enum class NotificationType {
    SYSTEM,
    PROMOTION,
    ACTIVITY,
    ;

    companion object {
        fun fromRaw(raw: String?): NotificationType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SYSTEM
    }
}
