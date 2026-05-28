package com.example.android_mvvm_arch.feature.auth.data.remote

import com.example.android_mvvm_arch.feature.auth.data.remote.dto.ForgotPasswordRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.LoginRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.LoginResponseDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.RefreshTokenRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.RegisterFcmTokenRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.RegisterRequestDto
import com.example.android_mvvm_arch.feature.auth.data.remote.dto.ResetPasswordRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @POST("api/v1/auth/logout")
    suspend fun logout()

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequestDto): LoginResponseDto

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): LoginResponseDto

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto)

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto)

    @POST("api/v1/device/fcm-token")
    suspend fun registerFcmToken(@Body request: RegisterFcmTokenRequestDto)
}
