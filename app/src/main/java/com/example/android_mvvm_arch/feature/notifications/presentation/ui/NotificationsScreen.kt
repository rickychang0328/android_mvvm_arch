package com.example.android_mvvm_arch.feature.notifications.presentation.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.model.NotificationType
import com.example.android_mvvm_arch.feature.notifications.presentation.state.NotificationsIntent
import com.example.android_mvvm_arch.feature.notifications.presentation.state.NotificationsUiEvent
import com.example.android_mvvm_arch.feature.notifications.presentation.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyPagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* 使用者拒絕亦不影響列表瀏覽 */ },
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is NotificationsUiEvent.ShowError -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "重試",
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(NotificationsIntent.Retry)
                    }
                }
                NotificationsUiEvent.AllMarkedRead ->
                    snackbarHostState.showSnackbar("已全部標記為已讀")
                NotificationsUiEvent.RefreshList -> lazyPagingItems.refresh()
                NotificationsUiEvent.RetryList -> lazyPagingItems.retry()
            }
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("通知") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.onIntent(NotificationsIntent.MarkAllRead) },
                            enabled = uiState.unreadCount > 0,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "全部標記為已讀",
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        NotificationsContent(
            modifier = Modifier
                .then(modifier)
                .padding(innerPadding),
            lazyPagingItems = lazyPagingItems,
            onRefresh = { viewModel.onIntent(NotificationsIntent.Refresh) },
            onRetry = lazyPagingItems::retry,
            onItemClick = { id -> viewModel.onIntent(NotificationsIntent.MarkRead(id)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsContent(
    modifier: Modifier,
    lazyPagingItems: androidx.paging.compose.LazyPagingItems<Notification>,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val refreshState = lazyPagingItems.loadState.refresh
    val appendState = lazyPagingItems.loadState.append
    val isRefreshing = refreshState is LoadState.Loading

    Box(modifier = modifier.fillMaxSize()) {
        when {
            refreshState is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            refreshState is LoadState.Error && lazyPagingItems.itemCount == 0 -> {
                FullScreenError(
                    message = refreshState.error.message ?: "載入通知失敗，請稍後再試。",
                    onRetry = onRetry,
                )
            }
            lazyPagingItems.itemCount == 0 -> {
                EmptyState()
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = { index -> lazyPagingItems[index]?.id ?: "placeholder_$index" },
                        ) { index ->
                            val notification = lazyPagingItems[index] ?: return@items
                            NotificationRow(
                                notification = notification,
                                onClick = { onItemClick(notification.id) },
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }

                        when (appendState) {
                            is LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            is LoadState.Error -> {
                                item {
                                    InlineAppendError(
                                        message = appendState.error.message ?: "載入更多通知失敗",
                                        onRetry = onRetry,
                                    )
                                }
                            }
                            is LoadState.NotLoading -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(text = "重試")
        }
    }
}

@Composable
private fun InlineAppendError(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Button(onClick = onRetry) {
            Text("重試")
        }
    }
}

@Composable
private fun NotificationRow(
    notification: Notification,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !notification.isRead, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        UnreadDot(isRead = notification.isRead)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = formatRelativeTime(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.type.label(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun UnreadDot(isRead: Boolean) {
    val color = if (isRead) Color.Transparent else MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(10.dp)
            .padding(top = 6.dp)
            .background(color = color, shape = CircleShape),
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "目前沒有通知",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun NotificationType.label(): String = when (this) {
    NotificationType.SYSTEM -> "系統通知"
    NotificationType.PROMOTION -> "行銷活動"
    NotificationType.ACTIVITY -> "活動提醒"
}
