package com.example.android_mvvm_arch.feature.auth.data.remote

import com.example.android_mvvm_arch.feature.auth.data.remote.dto.LoginRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @POST("api/v1/auth/logout")
    suspend fun logout()
}
