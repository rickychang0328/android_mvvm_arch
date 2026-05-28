package com.example.android_mvvm_arch.core.fcm

import android.util.Log
import com.example.android_mvvm_arch.core.notification.NotificationHelper
import com.example.android_mvvm_arch.feature.auth.domain.repo.AuthRepository
import com.example.android_mvvm_arch.feature.auth.domain.usecase.RegisterFcmTokenUseCase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service。
 *
 * - [onNewToken]：Firebase 重新核發 token 時，若使用者已登入則同步上報後端。
 * - [onMessageReceived]：收到推播訊息時，透過 [NotificationHelper] 顯示本地通知。
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var registerFcmTokenUseCase: RegisterFcmTokenUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        serviceScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            if (isLoggedIn) {
                authRepository.registerFcmToken(token)
                    .onFailure { Log.w(TAG, "Failed to upload refreshed FCM token", it) }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: ""
        notificationHelper.showNotification(title = title, body = body)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "FcmService"
    }
}
