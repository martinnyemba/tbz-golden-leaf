package zm.co.tbz.goldenleaf.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.glColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState()
    val state by viewModel.state.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Notifications")
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.error?.let { ErrorText(it) }
                    if (notifications.isEmpty()) {
                        GlEmptyState(
                            title = "No notifications yet",
                            icon = "bell",
                            subtitle = "Pull to refresh from the server",
                        )
                    } else {
                        notifications.forEach { notification ->
                            GlCard(
                                onClick = { if (!notification.is_read) viewModel.markAsRead(notification.id) },
                                accent = if (notification.is_read) GlAccent.None else GlAccent.Primary,
                                contentPadding = 14.dp,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        notification.subject,
                                        color = c.text,
                                        fontSize = 14.sp,
                                        fontWeight = if (notification.is_read) FontWeight.Normal else FontWeight.Bold,
                                    )
                                    Text(notification.message, color = c.textMuted, fontSize = 13.sp)
                                    Text(notification.created_at, color = c.textSubtle, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
