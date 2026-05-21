package com.example.android_mvvm_arch.feature.auth.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.auth.domain.repo.AuthRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class IsLoggedInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(): Boolean = withContext(dispatcherProvider.io) {
        authRepository.isLoggedIn()
    }
}
