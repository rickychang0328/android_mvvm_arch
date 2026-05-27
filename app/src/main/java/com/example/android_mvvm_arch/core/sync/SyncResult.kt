package com.example.android_mvvm_arch.core.sync

/**
 * 同步執行結果彙整，供 Worker 決定是否 retry。
 */
data class SyncResult(
    val succeeded: Set<SyncTarget> = emptySet(),
    val failed: Map<SyncTarget, Throwable> = emptyMap(),
    val skipped: Set<SyncTarget> = emptySet(),
    val shouldRetry: Boolean = false,
) {
    val isSuccess: Boolean
        get() = failed.isEmpty()
}
