package com.example.android_mvvm_arch.navigation

object Routes {
    const val AUTH_GRAPH = "auth_graph"
    const val MAIN_GRAPH = "main_graph"

    const val LOGIN = "login"
    const val HOME = "home"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"

    val mainDestinations: Set<String> = setOf(
        HOME,
        PROFILE,
        SETTINGS,
        NOTIFICATIONS,
    )

    fun titleForRoute(route: String?): String = when (route) {
        HOME -> "Home Dashboard"
        PROFILE -> "個人資料"
        SETTINGS -> "設定"
        NOTIFICATIONS -> "通知"
        else -> "Android MVVM"
    }
}
