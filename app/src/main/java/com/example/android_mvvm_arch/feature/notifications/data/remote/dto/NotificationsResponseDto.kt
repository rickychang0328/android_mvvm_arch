package com.example.android_mvvm_arch.feature.notifications.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationsResponseDto(
    @Json(name = "items") val items: List<NotificationDto>,
    @Json(name = "next_page") val nextPage: Int?,
    @Json(name = "has_more") val hasMore: Boolean,
)
