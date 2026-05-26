package com.example.android_mvvm_arch.feature.notifications.presentation.ui

import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * 將 epoch millis 轉為「N 分鐘前」等繁體中文相對時間字串。
 * 30 天以前的時間以「N 天前」呈現，不再進一步換算月份，避免月長差異造成混淆。
 */
internal fun formatRelativeTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val diff = max(0L, now - createdAt)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    if (seconds < 60) return "剛剛"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    if (minutes < 60) return "${minutes} 分鐘前"
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    if (hours < 24) return "${hours} 小時前"
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return "${days} 天前"
}
