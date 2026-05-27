package com.example.android_mvvm_arch

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.android_mvvm_arch.core.notification.NotificationHelper
import com.example.android_mvvm_arch.core.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 應用程式入口：
 * 1. 透過 [HiltAndroidApp] 啟動 Hilt 依賴注入容器。
 * 2. 實作 [Configuration.Provider] 並注入 [HiltWorkerFactory]，使 WorkManager 能解析 `@HiltWorker`。
 * 3. 啟動時建立通知 channel 並註冊 Offline-first 週期同步排程。
 */
@HiltAndroidApp
class AndroidMvvmArchApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var syncManager: SyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
        syncManager.schedulePeriodicSync()
    }
}
