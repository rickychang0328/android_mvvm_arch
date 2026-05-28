package com.example.android_mvvm_arch.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.android_mvvm_arch.MainActivity
import com.example.android_mvvm_arch.R
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地系統通知統一入口。
 * - [createChannel] 由 Application 啟動時呼叫，僅需建立一次。
 * - [showNotification] 由 Worker 在拉取到新通知後呼叫，會帶上 deep link 至通知列表。
 *
 * Android 13+（API 33）需要先取得 [Manifest.permission.POST_NOTIFICATIONS] 才會實際顯示，
 * 權限請求由 UI 端透過 `rememberLauncherForActivityResult` 處理。
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }

    fun showNotification(notification: Notification) {
        showNotification(
            title = notification.title,
            body = notification.body,
            notificationId = notification.id.hashCode(),
            deepLinkNotificationId = notification.id,
        )
    }

    /**
     * 顯示來自 FCM 推播的通知（不含本地 domain 物件）。
     */
    fun showNotification(
        title: String,
        body: String,
        notificationId: Int = title.hashCode(),
        deepLinkNotificationId: String? = null,
    ) {
        if (!hasPostNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK, DEEP_LINK_NOTIFICATIONS)
            deepLinkNotificationId?.let { putExtra(EXTRA_NOTIFICATION_ID, it) }
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingFlags,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        NotificationManagerCompat.from(context)
            .notify(notificationId, builder.build())
    }

    private fun hasPostNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "general_notifications"
        private const val CHANNEL_NAME = "一般通知"
        private const val CHANNEL_DESCRIPTION = "系統公告、活動與行銷推播"

        const val EXTRA_DEEP_LINK = "extra_deep_link"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val DEEP_LINK_NOTIFICATIONS = "notifications"
    }
}
