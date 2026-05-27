package com.example.android_mvvm_arch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.android_mvvm_arch.core.datastore.AppSettings
import com.example.android_mvvm_arch.core.datastore.SettingsDataStore
import com.example.android_mvvm_arch.core.notification.NotificationHelper
import com.example.android_mvvm_arch.core.sync.SyncManager
import com.example.android_mvvm_arch.navigation.AppNavGraph
import com.example.android_mvvm_arch.navigation.Routes
import com.example.android_mvvm_arch.presentation.MainViewModel
import com.example.android_mvvm_arch.ui.theme.Android_mvvm_archTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var syncManager: SyncManager

    private val pendingDeepLink = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeDeepLink(intent)
        setContent {
            val appSettings by settingsDataStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )

            Android_mvvm_archTheme(darkTheme = appSettings.isDarkMode) {
                val mainViewModel: MainViewModel = hiltViewModel()
                val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val destination = startDestination) {
                            null -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }

                            else -> key(destination) {
                                val navController = rememberNavController()
                                val deepLink by pendingDeepLink.collectAsState()

                                LaunchedEffect(deepLink) {
                                    val target = deepLink ?: return@LaunchedEffect
                                    if (target == NotificationHelper.DEEP_LINK_NOTIFICATIONS &&
                                        destination != Routes.LOGIN
                                    ) {
                                        navController.navigate(Routes.NOTIFICATIONS) {
                                            launchSingleTop = true
                                        }
                                    }
                                    pendingDeepLink.value = null
                                }

                                AppNavGraph(
                                    startDestination = destination,
                                    navController = navController,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeDeepLink(intent)
    }

    override fun onStart() {
        super.onStart()
        // App 回到前景時以一致入口補一次即時同步（實際執行由 Worker + constraints 管理）。
        syncManager.requestImmediateSync()
    }

    private fun consumeDeepLink(intent: Intent?) {
        val target = intent?.getStringExtra(NotificationHelper.EXTRA_DEEP_LINK) ?: return
        pendingDeepLink.value = target
    }
}
