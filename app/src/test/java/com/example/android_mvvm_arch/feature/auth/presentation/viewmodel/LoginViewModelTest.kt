package com.example.android_mvvm_arch.feature.auth.presentation.viewmodel

import app.cash.turbine.test
import com.example.android_mvvm_arch.core.network.ApiException
import com.example.android_mvvm_arch.feature.auth.domain.model.AuthTokens
import com.example.android_mvvm_arch.feature.auth.domain.usecase.LoginUseCase
import com.example.android_mvvm_arch.feature.auth.presentation.state.LoginIntent
import com.example.android_mvvm_arch.feature.auth.presentation.state.LoginUiEvent
import com.example.android_mvvm_arch.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val loginUseCase: LoginUseCase = mockk()
    private lateinit var viewModel: LoginViewModel

    private val tokens = AuthTokens(
        accessToken = "access",
        refreshToken = "refresh",
        expiresInSeconds = 3600,
        tokenType = "Bearer",
    )

    @BeforeEach
    fun setUp() {
        viewModel = LoginViewModel(loginUseCase)
    }

    @Test
    fun `email changed updates state`() = runTest {
        viewModel.onIntent(LoginIntent.EmailChanged("demo@example.com"))

        assertEquals("demo@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun `submit login emits navigate event on success`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.success(tokens)
        viewModel.onIntent(LoginIntent.EmailChanged("demo@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("password123"))

        viewModel.uiEvent.test {
            viewModel.onIntent(LoginIntent.SubmitLogin)
            assertEquals(LoginUiEvent.NavigateToProfile, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `submit login shows api error message on failure`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.failure(
            ApiException(401, "Email or password is incorrect."),
        )
        viewModel.onIntent(LoginIntent.EmailChanged("wrong@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("bad"))

        viewModel.onIntent(LoginIntent.SubmitLogin)

        assertEquals(
            "Email or password is incorrect.",
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `password changed clears error message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Result.failure(
            ApiException(401, "Email or password is incorrect."),
        )
        viewModel.onIntent(LoginIntent.EmailChanged("wrong@example.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("bad"))
        viewModel.onIntent(LoginIntent.SubmitLogin)
        assertEquals("Email or password is incorrect.", viewModel.uiState.value.errorMessage)

        viewModel.onIntent(LoginIntent.PasswordChanged("new"))

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
