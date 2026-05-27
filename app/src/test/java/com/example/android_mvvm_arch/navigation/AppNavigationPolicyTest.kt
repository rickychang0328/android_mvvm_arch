package com.example.android_mvvm_arch.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppNavigationPolicyTest {

    @Test
    fun `logged in start destination enters main graph`() {
        assertEquals(Routes.MAIN_GRAPH, AppNavigationPolicy.graphStartDestination(Routes.HOME))
    }

    @Test
    fun `login start destination enters auth graph`() {
        assertEquals(Routes.AUTH_GRAPH, AppNavigationPolicy.graphStartDestination(Routes.LOGIN))
    }

    @Test
    fun `drawer destinations are all valid main routes`() {
        val drawerRoutes = mainDrawerDestinations.map { it.route }.toSet()

        assertEquals(
            setOf(Routes.HOME, Routes.PROFILE, Routes.SETTINGS, Routes.NOTIFICATIONS),
            drawerRoutes,
        )
        assertTrue(drawerRoutes.all { it in Routes.mainDestinations })
    }

    @Test
    fun `main area destination switching keeps single top and restores state`() {
        val destinations = listOf(Routes.HOME, Routes.PROFILE, Routes.SETTINGS, Routes.NOTIFICATIONS)

        destinations.forEach { route ->
            val request = AppNavigationPolicy.mainAreaDestination(route)

            assertEquals(route, request.route)
            assertEquals(Routes.HOME, request.popUpToRoute)
            assertTrue(request.launchSingleTop)
            assertTrue(request.saveState)
            assertTrue(request.restoreState)
            assertFalse(request.inclusive)
        }
    }

    @Test
    fun `logout destination clears main graph from back stack`() {
        val request = AppNavigationPolicy.logoutDestination()

        assertEquals(Routes.LOGIN, request.route)
        assertEquals(Routes.MAIN_GRAPH, request.popUpToRoute)
        assertTrue(request.inclusive)
        assertTrue(request.launchSingleTop)
    }

    @Test
    fun `notification deep link routes to notifications only for authenticated start`() {
        assertTrue(
            AppNavigationPolicy.shouldNavigateToNotificationsFromDeepLink(
                deepLinkTarget = Routes.NOTIFICATIONS,
                startDestination = Routes.HOME,
            ),
        )
        assertFalse(
            AppNavigationPolicy.shouldNavigateToNotificationsFromDeepLink(
                deepLinkTarget = Routes.NOTIFICATIONS,
                startDestination = Routes.LOGIN,
            ),
        )
        assertFalse(
            AppNavigationPolicy.shouldNavigateToNotificationsFromDeepLink(
                deepLinkTarget = "unknown",
                startDestination = Routes.HOME,
            ),
        )
    }
}
