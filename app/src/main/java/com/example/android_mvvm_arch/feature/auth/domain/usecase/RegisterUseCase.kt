package com.example.android_mvvm_arch.feature.auth.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.auth.domain.model.AuthTokens
import com.example.android_mvvm_arch.feature.auth.domain.model.RegisterCredentials
import com.example.android_mvvm_arch.feature.auth.domain.repo.AuthRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String,
    ): Result<AuthTokens> = withContext(dispatcherProvider.io) {
        val trimmedEmail = email.trim()
        val trimmedDisplayName = displayName.trim()

        if (trimmedEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email cannot be empty."))
        }
        if (!EMAIL_REGEX.matches(trimmedEmail)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid email format."))
        }
        if (trimmedDisplayName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Display name cannot be empty."))
        }
        if (password.length < 8) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 8 characters."))
        }
        if (password != confirmPassword) {
            return@withContext Result.failure(IllegalArgumentException("Passwords do not match."))
        }

        authRepository.register(
            RegisterCredentials(
                email = trimmedEmail,
                password = password,
                displayName = trimmedDisplayName,
            )
        )
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    }
}
