package com.example.android_mvvm_arch.feature.profile.domain.usecase

import com.example.android_mvvm_arch.core.util.DispatcherProvider
import com.example.android_mvvm_arch.feature.profile.domain.model.ProfileUpdate
import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile
import com.example.android_mvvm_arch.feature.profile.domain.repo.ProfileRepository
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
class UpdateUserProfileUseCaseTest {

    private val profileRepository: ProfileRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private val dispatcherProvider: DispatcherProvider = mockk {
        coEvery { io } returns testDispatcher
    }
    private lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase

    private val profile = UserProfile(
        id = "usr_001",
        email = "demo@example.com",
        displayName = "Demo User",
        avatarUrl = null,
        phone = "+886912345678",
        bio = "Bio",
        createdAt = "2024-01-15T08:30:00Z",
        updatedAt = "2025-03-01T12:00:00Z",
    )

    @BeforeEach
    fun setUp() {
        updateUserProfileUseCase = UpdateUserProfileUseCase(profileRepository, dispatcherProvider)
    }

    @Test
    fun `invoke updates profile successfully`() = runTest(testDispatcher) {
        coEvery { profileRepository.updateProfile(any()) } returns Result.success(profile)

        val result = updateUserProfileUseCase("New Name", "+886900000000", "New bio")

        assertTrue(result.isSuccess)
        assertEquals(profile, result.getOrNull())
        coVerify {
            profileRepository.updateProfile(
                ProfileUpdate("New Name", "+886900000000", "New bio"),
            )
        }
    }

    @Test
    fun `invoke fails when display name is blank`() = runTest(testDispatcher) {
        val result = updateUserProfileUseCase("  ", "+886900000000", "bio")

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke fails when bio exceeds 200 characters`() = runTest(testDispatcher) {
        val result = updateUserProfileUseCase("Name", "", "x".repeat(201))

        assertTrue(result.isFailure)
    }
}
