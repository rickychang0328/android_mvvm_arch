package com.example.android_mvvm_arch.feature.auth.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterFcmTokenRequestDto(
    @Json(name = "fcm_token") val fcmToken: String,
    @Json(name = "platform") val platform: String = "android",
)
