package com.example.android_mvvm_arch.feature.auth.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.auth.domain.model.AuthTokens
import com.example.android_mvvm_arch.feature.auth.domain.model.LoginCredentials
import com.example.android_mvvm_arch.feature.auth.domain.repo.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider: DispatcherProvider = mockk {
        coEvery { io } returns testDispatcher
    }
    private lateinit var loginUseCase: LoginUseCase

    private val tokens = AuthTokens(
        accessToken = "access",
        refreshToken = "refresh",
        expiresInSeconds = 3600,
        tokenType = "Bearer",
    )

    @BeforeEach
    fun setUp() {
        loginUseCase = LoginUseCase(authRepository, dispatcherProvider)
    }

    @Test
    fun `invoke returns success when credentials are valid`() = runTest(testDispatcher) {
        coEvery { authRepository.login(any()) } returns Result.success(tokens)

        val result = loginUseCase("demo@example.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals(tokens, result.getOrNull())
        coVerify {
            authRepository.login(LoginCredentials("demo@example.com", "password123"))
        }
    }

    @Test
    fun `invoke fails when email is blank`() = runTest(testDispatcher) {
        val result = loginUseCase("  ", "password123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke fails when email format is invalid`() = runTest(testDispatcher) {
        val result = loginUseCase("not-an-email", "password123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke fails when password is blank`() = runTest(testDispatcher) {
        val result = loginUseCase("demo@example.com", "")

        assertTrue(result.isFailure)
    }
}
