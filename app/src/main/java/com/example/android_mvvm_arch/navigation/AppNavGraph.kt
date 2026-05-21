package com.example.android_mvvm_arch.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.android_mvvm_arch.feature.auth.presentation.ui.ForgotPasswordScreen
import com.example.android_mvvm_arch.feature.auth.presentation.ui.LoginScreen
import com.example.android_mvvm_arch.feature.auth.presentation.ui.RegisterScreen
import com.example.android_mvvm_arch.feature.auth.presentation.ui.ResetPasswordScreen
import com.example.android_mvvm_arch.feature.profile.presentation.ui.ProfileScreen

@Composable
fun AppNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
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
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(0) { inclusive = true }
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
        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.PROFILE) { inclusive = true }
                    }
                },
            )
        }
    }
}
