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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import zm.co.tbz.goldenleaf.ui.components.glVerticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val subtitle = "${stats.totalGrowers} growers · ${stats.pendingSync} pending sync"

    LaunchedEffect(Unit) { viewModel.refreshGrowers() }

    Box(Modifier.fillMaxSize()) {
        GlScaffold(containerColor = c.bg) { padding ->
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
                        GlKpiTile(
                            label = "Synced",
                            value = stats.synced.toString(),
                            tone = GlTone.Primary,
                            modifier = Modifier.weight(1f),
                        )
                        GlKpiTile(
                            label = "Pending",
                            value = stats.pendingSync.toString(),
                            tone = GlTone.Gold,
                            modifier = Modifier
                                .weight(1f)
                                .then(if (onOpenUpdates != null) Modifier.clickable(onClick = onOpenUpdates) else Modifier),
                        )
                        GlKpiTile(
                            label = "Failed",
                            value = stats.failed.toString(),
                            tone = GlTone.Danger,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    uiState.growersLoadError?.let { err ->
                        GlBanner(title = err, tone = GlTone.Warning, icon = "warning")
                    }
                }
                if (growers.isEmpty() && !uiState.isLoadingGrowers) {
                    GlEmptyState(
                        title = "No growers found",
                        subtitle = "Try another search or sync when back online.",
                        icon = "profile",
                        modifier = Modifier.weight(1f).padding(20.dp),
                    )
                } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(growers, key = { it.localId }) { grower ->
                        GrowerListCard(grower = grower, onClick = { onOpenDetail(grower.localId) })
                    }
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
    val queueStats by viewModel.updateQueueStats.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
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
            }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlKpiTile(label = "Pending", value = queueStats.first.toString(), tone = GlTone.Gold, modifier = Modifier.weight(1f))
                    GlKpiTile(label = "Failed", value = queueStats.second.toString(), tone = GlTone.Danger, modifier = Modifier.weight(1f))
                    GlKpiTile(label = "Synced", value = queueStats.third.toString(), tone = GlTone.Success, modifier = Modifier.weight(1f))
                }
            }
            item {
                GlFilterPills(
                    options = GrowerFormChoices.updateQueueFilters,
                    selected = uiState.updateQueueFilter,
                    onSelect = viewModel::onUpdateQueueFilterChange,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            items(items, key = { it.localId }) { item ->
                GrowerUpdateCard(
                    item = item,
                    onEdit = { onEdit(item.growerLocalId) },
                    onSync = { viewModel.syncEdit(item.localId) },
                    onDiscard = { viewModel.deleteEdit(item.localId) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun GrowerUpdateCard(
    item: GrowerUpdateQueueItem,
    onEdit: () -> Unit,
    onSync: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
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
                            GlButton(text = "Discard", onClick = onDiscard, variant = GlButtonVariant.Ghost, size = GlButtonSize.Sm, fillMaxWidth = false, leadingIcon = "trash")
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
    val grower by remember(localId) { viewModel.observeGrower(localId) }.collectAsState()
    val provinces by viewModel.provinces.collectAsState()
    var tab by remember { mutableStateOf("Overview") }
    val c = glColors()
    val entity = grower
    val name = entity?.let { listOfNotNull(it.first_name, it.middle_name, it.last_name).joinToString(" ") } ?: "Grower"
    val tbzId = entity?.tbz_id ?: entity?.nrc_number ?: localId.take(12)
    val returned = entity?.status == "RETURNED_FOR_CORRECTION"
    val statusLabel = entity?.status?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.titlecase() } ?: "Unknown"

    GlScaffold(
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).glVerticalScroll()) {
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
                            GlPill(text = statusLabel, tone = GlTone.Default, size = GlPillSize.Sm)
                        }
                    }
                }
            }
            GlCard(
                elevated = true,
                contentPadding = 0.dp,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    listOf(
                        Triple("NRC", entity?.nrc_number ?: "—", ""),
                        Triple("Phone", entity?.phone_number ?: "—", ""),
                        Triple("Sync", entity?.sync_status?.replace('_', ' ') ?: "—", ""),
                    ).forEachIndexed { index, (label, value, _) ->
                        val numeric = label == "NRC" || label == "Phone"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                value,
                                color = c.text,
                                fontSize = if (numeric) 12.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (numeric) FontFamily.Monospace else FontFamily.Default,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(label.uppercase(), color = c.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (index < 2) {
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
                "Overview" -> GrowerDetailOverview(entity, provinces)
                "Inspections" -> GrowerDetailEmptyTab("No inspections cached for this grower yet.")
                "Permits" -> GrowerDetailEmptyTab("No permits cached for this grower yet.")
                "Sales" -> GrowerDetailEmptyTab("No sales cached for this grower yet.")
                "Documents" -> GrowerDetailDocumentsTab(entity)
            }
            Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GrowerDetailEmptyTab(message: String) {
    GlEmptyState(title = message, icon = "info", modifier = Modifier.padding(20.dp))
}

@Composable
private fun GrowerDetailOverview(
    grower: GrowerEntity?,
    provinces: List<zm.co.tbz.goldenleaf.data.local.entity.ProvinceEntity>,
) {
    val c = glColors()
    if (grower == null) {
        GlEmptyState(title = "Grower not found locally", subtitle = "Sync growers or check your connection.", icon = "profile", modifier = Modifier.padding(20.dp))
        return
    }
    val provinceName = provinces.firstOrNull { it.id == grower.province }?.name ?: grower.province.orEmpty()
    Column {
        grower.correction_reason?.let { reason ->
            GlCard(accent = GlAccent.Gold, contentPadding = 14.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text("Correction required", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(reason, color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        GlSectionHeader(title = "Identity", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        GlCard(contentPadding = 14.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            GlFieldRow(label = "NRC", value = grower.nrc_number, mono = true)
            GlDivider()
            GlFieldRow(
                label = "DOB / Gender",
                value = listOfNotNull(grower.date_of_birth, grower.sex).joinToString(" · ").ifBlank { "—" },
            )
            GlDivider()
            GlFieldRow(label = "Phone", value = grower.phone_number ?: "—")
            GlDivider()
            GlFieldRow(label = "Email", value = grower.email ?: "—")
            GlDivider()
            GlFieldRow(label = "Category", value = grower.category ?: "—")
        }
        GlSectionHeader(title = "Location", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        GlCard(contentPadding = 14.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            GlFieldRow(label = "Province · District", value = listOf(provinceName, grower.district).filter { !it.isNullOrBlank() }.joinToString(" · "))
            GlDivider()
            GlFieldRow(label = "Address", value = grower.address ?: "—")
            GlDivider()
            GlFieldRow(label = "Town / village", value = grower.town_village ?: "—")
            GlDivider()
            GlFieldRow(
                label = "GPS",
                value = listOfNotNull(grower.gps_latitude, grower.gps_longitude).joinToString(", ").ifBlank { "—" },
                mono = true,
            )
        }
    }
}

@Composable
private fun GrowerDetailDocumentsTab(grower: GrowerEntity?) {
    val c = glColors()
    val docs = listOfNotNull(
        grower?.profile_photo_path?.let { "Profile photo" to "On device" },
        grower?.id_front_path?.let { "ID front" to "On device" },
        grower?.id_back_path?.let { "ID back" to "On device" },
    )
    if (docs.isEmpty()) {
        GrowerDetailEmptyTab("No documents stored locally for this grower.")
        return
    }
    GlCard(contentPadding = 4.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        docs.forEachIndexed { index, (title, date) ->
            GlRow(
                title = title,
                subtitle = date,
                leadingIcon = "image",
                tone = GlTone.Primary,
                trailing = { GlIcon("chevron-right", size = 18.dp, tint = c.textSubtle) },
            )
            if (index < docs.lastIndex) GlDivider()
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

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
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
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
