package com.example.android_mvvm_arch.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class MainDrawerDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val mainDrawerDestinations: List<MainDrawerDestination> = listOf(
    MainDrawerDestination(
        route = Routes.HOME,
        label = "Dashboard",
        icon = Icons.Default.Home,
    ),
    MainDrawerDestination(
        route = Routes.PROFILE,
        label = "個人資料",
        icon = Icons.Default.Person,
    ),
    MainDrawerDestination(
        route = Routes.SETTINGS,
        label = "設定",
        icon = Icons.Default.Settings,
    ),
    MainDrawerDestination(
        route = Routes.NOTIFICATIONS,
        label = "通知",
        icon = Icons.Default.Notifications,
    ),
)
