package com.example.android_mvvm_arch.feature.profile.data.remote

import com.example.android_mvvm_arch.feature.profile.data.remote.dto.UpdateProfileRequestDto
import com.example.android_mvvm_arch.feature.profile.data.remote.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ProfileApi {
    @GET("api/v1/users/me")
    suspend fun getProfile(): UserProfileDto

    @PUT("api/v1/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): UserProfileDto
}
