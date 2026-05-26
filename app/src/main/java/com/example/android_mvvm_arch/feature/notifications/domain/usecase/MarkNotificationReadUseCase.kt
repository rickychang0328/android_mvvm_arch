package com.example.android_mvvm_arch.feature.notifications.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.notifications.domain.repo.NotificationsRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MarkNotificationReadUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(id: String): Result<Unit> = withContext(dispatcherProvider.io) {
        if (id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Notification id cannot be empty."))
        }
        notificationsRepository.markAsRead(id)
    }
}
