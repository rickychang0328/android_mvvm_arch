package com.example.android_mvvm_arch.navigation

import androidx.navigation.NavHostController

data class NavigationRequest(
    val route: String,
    val popUpToRoute: String? = null,
    val inclusive: Boolean = false,
    val launchSingleTop: Boolean = false,
    val saveState: Boolean = false,
    val restoreState: Boolean = false,
)

object AppNavigationPolicy {
    fun graphStartDestination(startDestination: String): String =
        if (startDestination == Routes.LOGIN) Routes.AUTH_GRAPH else Routes.MAIN_GRAPH

    fun isMainAreaRoute(route: String?): Boolean = route in Routes.mainDestinations

    fun shouldNavigateToNotificationsFromDeepLink(
        deepLinkTarget: String?,
        startDestination: String,
    ): Boolean = deepLinkTarget == Routes.NOTIFICATIONS && startDestination != Routes.LOGIN

    fun mainAreaDestination(route: String): NavigationRequest = NavigationRequest(
        route = route,
        popUpToRoute = Routes.HOME,
        launchSingleTop = true,
        saveState = true,
        restoreState = true,
    )

    fun authSuccessDestination(): NavigationRequest = NavigationRequest(
        route = Routes.HOME,
        popUpToRoute = Routes.AUTH_GRAPH,
        inclusive = true,
        launchSingleTop = true,
    )

    fun logoutDestination(): NavigationRequest = NavigationRequest(
        route = Routes.LOGIN,
        popUpToRoute = Routes.MAIN_GRAPH,
        inclusive = true,
        launchSingleTop = true,
    )
}

fun NavHostController.navigateWithRequest(request: NavigationRequest) {
    navigate(request.route) {
        request.popUpToRoute?.let { route ->
            popUpTo(route) {
                inclusive = request.inclusive
                saveState = request.saveState
            }
        }
        launchSingleTop = request.launchSingleTop
        restoreState = request.restoreState
    }
}
