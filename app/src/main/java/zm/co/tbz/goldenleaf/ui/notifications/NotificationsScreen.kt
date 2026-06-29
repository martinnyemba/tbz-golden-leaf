package zm.co.tbz.goldenleaf.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import zm.co.tbz.goldenleaf.ui.components.glVerticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSearchBar
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState()
    val state by viewModel.state.collectAsState()
    val c = glColors()
    var filter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val unreadCount = notifications.count { !it.is_read }
    val visible = remember(notifications, filter, searchQuery) {
        notifications.filter { notification ->
            val matchesFilter = when (filter) {
                "Unread" -> !notification.is_read
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                notification.subject.contains(searchQuery, ignoreCase = true) ||
                notification.message.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(
                title = "Notifications",
                subtitle = if (unreadCount > 0) "$unreadCount unread" else null,
                onBack = onBack,
                actions = {
                    if (unreadCount > 0) {
                        GlButton(
                            text = "Mark all",
                            onClick = { visible.filter { !it.is_read }.forEach { viewModel.markAsRead(it.id) } },
                            variant = GlButtonVariant.Ghost,
                            size = GlButtonSize.Sm,
                            fillMaxWidth = false,
                        )
                    }
                },
            )
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .glVerticalScroll()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search notifications",
                    )
                    GlFilterPills(
                        options = listOf("All", "Unread", "Favorite", "Archive"),
                        selected = filter,
                        onSelect = { filter = it },
                    )
                    state.error?.let { ErrorText(it) }
                    if (visible.isEmpty()) {
                        GlEmptyState(
                            title = if (notifications.isEmpty()) "No notifications yet" else "No matches",
                            icon = "bell",
                            subtitle = if (notifications.isEmpty()) "Pull to refresh from the server" else "Try a different filter",
                        )
                    } else {
                        visible.forEach { notification ->
                            NotificationCard(
                                subject = notification.subject,
                                message = notification.message,
                                time = notification.created_at,
                                reference = notification.reference,
                                unread = !notification.is_read,
                                onClick = { if (!notification.is_read) viewModel.markAsRead(notification.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    subject: String,
    message: String,
    time: String,
    reference: String?,
    unread: Boolean,
    onClick: () -> Unit,
) {
    val c = glColors()
    GlCard(
        onClick = onClick,
        accent = if (unread) GlAccent.Primary else GlAccent.None,
        contentPadding = 14.dp,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BoxIcon(tone = if (unread) GlTone.Primary else GlTone.Default, icon = "bell")
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        subject,
                        color = c.text,
                        fontSize = 14.sp,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(time, color = c.textSubtle, fontSize = 11.sp)
                }
                Text(message, color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                if (!reference.isNullOrBlank()) {
                    GlPill(
                        text = "Ref · $reference",
                        tone = GlTone.Default,
                        size = GlPillSize.Sm,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxIcon(tone: GlTone, icon: String) {
    val c = glColors()
    val (bg, fg) = when (tone) {
        GlTone.Primary -> c.primarySoft to c.primary
        GlTone.Gold -> c.goldSoft to c.goldDeep
        GlTone.Danger -> c.dangerSoft to c.danger
        GlTone.Info -> c.infoSoft to c.info
        else -> c.surfaceAlt to c.text
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        GlIcon(icon, size = 20.dp, tint = fg)
    }
}
