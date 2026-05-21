package com.example.android_mvvm_arch.feature.auth.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.auth.domain.repo.AuthRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> =
        withContext(dispatcherProvider.io) {
            val trimmedToken = token.trim()
            if (trimmedToken.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Reset token cannot be empty."))
            }
            if (newPassword.length < 8) {
                return@withContext Result.failure(
                    IllegalArgumentException("Password must be at least 8 characters.")
                )
            }
            authRepository.resetPassword(trimmedToken, newPassword)
        }
}
