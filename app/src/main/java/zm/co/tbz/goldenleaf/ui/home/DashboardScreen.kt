package zm.co.tbz.goldenleaf.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlKpiTile
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.LoadingBox
import zm.co.tbz.goldenleaf.ui.components.TbzMark
import zm.co.tbz.goldenleaf.ui.components.glColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenRegistration: () -> Unit,
    onOpenCorrections: () -> Unit,
    onOpenSync: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Compact header — logo mark, greeting, sync status.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TbzMark(size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Welcome back",
                            color = c.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            profile?.full_name ?: "Officer",
                            color = c.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    GlSyncChip(status = if (state.isLoading) "syncing" else "synced")
                }

                state.error?.let { ErrorText(it) }

                if (state.isLoading && state.dashboard == null) {
                    LoadingBox()
                } else {
                    val dash = state.dashboard

                    GlSectionHeader(title = "Overview")
                    val kpis = listOf(
                        Triple("Pending registrations", dash?.pending_registrations, GlTone.Primary to "document"),
                        Triple("Returned registrations", dash?.returned_registrations, GlTone.Danger to "edit"),
                        Triple("Pending permits", dash?.pending_permits, GlTone.Gold to "permit"),
                        Triple("Returned permits", dash?.returned_permits, GlTone.Danger to "permit"),
                        Triple("Scheduled inspections", dash?.scheduled_inspections, GlTone.Info to "inspection"),
                        Triple("Bales today", dash?.bales_today, GlTone.Success to "bale"),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        kpis.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                pair.forEach { (title, value, toneIcon) ->
                                    val (tone, icon) = toneIcon
                                    GlKpiTile(
                                        label = title,
                                        value = (value ?: 0).toString(),
                                        tone = tone,
                                        icon = icon,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    GlSectionHeader(title = "Quick access")
                    GlCard(accent = GlAccent.Primary) {
                        Column {
                            GlRow(
                                title = "Registration",
                                subtitle = "Register and manage growers",
                                leadingIcon = "document",
                                tone = GlTone.Primary,
                                onClick = onOpenRegistration,
                            )
                            GlDivider()
                            GlRow(
                                title = "Corrections Inbox",
                                subtitle = "Review returned submissions",
                                leadingIcon = "edit",
                                tone = GlTone.Gold,
                                onClick = onOpenCorrections,
                            )
                            GlDivider()
                            GlRow(
                                title = "Sync Settings",
                                subtitle = "Manage offline data sync",
                                leadingIcon = "sync",
                                tone = GlTone.Default,
                                onClick = onOpenSync,
                            )
                        }
                    }
                }
            }
        }
    }
}
