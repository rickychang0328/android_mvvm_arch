package com.example.android_mvvm_arch.feature.home.presentation.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.android_mvvm_arch.feature.home.presentation.state.HomeUiState
import com.example.android_mvvm_arch.feature.home.presentation.state.QuickAction
import com.example.android_mvvm_arch.feature.home.presentation.viewmodel.HomeViewModel
import com.example.android_mvvm_arch.feature.notifications.domain.model.Notification
import com.example.android_mvvm_arch.feature.notifications.domain.model.NotificationType
import com.example.android_mvvm_arch.navigation.Routes
import java.util.concurrent.TimeUnit
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToRoute: (String) -> Unit,
    showTopBar: Boolean = true,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.recentNotificationsPagingDataFlow.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Home Dashboard") },
                    navigationIcon = {
                        if (onOpenDrawer != null) {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "開啟選單",
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        HomeContent(
            modifier = Modifier
                .then(modifier)
                .fillMaxSize()
                .padding(innerPadding),
            uiState = uiState,
            lazyPagingItems = lazyPagingItems,
            onActionClick = onNavigateToRoute,
            onRefreshAll = {
                viewModel.onRefresh()
                lazyPagingItems.refresh()
            },
            onRetryPaging = lazyPagingItems::retry,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    lazyPagingItems: LazyPagingItems<Notification>,
    onActionClick: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onRetryPaging: () -> Unit,
) {
    val refreshState = lazyPagingItems.loadState.refresh
    val appendState = lazyPagingItems.loadState.append
    val pagingRefreshing = refreshState is LoadState.Loading && lazyPagingItems.itemCount > 0
    val isRefreshing = uiState.isLoading || pagingRefreshing
    val actionRows = remember(uiState.quickActions) { uiState.quickActions.chunked(2) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefreshAll,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileSummaryCard(
                    displayName = uiState.userProfile?.displayName ?: "Loading...",
                    email = uiState.userProfile?.email ?: "",
                )
            }

            uiState.error?.takeIf { it.isNotBlank() }?.let { message ->
                item {
                    InlineErrorCard(
                        message = message,
                        onRetry = onRefreshAll,
                    )
                }
            }

            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            items(actionRows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { action ->
                        Box(modifier = Modifier.weight(1f)) {
                            QuickActionCard(
                                action = action,
                                onClick = { onActionClick(action.route) },
                            )
                        }
                    }
                    if (row.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Recent Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = onRefreshAll) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("刷新")
                    }
                }
            }

            when {
                refreshState is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                refreshState is LoadState.Error && lazyPagingItems.itemCount == 0 -> {
                    item {
                        InlineErrorCard(
                            message = refreshState.error.message ?: "載入近期通知失敗，請稍後再試。",
                            onRetry = onRetryPaging,
                        )
                    }
                }

                lazyPagingItems.itemCount == 0 -> {
                    item {
                        EmptyRecentNotificationsCard()
                    }
                }

                else -> {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = { index -> lazyPagingItems[index]?.id ?: "home_recent_$index" },
                    ) { index ->
                        val notification = lazyPagingItems[index] ?: return@items
                        RecentNotificationCard(
                            notification = notification,
                            onClick = { onActionClick(Routes.NOTIFICATIONS) },
                        )
                    }

                    when (appendState) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        is LoadState.Error -> {
                            item {
                                InlineErrorCard(
                                    message = appendState.error.message ?: "載入更多通知失敗",
                                    onRetry = onRetryPaging,
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

@Composable
fun ProfileSummaryCard(
    displayName: String,
    email: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit,
) {
    val icon = when (action.route) {
        Routes.PROFILE -> Icons.Default.Person
        Routes.SETTINGS -> Icons.Default.Settings
        Routes.NOTIFICATIONS -> Icons.Default.Notifications
        else -> Icons.Default.Person
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun RecentNotificationCard(
    notification: Notification,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            UnreadDot(isRead = notification.isRead)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDashboardRelativeTime(notification.createdAt),
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
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notification.type.toLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
private fun EmptyRecentNotificationsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "目前沒有近期通知",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InlineErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onRetry) {
                Text("重試")
            }
        }
    }
}

private fun NotificationType.toLabel(): String = when (this) {
    NotificationType.SYSTEM -> "系統通知"
    NotificationType.PROMOTION -> "行銷活動"
    NotificationType.ACTIVITY -> "活動提醒"
}

private fun formatDashboardRelativeTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val diff = max(0L, now - createdAt)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    if (seconds < 60) return "剛剛"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    if (minutes < 60) return "${minutes} 分鐘前"
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    if (hours < 24) return "${hours} 小時前"
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return "${days} 天前"
}
