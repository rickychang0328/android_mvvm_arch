package com.example.android_mvvm_arch.core.sync

interface SyncManager {
    fun schedulePeriodicSync()

    fun requestImmediateSync(targets: Set<SyncTarget> = SyncTarget.defaultTargets)

    suspend fun runSync(targets: Set<SyncTarget>): SyncResult
}
