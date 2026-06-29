package zm.co.tbz.goldenleaf.ui.home

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlSearchBar
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.LoadingBox
import zm.co.tbz.goldenleaf.ui.components.TbzMark
import zm.co.tbz.goldenleaf.ui.components.glColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenRegistration: () -> Unit,
    onOpenPermits: () -> Unit,
    onOpenInspection: () -> Unit,
    onOpenMarketing: () -> Unit,
    onOpenCorrections: () -> Unit,
    onOpenArbitration: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPermitValidate: () -> Unit,
    onOpenInspectionDetail: (String) -> Unit,
    onOpenMenu: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val inspections by viewModel.inspections.collectAsState()
    val highRiskGrowerIds by viewModel.highRiskGrowerIds.collectAsState()
    val pendingSync by viewModel.pendingSyncCount.collectAsState()
    val unreadNotifications by viewModel.unreadNotificationCount.collectAsState()
    val c = glColors()

    var activePeriod by remember { mutableStateOf("Today") }
    var activeFilter by remember { mutableStateOf("All") }
    val scheduleCards = remember(inspections, highRiskGrowerIds, activePeriod, activeFilter) {
        viewModel.scheduleCards(inspections, highRiskGrowerIds, activePeriod, activeFilter)
    }

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }

    val useHandoffPreview = inspections.isEmpty()
    val displayName = profile?.full_name ?: "Officer"
    val syncChipStatus = when {
        state.isLoading -> "syncing"
        pendingSync > 0 || useHandoffPreview -> "pending"
        else -> "synced"
    }
    val syncChipCount = when {
        pendingSync > 0 -> pendingSync
        useHandoffPreview -> 4
        else -> pendingSync
    }

    Scaffold(containerColor = c.bg) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TbzMark(size = 36.dp)
                        Column {
                            Text(greeting, color = c.textMuted, fontSize = 13.sp)
                            Text(displayName, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    GlSyncChip(
                        status = syncChipStatus,
                        count = syncChipCount,
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(c.surfaceAlt)
                            .clickable(onClick = onOpenNotifications),
                        contentAlignment = Alignment.Center,
                    ) {
                        GlIcon("bell", size = 20.dp, tint = c.text)
                        if (unreadNotifications > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(c.gold),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    unreadNotifications.coerceAtMost(9).toString(),
                                    color = c.onGold,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                }

                GlSearchBar(
                    value = "",
                    onValueChange = {},
                    placeholder = "Search growers, permits, NRC...",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                    readOnly = true,
                    onClick = onOpenSearch,
                    onScan = onOpenPermitValidate,
                )

                Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)) {
                    Text(
                        "Module",
                        color = c.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(onLongPress = { onOpenMenu() })
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        val modules = listOf(
                            ModuleShortcut("Growers", "users", null, onOpenRegistration),
                            ModuleShortcut("Permits", "permit", null, onOpenPermits),
                            ModuleShortcut("Inspect", "inspection", null, onOpenInspection),
                            ModuleShortcut("Sales", "bale", 3, onOpenMarketing),
                            ModuleShortcut("Corrections", "warning", 3, onOpenCorrections),
                            ModuleShortcut("Arbitration", "gavel", null, onOpenArbitration),
                        )
                        modules.forEach { module ->
                            DashboardModuleIcon(
                                label = module.label,
                                icon = module.icon,
                                badge = module.badge,
                                onClick = module.onClick,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(c.surfaceMuted),
                )

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        listOf("Today", "This week").forEach { tab ->
                            val selected = tab == activePeriod
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activePeriod = tab }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    tab,
                                    color = if (selected) c.primary else c.textMuted,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(2.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (selected) c.primary else Color.Transparent),
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.outlineSoft))
                }

                GlFilterPills(
                    options = listOf("All", "Field", "Nursery", "Curing", "Validation"),
                    selected = activeFilter,
                    onSelect = { activeFilter = it },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
                )

                state.error?.let {
                    ErrorText(it, Modifier.padding(horizontal = 20.dp))
                }

                if (state.isLoading && state.dashboard == null) {
                    LoadingBox()
                } else if (scheduleCards.isEmpty()) {
                    Text(
                        "No ${if (activeFilter == "All") "" else "$activeFilter "}inspections ${activePeriod.lowercase()}.",
                        color = c.textMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        scheduleCards.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                row.forEach { card ->
                                    DashboardScheduleCard(
                                        card = card,
                                        onClick = {
                                            if (!card.localId.startsWith("preview")) {
                                                onOpenInspectionDetail(card.localId)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private data class ModuleShortcut(
    val label: String,
    val icon: String,
    val badge: Int?,
    val onClick: () -> Unit,
)

@Composable
private fun DashboardModuleIcon(
    label: String,
    icon: String,
    badge: Int?,
    onClick: () -> Unit,
) {
    val c = glColors()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(c.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            GlIcon(icon, size = 24.dp, tint = c.primary)
            val count = badge ?: 0
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(c.gold),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        count.coerceAtMost(9).toString(),
                        color = c.onGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        Text(label, color = c.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DashboardScheduleCard(
    card: DashboardScheduleCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = glColors()
    GlCard(modifier = modifier, onClick = onClick, elevated = true, contentPadding = 12.dp) {
        Column {
            GlPill(text = card.kind, tone = card.pillTone, size = GlPillSize.Sm)
            Spacer(Modifier.height(8.dp))
            Text(card.growerName, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(
                card.growerId,
                color = c.textMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 3.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlIcon("clock", size = 11.dp, tint = c.textMuted)
                    Text(card.timeLabel, color = c.textMuted, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlIcon("map-pin", size = 11.dp, tint = c.textMuted)
                    Text(card.location, color = c.textMuted, fontSize = 11.sp, maxLines = 1)
                }
            }
            if (card.isHighRisk) {
                Spacer(Modifier.height(8.dp))
                GlPill(text = "High risk", tone = GlTone.Warning, size = GlPillSize.Sm, leadingIcon = "warning")
            }
        }
    }
}
