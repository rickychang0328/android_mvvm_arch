package com.example.android_mvvm_arch.feature.home.presentation.viewmodel

import androidx.paging.PagingData
import com.example.android_mvvm_arch.core.sync.SyncManager
import com.example.android_mvvm_arch.core.sync.SyncTarget
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.usecase.GetNotificationsPagingUseCase
import com.example.android_mvvm_arch.feature.profile.domain.usecase.GetUserProfileUseCase
import com.example.android_mvvm_arch.navigation.Routes
import com.example.android_mvvm_arch.navigation.mainDrawerDestinations
import com.example.android_mvvm_arch.util.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelNavigationTest {

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `dashboard quick actions share the same routes as drawer main entries`() = runTest {
        val getUserProfileUseCase = mockk<GetUserProfileUseCase>()
        val getNotificationsPagingUseCase = mockk<GetNotificationsPagingUseCase>()
        val syncManager = mockk<SyncManager>()
        every { getUserProfileUseCase.observeProfile() } returns flowOf(null)
        every { getNotificationsPagingUseCase(any()) } returns flowOf(PagingData.empty<Notification>())
        every { syncManager.requestImmediateSync(any()) } just runs

        val viewModel = HomeViewModel(
            getUserProfileUseCase = getUserProfileUseCase,
            getNotificationsPagingUseCase = getNotificationsPagingUseCase,
            syncManager = syncManager,
        )
        advanceUntilIdle()

        val quickActionRoutes = viewModel.uiState.value.quickActions.map { it.route }.toSet()
        val drawerMainRoutes = mainDrawerDestinations
            .map { it.route }
            .filterNot { it == Routes.HOME }
            .toSet()

        assertEquals(drawerMainRoutes, quickActionRoutes)
        verify {
            syncManager.requestImmediateSync(setOf(SyncTarget.PROFILE, SyncTarget.NOTIFICATIONS))
        }
    }
}
