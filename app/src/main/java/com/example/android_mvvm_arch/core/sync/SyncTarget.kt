package com.example.android_mvvm_arch.core.sync

/**
 * 定義可被排程/即時觸發的同步目標。
 */
enum class SyncTarget {
    PROFILE,
    NOTIFICATIONS,
    ;

    companion object {
        val defaultTargets: Set<SyncTarget> = entries.toSet()

        fun fromToken(token: String): SyncTarget? =
            entries.firstOrNull { it.name.equals(token.trim(), ignoreCase = true) }
    }
}
