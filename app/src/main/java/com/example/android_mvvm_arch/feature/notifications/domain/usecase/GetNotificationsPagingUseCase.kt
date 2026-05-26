package com.example.android_mvvm_arch.feature.notifications.domain.usecase

import androidx.paging.PagingData
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.repo.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotificationsPagingUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    operator fun invoke(pageSize: Int = 20): Flow<PagingData<Notification>> =
        notificationsRepository.getNotificationsPagingData(pageSize = pageSize)
}
