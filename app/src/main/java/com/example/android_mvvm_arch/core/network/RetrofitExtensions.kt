package com.example.android_mvvm_arch.core.network

import com.example.android_mvvm_arch.feature.auth.data.remote.dto.ApiErrorDto
import com.squareup.moshi.Moshi
import retrofit2.HttpException

suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (http: HttpException) {
    val message = parseErrorMessage(http) ?: "Request failed with code ${http.code()}"
    Result.failure(ApiException(http.code(), message))
} catch (e: Exception) {
    Result.failure(e)
}

private fun parseErrorMessage(http: HttpException): String? = try {
    val body = http.response()?.errorBody()?.string() ?: return null
    val adapter = Moshi.Builder().build().adapter(ApiErrorDto::class.java)
    adapter.fromJson(body)?.message
} catch (_: Exception) {
    null
}
