package com.example.android_mvvm_arch.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.android_mvvm_arch.feature.auth.presentation.ui.ForgotPasswordScreen
import com.example.android_mvvm_arch.feature.auth.presentation.ui.LoginScreen
import com.example.android_mvvm_arch.feature.auth.presentation.ui.RegisterScreen
import com.example.android_mvvm_arch.feature.auth.presentation.ui.ResetPasswordScreen
import com.example.android_mvvm_arch.feature.home.presentation.ui.HomeScreen
import com.example.android_mvvm_arch.feature.notifications.presentation.ui.NotificationsScreen
import com.example.android_mvvm_arch.feature.profile.presentation.ui.ProfileScreen
import com.example.android_mvvm_arch.feature.settings.presentation.ui.SettingsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val inMainArea = currentRoute in Routes.mainDestinations
    val graphStartDestination = if (startDestination == Routes.LOGIN) {
        Routes.AUTH_GRAPH
    } else {
        Routes.MAIN_GRAPH
    }

    val navigateToMainDestination: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.HOME) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = inMainArea,
        drawerContent = {
            if (inMainArea) {
                ModalDrawerSheet {
                    mainDrawerDestinations.forEach { destination ->
                        NavigationDrawerItem(
                            label = { Text(destination.label) },
                            selected = currentRoute == destination.route,
                            icon = { DrawerItemIcon(icon = destination.icon) },
                            onClick = {
                                navigateToMainDestination(destination.route)
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                if (inMainArea) {
                    TopAppBar(
                        title = { Text(Routes.titleForRoute(currentRoute)) },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "開啟選單",
                                )
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                startDestination = graphStartDestination,
            ) {
                navigation(
                    route = Routes.AUTH_GRAPH,
                    startDestination = Routes.LOGIN,
                ) {
                    composable(Routes.LOGIN) {
                        LoginScreen(
                            onNavigateToHome = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.AUTH_GRAPH) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate(Routes.REGISTER)
                            },
                            onNavigateToForgotPassword = {
                                navController.navigate(Routes.FORGOT_PASSWORD)
                            },
                        )
                    }
                    composable(Routes.REGISTER) {
                        RegisterScreen(
                            onNavigateToHome = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.AUTH_GRAPH) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToLogin = {
                                navController.popBackStack()
                            },
                        )
                    }
                    composable(Routes.FORGOT_PASSWORD) {
                        ForgotPasswordScreen(
                            onNavigateToResetPassword = {
                                navController.navigate(Routes.RESET_PASSWORD)
                            },
                            onNavigateToLogin = {
                                navController.popBackStack(Routes.LOGIN, inclusive = false)
                            },
                        )
                    }
                    composable(Routes.RESET_PASSWORD) {
                        ResetPasswordScreen(
                            onNavigateToLogin = {
                                navController.popBackStack(Routes.LOGIN, inclusive = false)
                            },
                        )
                    }
                }

                navigation(
                    route = Routes.MAIN_GRAPH,
                    startDestination = Routes.HOME,
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            onNavigateToRoute = navigateToMainDestination,
                            showTopBar = false,
                            modifier = Modifier,
                        )
                    }
                    composable(Routes.PROFILE) {
                        ProfileScreen(
                            onNavigateToLogin = {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.MAIN_GRAPH) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToSettings = {
                                navigateToMainDestination(Routes.SETTINGS)
                            },
                            onNavigateToNotifications = {
                                navigateToMainDestination(Routes.NOTIFICATIONS)
                            },
                            onNavigateBack = { navController.popBackStack() },
                            showTopBar = false,
                            modifier = Modifier,
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLogin = {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.MAIN_GRAPH) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            showTopBar = false,
                            modifier = Modifier,
                        )
                    }
                    composable(Routes.NOTIFICATIONS) {
                        NotificationsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            showTopBar = false,
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerItemIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
    )
}
