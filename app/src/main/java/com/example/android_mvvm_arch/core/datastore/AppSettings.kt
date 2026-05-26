package com.example.android_mvvm_arch.core.datastore

data class AppSettings(
    val isDarkMode: Boolean = false,
    val language: String = "zh-TW",
    val notificationsEnabled: Boolean = true,
)
