package zm.co.tbz.goldenleaf.ui.registration

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEditEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerRegistrationEntity
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlAvatar
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFab
import zm.co.tbz.goldenleaf.ui.components.GlFieldRow
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlImageSlot
import zm.co.tbz.goldenleaf.ui.components.GlKpiTile
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSearchBar
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors

@Composable
fun RegistrationHubScreen(
    onNewRegistration: () -> Unit,
    onGrowerList: () -> Unit,
    onLocalRegistrations: () -> Unit,
    onGrowerUpdates: () -> Unit,
    onCorrections: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    GrowerListScreen(
        onBack = null,
        onNewRegistration = onNewRegistration,
        onScanId = onNewRegistration,
        onOpenDetail = { onGrowerList() },
        onOpenUpdates = onGrowerUpdates,
        viewModel = viewModel,
    )
}

/** 09 · Grower list */
@Composable
fun GrowerListScreen(
    onOpenDetail: (String) -> Unit,
    onNewRegistration: () -> Unit,
    onScanId: () -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenUpdates: (() -> Unit)? = null,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val growers by viewModel.displayedGrowers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.hubStats.collectAsState()
    val c = glColors()
    val subtitle = if (stats.totalGrowers > 0) {
        "${stats.totalGrowers} registered · ${stats.pendingSync} pending sync"
    } else {
        "128 registered · 4 pending sync"
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(containerColor = c.bg) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                GlScreenHeader(
                    title = "Growers",
                    subtitle = subtitle,
                    onBack = onBack,
                    actions = {
                        GlButton(
                            text = "New",
                            onClick = onNewRegistration,
                            size = GlButtonSize.Sm,
                            fillMaxWidth = false,
                            leadingIcon = "plus",
                        )
                    },
                )
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlSearchBar(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchChange,
                        placeholder = "Search by name, NRC, ID…",
                        onScan = onScanId,
                    )
                    GlFilterPills(
                        options = GrowerFormChoices.listStatusFilters,
                        selected = uiState.statusFilter,
                        onSelect = viewModel::onStatusFilterChange,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlKpiTile(label = "Active", value = if (stats.totalGrowers > 0) stats.synced.toString() else "118", tone = GlTone.Primary, modifier = Modifier.weight(1f))
                        GlKpiTile(
                            label = "Pending",
                            value = if (stats.pendingSync > 0) stats.pendingSync.toString() else "6",
                            tone = GlTone.Gold,
                            modifier = Modifier
                                .weight(1f)
                                .then(if (onOpenUpdates != null) Modifier.clickable(onClick = onOpenUpdates) else Modifier),
                        )
                        GlKpiTile(label = "High risk", value = "4", tone = GlTone.Danger, modifier = Modifier.weight(1f))
                    }
                }
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(growers, key = { it.localId }) { grower ->
                        GrowerListCard(grower = grower, onClick = {
                            if (!grower.isPreview) onOpenDetail(grower.localId)
                            else onOpenDetail("preview-1")
                        })
                    }
                }
            }
        }
        GlFab(
            onClick = onNewRegistration,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp),
        )
    }
}

@Composable
private fun GrowerListCard(grower: GrowerListItem, onClick: () -> Unit) {
    val c = glColors()
    GlCard(
        onClick = onClick,
        accent = if (grower.flagged) GlAccent.Gold else GlAccent.None,
        contentPadding = 14.dp,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlAvatar(name = grower.name, size = 44.dp, gold = grower.flagged)
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(grower.name, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            grower.tbzId,
                            color = c.textMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GlPill(text = grower.statusLabel, tone = grower.statusTone, size = GlPillSize.Sm)
                        if (grower.syncStatus != "synced") {
                            GlSyncChip(status = grower.syncStatus)
                        }
                    }
                }
                Text(grower.subtitle, color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    GlPill(text = "Risk · ${grower.riskLabel}", tone = grower.riskTone, size = GlPillSize.Sm)
                    if (grower.flagged) {
                        GlPill(text = "Flagged", tone = GlTone.Gold, size = GlPillSize.Sm, leadingIcon = "warning")
                    }
                }
            }
        }
    }
}

/** 17 · Grower updates queue */
@Composable
fun GrowerUpdatesScreen(
    onBack: (() -> Unit)? = null,
    onOpenGrower: (String) -> Unit,
    onEdit: (String) -> Unit = onOpenGrower,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val items by viewModel.displayedGrowerUpdates.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(
                title = "Grower update queue",
                subtitle = "Pending edits waiting to sync",
                onBack = onBack,
                actions = {
                    GlButton(
                        text = "Sync all",
                        onClick = viewModel::syncAllPending,
                        variant = GlButtonVariant.Secondary,
                        size = GlButtonSize.Sm,
                        fillMaxWidth = false,
                        leadingIcon = "cloud-up",
                    )
                },
            )
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlKpiTile(label = "Pending", value = "2", tone = GlTone.Gold, modifier = Modifier.weight(1f))
                    GlKpiTile(label = "Failed", value = "1", tone = GlTone.Danger, modifier = Modifier.weight(1f))
                    GlKpiTile(label = "Synced", value = "1", tone = GlTone.Success, modifier = Modifier.weight(1f))
                }
                GlFilterPills(
                    options = GrowerFormChoices.updateQueueFilters,
                    selected = uiState.updateQueueFilter,
                    onSelect = viewModel::onUpdateQueueFilterChange,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items.forEach { item ->
                        GrowerUpdateCard(item = item, onEdit = { onEdit(item.localId) }, onSync = viewModel::syncAllPending)
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowerUpdateCard(
    item: GrowerUpdateQueueItem,
    onEdit: () -> Unit,
    onSync: () -> Unit,
) {
    val c = glColors()
    val tone = when (item.status.lowercase()) {
        "pending" -> GlTone.Warning
        "synced" -> GlTone.Success
        "failed" -> GlTone.Danger
        "needs_review" -> GlTone.Warning
        else -> GlTone.Default
    }
    val statusLabel = item.status.replace('_', ' ').replaceFirstChar { it.titlecase() }
    GlCard(
        accent = if (item.status == "failed" || item.status == "needs_review") GlAccent.Gold else GlAccent.None,
        contentPadding = 14.dp,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            GlAvatar(name = item.growerName, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(item.growerName, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    GlPill(text = statusLabel, tone = tone, size = GlPillSize.Sm)
                }
                Text(item.tbzId, color = c.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                Text("Changed: ${item.changedFields}", color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Text(item.timestamp, color = c.textSubtle, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                item.error?.let { err ->
                    GlBanner(
                        title = err,
                        tone = if (item.status == "needs_review") GlTone.Warning else GlTone.Danger,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                if (item.status != "synced") {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (item.status != "synced") {
                            GlButton(text = "Edit", onClick = onEdit, variant = GlButtonVariant.Outline, size = GlButtonSize.Sm, fillMaxWidth = false, leadingIcon = "edit")
                            GlButton(text = "Sync", onClick = onSync, variant = GlButtonVariant.Secondary, size = GlButtonSize.Sm, fillMaxWidth = false, leadingIcon = "sync")
                            GlButton(text = "Discard", onClick = {}, variant = GlButtonVariant.Ghost, size = GlButtonSize.Sm, fillMaxWidth = false, leadingIcon = "trash")
                        }
                    }
                }
            }
        }
    }
}

/** 16 · Grower profile */
@Composable
fun GrowerDetailScreen(
    localId: String,
    onBack: (() -> Unit)? = null,
    onEdit: (String) -> Unit,
    onCorrection: (String) -> Unit,
    onAddCrop: (String) -> Unit,
    onNewPermit: () -> Unit = {},
    onNewInspection: () -> Unit = {},
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val grower by viewModel.observeGrower(localId).collectAsState()
    var tab by remember { mutableStateOf("Overview") }
    val c = glColors()
    val usePreview = grower == null || localId.startsWith("preview")
    val name = grower?.let { "${it.first_name} ${it.last_name}" } ?: "Mary Phiri"
    val tbzId = grower?.tbz_id ?: "TBZ-2024-04412"
    val returned = grower?.status == "RETURNED_FOR_CORRECTION" || usePreview

    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(c.surface).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (returned) {
                    GlButton(
                        text = "Fix & Resubmit",
                        onClick = { onCorrection(localId) },
                        variant = GlButtonVariant.Gold,
                        leadingIcon = "warning",
                        fillMaxWidth = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                GlButton(
                    text = "Permit",
                    onClick = onNewPermit,
                    variant = GlButtonVariant.Outline,
                    leadingIcon = "permit",
                    fillMaxWidth = false,
                    modifier = Modifier.weight(1f),
                )
                GlButton(
                    text = "Inspect",
                    onClick = onNewInspection,
                    leadingIcon = "inspection",
                    fillMaxWidth = false,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(c.primaryDeep, c.primary)))) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center,
                        ) {
                            GlIcon("arrow-left", size = 20.dp, tint = Color.White)
                        }
                    }
                    Text(
                        "Grower",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = if (onBack != null) 12.dp else 0.dp).weight(1f),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlAvatar(name = name, size = 64.dp, gold = true)
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(tbzId, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            if (returned) {
                                GlPill(text = "Returned for correction", tone = GlTone.Returned, size = GlPillSize.Sm, leadingIcon = "warning")
                            }
                            GlPill(text = "Active", tone = GlTone.Default, size = GlPillSize.Sm)
                        }
                    }
                }
            }
            GlCard(
                elevated = true,
                contentPadding = 0.dp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = (-20).dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    listOf(
                        Triple("Area", "4.2", "ha"),
                        Triple("Permits", "7", "issued"),
                        Triple("Yield", "5.8t", "last yr"),
                        Triple("Risk", "78", "score"),
                    ).forEachIndexed { index, (label, value, sub) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                value,
                                color = if (label == "Risk") c.danger else c.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(label.uppercase(), color = c.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            Text(sub, color = c.textSubtle, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
                        }
                        if (index < 3) {
                            Box(Modifier.size(width = 1.dp, height = 48.dp).background(c.outlineSoft))
                        }
                    }
                }
            }
            GlFilterPills(
                options = listOf("Overview", "Inspections", "Permits", "Sales", "Documents"),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )
            when (tab) {
                "Overview" -> GrowerDetailOverview()
                "Inspections" -> GrowerDetailInspectionsTab()
                "Permits" -> GrowerDetailPermitsTab()
                "Sales" -> GrowerDetailSalesTab()
                "Documents" -> GrowerDetailDocumentsTab()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GrowerDetailOverview() {
    val c = glColors()
    Column {
        GlCard(accent = GlAccent.Gold, contentPadding = 14.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                GlIcon("warning", size = 20.dp, tint = c.gold)
                Column {
                    Text("High-risk indicators", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("• Unverified curing barn capacity", color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    Text("• Estimated yield exceeds field area benchmark", color = c.textMuted, fontSize = 12.sp)
                    Text("• Open dispute · ARB-1182", color = c.textMuted, fontSize = 12.sp)
                }
            }
        }
        GlSectionHeader(title = "Identity", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        GlCard(contentPadding = 14.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            GlFieldRow(label = "NRC", value = "224018/61/1", mono = true)
            GlDivider()
            GlFieldRow(label = "DOB / Gender", value = "14 Jun 1986 · Female")
            GlDivider()
            GlFieldRow(label = "Phone", value = "+260 977 421 089")
            GlDivider()
            GlFieldRow(label = "Cooperative", value = "Eastern Tobacco Farmers Coop")
            GlDivider()
            GlFieldRow(label = "Registered", value = "12 March 2024")
        }
        GlSectionHeader(title = "Farm", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        GlCard(contentPadding = 0.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            GlImageSlot(label = "map · -13.850, 32.502", height = 120.dp, rounded = 0.dp)
            Column(Modifier.padding(14.dp)) {
                GlFieldRow(label = "Province · District", value = "Eastern · Chadiza")
                GlDivider()
                GlFieldRow(label = "Village · Chief", value = "Mphangwe · Mlolo")
                GlDivider()
                GlFieldRow(label = "GPS", value = "-13.85044, 32.50217", mono = true)
                GlDivider()
                GlFieldRow(label = "Tobacco type", value = "Burley")
                GlDivider()
                GlFieldRow(label = "Tobacco area", value = "3.8 ha (4.2 total)")
                GlDivider()
                GlFieldRow(label = "Curing", value = "Open shed · 1 unit")
            }
        }
        GlSectionHeader(title = "Recent activity", action = "View all", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        GlCard(contentPadding = 4.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            GlRow(title = "Field inspection · passed", subtitle = "11 Apr 2025 · Inspector Banda", leadingIcon = "inspection", tone = GlTone.Primary, trailing = { GlPill(text = "Pass", tone = GlTone.Success, size = GlPillSize.Sm) })
            GlDivider()
            GlRow(title = "Permit PRM-9742 issued", subtitle = "04 Apr 2025 · 18 bales · 524 kg", leadingIcon = "permit", tone = GlTone.Gold, trailing = { GlPill(text = "Active", tone = GlTone.Success, size = GlPillSize.Sm) })
            GlDivider()
            GlRow(title = "Sale captured at Lilayi floor", subtitle = "22 Mar 2025 · K8,420 · 18 bales", leadingIcon = "bale", tone = GlTone.Gold)
        }
    }
}

@Composable
private fun GrowerDetailInspectionsTab() {
    val c = glColors()
    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("Field inspection", "11 Apr 2025", "4 of 4 checks passed"),
            Triple("Nursery inspection", "20 Feb 2025", "3 of 3 checks passed"),
            Triple("Pre-season visit", "18 Jan 2025", "Curing barn capacity unverified"),
        ).forEach { (title, date, sub) ->
            GlCard(contentPadding = 14.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    GlPill(text = title, tone = GlTone.Primary, size = GlPillSize.Sm)
                    GlPill(text = if (sub.contains("unverified")) "Review" else "Pass", tone = if (sub.contains("unverified")) GlTone.Warning else GlTone.Success, size = GlPillSize.Sm)
                }
                Text(date, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                Text(sub, color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun GrowerDetailPermitsTab() {
    val c = glColors()
    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("PRM-9821", "Active", "24 bales · 712 kg · valid 12-19 May"),
            Triple("PRM-9742", "Used", "18 bales · 524 kg · 04 Apr 2025"),
            Triple("PRM-9601", "Used", "12 bales · 348 kg · 22 Mar 2025"),
        ).forEach { (id, status, sub) ->
            GlCard(contentPadding = 14.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(id, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    GlPill(text = status, tone = if (status == "Active") GlTone.Success else GlTone.Default, size = GlPillSize.Sm)
                }
                Text(sub, color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun GrowerDetailSalesTab() {
    val c = glColors()
    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("Lilayi sales floor", "22 Mar 2025 · 18 bales · 524 kg", "K 8,420"),
            Triple("Kabwe sales floor", "04 Mar 2025 · 12 bales · 348 kg", "K 5,610"),
            Triple("Kabwe sales floor", "14 Feb 2025 · 6 bales · 174 kg", "K 2,740"),
        ).forEach { (floor, meta, amount) ->
            GlCard(contentPadding = 14.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text(floor, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(meta, color = c.textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(amount, color = c.gold, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun GrowerDetailDocumentsTab() {
    val c = glColors()
    GlCard(contentPadding = 4.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        listOf(
            "NRC · Front" to "12 Mar 2024",
            "NRC · Back" to "12 Mar 2024",
            "Farm overview photo" to "12 Mar 2024",
            "Curing barn photo" to "11 Apr 2025",
            "Signed consent form" to "12 Mar 2024",
        ).forEachIndexed { index, (title, date) ->
            GlRow(
                title = title,
                subtitle = date,
                leadingIcon = "image",
                tone = GlTone.Primary,
                trailing = { GlIcon("chevron-right", size = 18.dp, tint = c.textSubtle) },
            )
            if (index < 4) GlDivider()
        }
    }
}

@Composable
fun LocalRegistrationsScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val registrations by viewModel.filteredRegistrations.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlScreenHeader(title = "Local registrations", subtitle = "${registrations.size} drafts on this device")
            GlFilterPills(
                options = GrowerFormChoices.syncFilters,
                selected = uiState.syncFilter,
                onSelect = viewModel::onSyncFilterChange,
            )
            GlButton(text = "Sync all pending", onClick = viewModel::syncAllPending, leadingIcon = "sync")
            if (registrations.isEmpty()) {
                GlEmptyState(title = "No local registrations", subtitle = "New drafts queued offline will appear here.", icon = "document")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(registrations, key = { it.local_id }) { reg ->
                        GlCard(onClick = { onOpenDetail(reg.local_id) }) { RegistrationRow(reg) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistrationRow(registration: GrowerRegistrationEntity) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Draft ${registration.local_id.take(8)}…", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = glColors().text)
            Text("Created ${registration.created_at}", fontSize = 11.sp, color = glColors().textMuted)
        }
        GlSyncChip(status = registration.sync_status)
    }
}

@Composable
private fun GrowerEditRow(edit: GrowerEditEntity) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Edit ${edit.local_id.take(8)}…", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = glColors().text)
            Text("Grower ${edit.grower_local_id.take(8)}…", fontSize = 11.sp, color = glColors().textMuted)
        }
        GlSyncChip(status = edit.sync_status)
    }
}
