package com.example.android_mvvm_arch.feature.auth.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.auth.domain.model.AuthTokens
import com.example.android_mvvm_arch.feature.auth.domain.model.LoginCredentials
import com.example.android_mvvm_arch.feature.auth.domain.repo.AuthRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthTokens> =
        withContext(dispatcherProvider.io) {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Email cannot be empty."))
            }
            if (!EMAIL_REGEX.matches(trimmedEmail)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid email format."))
            }
            if (password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Password cannot be empty."))
            }
            authRepository.login(LoginCredentials(trimmedEmail, password))
        }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    }
}
