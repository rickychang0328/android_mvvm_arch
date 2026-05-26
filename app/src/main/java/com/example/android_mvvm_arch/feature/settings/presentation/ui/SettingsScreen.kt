package com.example.android_mvvm_arch.feature.settings.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android_mvvm_arch.feature.settings.presentation.state.SettingsIntent
import com.example.android_mvvm_arch.feature.settings.presentation.state.SettingsUiEvent
import com.example.android_mvvm_arch.feature.settings.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SettingsUiEvent.NavigateBack -> onNavigateBack()
                SettingsUiEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
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
