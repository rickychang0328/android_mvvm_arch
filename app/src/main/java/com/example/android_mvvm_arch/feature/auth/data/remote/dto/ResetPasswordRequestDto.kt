package com.example.android_mvvm_arch.feature.auth.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResetPasswordRequestDto(
    @Json(name = "token") val token: String,
    @Json(name = "new_password") val newPassword: String,
)
