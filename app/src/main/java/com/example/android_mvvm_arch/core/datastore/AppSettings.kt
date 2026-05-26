package com.example.android_mvvm_arch.core.datastore

data class AppSettings(
    val isDarkMode: Boolean = false,
    val language: String = "zh-TW",
    val notificationsEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,
    val personalizedAdsEnabled: Boolean = false,
    val biometricLoginEnabled: Boolean = false,
)
