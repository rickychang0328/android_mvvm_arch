package com.example.android_mvvm_arch.feature.profile.presentation.viewmodel

import app.cash.turbine.test
import com.example.android_mvvm_arch.feature.auth.domain.usecase.LogoutUseCase
import com.example.android_mvvm_arch.feature.profile.domain.model.UserProfile
import com.example.android_mvvm_arch.feature.profile.domain.usecase.GetUserProfileUseCase
import com.example.android_mvvm_arch.feature.profile.domain.usecase.UpdateUserProfileUseCase
import com.example.android_mvvm_arch.feature.profile.presentation.state.ProfileIntent
import com.example.android_mvvm_arch.feature.profile.presentation.state.ProfileUiEvent
import com.example.android_mvvm_arch.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val getUserProfileUseCase: GetUserProfileUseCase = mockk()
    private val updateUserProfileUseCase: UpdateUserProfileUseCase = mockk()
    private val logoutUseCase: LogoutUseCase = mockk()
    private val profileFlow = MutableStateFlow<UserProfile?>(null)
    private lateinit var viewModel: ProfileViewModel

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
        every { getUserProfileUseCase.observeProfile() } returns profileFlow
        coEvery { getUserProfileUseCase.refresh() } returns Result.success(profile)
        coEvery { updateUserProfileUseCase(any(), any(), any()) } returns Result.success(profile)
        coEvery { logoutUseCase() } returns Result.success(Unit)
        viewModel = ProfileViewModel(
            getUserProfileUseCase,
            updateUserProfileUseCase,
            logoutUseCase,
        )
    }

    @Test
    fun `start editing enables edit mode`() = runTest {
        viewModel.onIntent(ProfileIntent.StartEditing)

        assertTrue(viewModel.uiState.value.isEditing)
    }

    @Test
    fun `save profile updates success message`() = runTest {
        profileFlow.value = profile
        viewModel.onIntent(ProfileIntent.StartEditing)
        viewModel.onIntent(ProfileIntent.DisplayNameChanged("Updated Name"))
        viewModel.onIntent(ProfileIntent.SaveProfile)

        assertEquals("Profile updated.", viewModel.uiState.value.successMessage)
        assertEquals(false, viewModel.uiState.value.isEditing)
    }

    @Test
    fun `logout emits navigate to login event`() = runTest {
        viewModel.uiEvent.test {
            viewModel.onIntent(ProfileIntent.Logout)
            assertEquals(ProfileUiEvent.NavigateToLogin, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { logoutUseCase() }
    }
}
