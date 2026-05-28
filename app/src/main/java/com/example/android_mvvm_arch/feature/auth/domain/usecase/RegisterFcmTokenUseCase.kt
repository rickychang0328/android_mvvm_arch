package com.example.android_mvvm_arch.feature.auth.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.auth.domain.repo.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 取得裝置最新的 FCM token，並上報至後端 API。
 *
 * 若 Firebase 尚未初始化（例如尚未放置 google-services.json），
 * 方法將以失敗形式回傳，不影響登入主流程。
 */
class RegisterFcmTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(): Result<Unit> = withContext(dispatcherProvider.io) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            authRepository.registerFcmToken(token).getOrThrow()
        }
    }
}
