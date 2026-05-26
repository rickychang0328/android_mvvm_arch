package com.example.android_mvvm_arch.feature.notifications.domain.usecase

import com.example.android_mvvm_arch.feature.notifications.domain.repo.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUnreadCountUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    operator fun invoke(): Flow<Int> = notificationsRepository.observeUnreadCount()
}
