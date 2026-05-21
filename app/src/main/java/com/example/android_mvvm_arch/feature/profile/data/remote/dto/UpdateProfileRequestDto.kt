package com.example.android_mvvm_arch.feature.profile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateProfileRequestDto(
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "phone") val phone: String?,
    @Json(name = "bio") val bio: String?,
)
