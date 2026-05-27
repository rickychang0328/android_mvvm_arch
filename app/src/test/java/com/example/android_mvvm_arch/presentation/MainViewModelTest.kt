package com.example.android_mvvm_arch.presentation

import com.example.android_mvvm_arch.core.auth.AuthEventBus
import com.example.android_mvvm_arch.feature.auth.domain.usecase.IsLoggedInUseCase
import com.example.android_mvvm_arch.navigation.Routes
import com.example.android_mvvm_arch.util.MainDispatcherRule
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `logged in user starts in home route`() = runTest {
        val isLoggedInUseCase = io.mockk.mockk<IsLoggedInUseCase>()
        val authEventBus = AuthEventBus()
        coEvery { isLoggedInUseCase() } returns true

        val viewModel = MainViewModel(isLoggedInUseCase, authEventBus)
        advanceUntilIdle()

        assertEquals(Routes.HOME, viewModel.startDestination.value)
    }

    @Test
    fun `force logout event changes start destination to login`() = runTest {
        val isLoggedInUseCase = io.mockk.mockk<IsLoggedInUseCase>()
        val authEventBus = AuthEventBus()
        coEvery { isLoggedInUseCase() } returns true

        val viewModel = MainViewModel(isLoggedInUseCase, authEventBus)
        advanceUntilIdle()
        authEventBus.sendForceLogout()
        advanceUntilIdle()

        assertEquals(Routes.LOGIN, viewModel.startDestination.value)
    }
}
