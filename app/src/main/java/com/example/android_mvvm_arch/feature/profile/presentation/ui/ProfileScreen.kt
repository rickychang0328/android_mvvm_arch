package com.example.android_mvvm_arch.feature.profile.presentation.ui

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android_mvvm_arch.feature.profile.presentation.state.ProfileIntent
import com.example.android_mvvm_arch.feature.profile.presentation.state.ProfileUiEvent
import com.example.android_mvvm_arch.feature.profile.presentation.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                ProfileUiEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "個人資料",
                style = MaterialTheme.typography.headlineMedium,
            )
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "設定",
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading && uiState.profile == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            uiState.profile?.email?.let { email ->
                Text(
                    text = "Email: $email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isEditing) {
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = { viewModel.onIntent(ProfileIntent.DisplayNameChanged(it)) },
                    label = { Text("顯示名稱") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = { viewModel.onIntent(ProfileIntent.PhoneChanged(it)) },
                    label = { Text("電話") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.bio,
                    onValueChange = { viewModel.onIntent(ProfileIntent.BioChanged(it)) },
                    label = { Text("簡介") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = !uiState.isSaving,
                )
            } else {
                ProfileReadOnlyRow(label = "顯示名稱", value = uiState.profile?.displayName)
                ProfileReadOnlyRow(label = "電話", value = uiState.profile?.phone)
                ProfileReadOnlyRow(label = "簡介", value = uiState.profile?.bio)
            }

            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
            uiState.successMessage?.let { success ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = success, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.isEditing) {
                    OutlinedButton(
                        onClick = { viewModel.onIntent(ProfileIntent.CancelEditing) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving,
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = { viewModel.onIntent(ProfileIntent.SaveProfile) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving,
                    ) {
                        Text(if (uiState.isSaving) "儲存中…" else "儲存")
                    }
                } else {
                    Button(
                        onClick = { viewModel.onIntent(ProfileIntent.StartEditing) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("編輯")
                    }
                    OutlinedButton(
                        onClick = { viewModel.onIntent(ProfileIntent.Logout) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("登出")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileReadOnlyRow(label: String, value: String?) {
    Text(
        text = "$label：${value ?: "-"}",
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(8.dp))
}
