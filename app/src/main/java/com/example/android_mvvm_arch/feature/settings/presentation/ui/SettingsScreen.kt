package com.example.android_mvvm_arch.feature.settings.presentation.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android_mvvm_arch.feature.settings.presentation.state.SettingsIntent
import com.example.android_mvvm_arch.feature.settings.presentation.state.SettingsUiEvent
import com.example.android_mvvm_arch.feature.settings.presentation.viewmodel.SettingsViewModel

private const val PRIVACY_POLICY_URL = "https://example.com/privacy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showClearCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SettingsUiEvent.NavigateBack -> onNavigateBack()
                SettingsUiEvent.NavigateToLogin -> onNavigateToLogin()
                SettingsUiEvent.CacheCleared -> snackbarHostState.showSnackbar("已清除本地快取")
                is SettingsUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清除快取") },
            text = { Text("將清除本地暫存的個人資料快取，但會保留登入狀態與設定。是否要繼續？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.onIntent(SettingsIntent.ClearCache)
                    },
                ) {
                    Text("確認")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            SettingsSwitchRow(
                label = "深色模式",
                checked = uiState.isDarkMode,
                onCheckedChange = { viewModel.onIntent(SettingsIntent.DarkModeChanged(it)) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "語言",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val languageOptions = listOf("zh-TW" to "繁體中文", "en" to "English")
                languageOptions.forEachIndexed { index, (code, label) ->
                    SegmentedButton(
                        selected = uiState.language == code,
                        onClick = { viewModel.onIntent(SettingsIntent.LanguageChanged(code)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = languageOptions.size,
                        ),
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSwitchRow(
                label = "通知",
                checked = uiState.notificationsEnabled,
                onCheckedChange = { viewModel.onIntent(SettingsIntent.NotificationsChanged(it)) },
            )

            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "隱私與資料",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacySwitchRow(
                icon = Icons.Filled.Analytics,
                title = "使用分析",
                description = "協助改善 App 體驗",
                checked = uiState.analyticsEnabled,
                onCheckedChange = { viewModel.onIntent(SettingsIntent.UpdateAnalytics(it)) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacySwitchRow(
                icon = Icons.Filled.BugReport,
                title = "當機回報",
                description = "自動回報當機資訊",
                checked = uiState.crashReportingEnabled,
                onCheckedChange = { viewModel.onIntent(SettingsIntent.UpdateCrashReporting(it)) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacySwitchRow(
                icon = Icons.Filled.Campaign,
                title = "個人化廣告",
                description = "根據使用習慣顯示相關內容",
                checked = uiState.personalizedAdsEnabled,
                onCheckedChange = { viewModel.onIntent(SettingsIntent.UpdatePersonalizedAds(it)) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacySwitchRow(
                icon = Icons.Filled.Fingerprint,
                title = "生物辨識登入",
                description = "使用指紋或臉部辨識登入",
                checked = uiState.biometricLoginEnabled,
                onCheckedChange = { viewModel.onIntent(SettingsIntent.UpdateBiometricLogin(it)) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrivacyActionRow(
                icon = Icons.Filled.DeleteSweep,
                title = "清除快取",
                description = "清除本地暫存資料（保留登入狀態與設定）",
                actionLabel = "清除",
                onClick = { showClearCacheDialog = true },
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrivacyActionRow(
                icon = Icons.Filled.PrivacyTip,
                title = "隱私權政策",
                description = "於瀏覽器開啟最新版本",
                actionLabel = "開啟",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { viewModel.onIntent(SettingsIntent.Logout) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("登出")
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun PrivacySwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun PrivacyActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick) {
            Text(actionLabel)
        }
    }
}
