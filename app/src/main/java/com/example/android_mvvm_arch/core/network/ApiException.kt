package com.example.android_mvvm_arch.core.network

class ApiException(
    val code: Int,
    override val message: String,
    val errorCode: String? = null,
) : Exception(message)
