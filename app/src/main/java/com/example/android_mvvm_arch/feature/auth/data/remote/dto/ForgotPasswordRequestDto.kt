package com.example.android_mvvm_arch.feature.auth.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequestDto(
    @Json(name = "email") val email: String,
)
