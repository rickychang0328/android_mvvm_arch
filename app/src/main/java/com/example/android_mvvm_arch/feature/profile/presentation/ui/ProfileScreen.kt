package com.example.android_mvvm_arch.feature.profile.presentation.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.android_mvvm_arch.feature.profile.presentation.state.ProfileIntent
import com.example.android_mvvm_arch.feature.profile.presentation.state.ProfileUiEvent
import com.example.android_mvvm_arch.feature.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAvatarSheet by remember { mutableStateOf(false) }
    var pendingSource by remember { mutableStateOf<AvatarSource?>(null) }
    var cameraTempFile by remember { mutableStateOf<File?>(null) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    var localPreviewUri by remember { mutableStateOf<Uri?>(null) }

    val galleryPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val cameraPermissions = remember { galleryPermissions + Manifest.permission.CAMERA }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                ProfileUiEvent.NavigateToLogin -> onNavigateToLogin()
                is ProfileUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            localPreviewUri = uri
            scope.launch {
                val file = context.copyUriToCache(uri)
                if (file != null) {
                    viewModel.onIntent(ProfileIntent.UploadAvatar(file))
                } else {
                    snackbarHostState.showSnackbar("讀取圖片失敗，請重試。")
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val photoFile = cameraTempFile
        if (result.resultCode == Activity.RESULT_OK && photoFile != null) {
            localPreviewUri = cameraTempUri
            viewModel.onIntent(ProfileIntent.UploadAvatar(photoFile))
        } else {
            photoFile?.delete()
        }
        cameraTempFile = null
        cameraTempUri = null
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantResults ->
        val granted = grantResults.values.all { it }
        val source = pendingSource
        if (granted && source != null) {
            when (source) {
                AvatarSource.GALLERY -> {
                    showAvatarSheet = false
                    launchGalleryPicker(galleryLauncher)
                }
                AvatarSource.CAMERA -> {
                    showAvatarSheet = false
                    val (file, uri) = createAvatarCacheFile(context)
                    cameraTempFile = file
                    cameraTempUri = uri
                    launchCameraCapture(cameraLauncher, uri)
                }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("需要授權才能更換頭像") }
        }
        pendingSource = null
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("個人資料") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToNotifications) {
                            BadgedBox(
                                badge = {
                                    if (uiState.unreadNotificationsCount > 0) {
                                        Badge {
                                            Text(text = uiState.unreadNotificationsCount.toString())
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "通知",
                                )
                            }
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "設定",
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .then(modifier)
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            if (uiState.isLoading && uiState.profile == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(112.dp),
                ) {
                    val avatarData = localPreviewUri ?: uiState.profile?.avatarUrl
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarData)
                            .crossfade(true)
                            .build(),
                        contentDescription = "頭像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    if (avatarData == null) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "預設頭像",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp),
                        )
                    }
                    IconButton(
                        onClick = { showAvatarSheet = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape,
                            ),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "更換頭像",
                        )
                    }
                    if (uiState.isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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

    if (showAvatarSheet) {
        ModalBottomSheet(onDismissRequest = { showAvatarSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "更換頭像",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "支援相簿與相機，上傳後會同步更新本地快取。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                AvatarSheetItem(
                    icon = Icons.Default.Image,
                    label = "從相簿選擇",
                    onClick = {
                        pendingSource = AvatarSource.GALLERY
                        if (hasPermissions(context, galleryPermissions)) {
                            showAvatarSheet = false
                            launchGalleryPicker(galleryLauncher)
                            pendingSource = null
                        } else {
                            permissionsLauncher.launch(galleryPermissions)
                        }
                    },
                )
                AvatarSheetItem(
                    icon = Icons.Default.CameraAlt,
                    label = "相機拍照",
                    onClick = {
                        pendingSource = AvatarSource.CAMERA
                        if (hasPermissions(context, cameraPermissions)) {
                            showAvatarSheet = false
                            val (file, uri) = createAvatarCacheFile(context)
                            cameraTempFile = file
                            cameraTempUri = uri
                            launchCameraCapture(cameraLauncher, uri)
                            pendingSource = null
                        } else {
                            permissionsLauncher.launch(cameraPermissions)
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showAvatarSheet = false }) {
                    Text("取消")
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

@Composable
private fun AvatarSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private enum class AvatarSource { GALLERY, CAMERA }

private fun hasPermissions(context: android.content.Context, permissions: Array<String>): Boolean =
    permissions.all {
        ContextCompat.checkSelfPermission(context, it) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

private fun launchGalleryPicker(
    launcher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>,
) {
    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        type = "image/*"
    }
    launcher.launch(intent)
}

private fun launchCameraCapture(
    launcher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>,
    outputUri: Uri,
) {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    launcher.launch(intent)
}

private fun createAvatarCacheFile(context: android.content.Context): Pair<File, Uri> {
    val file = File.createTempFile("avatar_", ".jpg", context.cacheDir)
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    return file to uri
}

private suspend fun android.content.Context.copyUriToCache(uri: Uri): File? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val file = File.createTempFile("avatar_", ".jpg", cacheDir)
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
                file
            }
        } catch (_: Exception) {
            null
        }
    }
