package com.example.android_mvvm_arch.feature.notifications.data.mapper

import com.example.android_mvvm_arch.feature.notifications.data.local.NotificationEntity
import com.example.android_mvvm_arch.feature.notifications.data.remote.dto.NotificationDto
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.model.NotificationType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationMapper @Inject constructor() {

    fun toEntity(dto: NotificationDto): NotificationEntity = NotificationEntity(
        id = dto.id,
        title = dto.title,
        body = dto.body,
        type = NotificationType.fromRaw(dto.type).name,
        isRead = dto.isRead,
        createdAt = dto.createdAt,
    )

    fun toDomain(entity: NotificationEntity): Notification = Notification(
        id = entity.id,
        title = entity.title,
        body = entity.body,
        type = NotificationType.fromRaw(entity.type),
        isRead = entity.isRead,
        createdAt = entity.createdAt,
    )

    fun toDomain(dto: NotificationDto): Notification = Notification(
        id = dto.id,
        title = dto.title,
        body = dto.body,
        type = NotificationType.fromRaw(dto.type),
        isRead = dto.isRead,
        createdAt = dto.createdAt,
    )
}
