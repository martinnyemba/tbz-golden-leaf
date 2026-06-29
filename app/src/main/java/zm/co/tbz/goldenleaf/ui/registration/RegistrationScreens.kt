package zm.co.tbz.goldenleaf.ui.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEditEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerRegistrationEntity
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlAvatar
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFieldRow
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlKpiTile
import zm.co.tbz.goldenleaf.ui.components.GlPill
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
    val stats by viewModel.hubStats.collectAsState()
    val registrations by viewModel.registrations.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlScreenHeader(title = "Registration", subtitle = "${stats.totalGrowers} growers · ${stats.pendingSync} pending sync")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlKpiTile(label = "Growers", value = stats.totalGrowers.toString(), tone = GlTone.Primary, icon = "users", modifier = Modifier.weight(1f))
                GlKpiTile(label = "Pending", value = stats.pendingSync.toString(), tone = GlTone.Gold, icon = "sync", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlKpiTile(label = "Synced", value = stats.synced.toString(), tone = GlTone.Success, icon = "check-circle", modifier = Modifier.weight(1f))
                GlKpiTile(label = "Failed", value = stats.failed.toString(), tone = GlTone.Danger, icon = "warning", modifier = Modifier.weight(1f))
            }
            GlSectionHeader(title = "Quick access")
            GlCard(accent = GlAccent.Primary) {
                Column {
                    GlRow(title = "New grower registration", subtitle = "Two-step offline-first capture", leadingIcon = "plus", tone = GlTone.Primary, onClick = onNewRegistration)
                    GlDivider()
                    GlRow(title = "Portal growers list", subtitle = "Search cached growers", leadingIcon = "users", onClick = onGrowerList)
                    GlDivider()
                    GlRow(title = "Local registrations", subtitle = "Drafts and pending sync", leadingIcon = "sync", onClick = onLocalRegistrations)
                    GlDivider()
                    GlRow(title = "Grower updates queue", subtitle = "Pending PATCH edits", leadingIcon = "edit", onClick = onGrowerUpdates)
                    GlDivider()
                    GlRow(title = "Corrections inbox", subtitle = "Returned-for-correction", leadingIcon = "warning", tone = GlTone.Gold, onClick = onCorrections)
                }
            }
            if (registrations.isNotEmpty()) {
                GlSectionHeader(title = "Recent local saves")
                GlCard {
                    Column {
                        registrations.take(3).forEachIndexed { i, reg ->
                            RegistrationRow(reg)
                            if (i < registrations.take(3).lastIndex) GlDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GrowerListScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val growers by viewModel.filteredGrowers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.hubStats.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Growers", subtitle = "${stats.totalGrowers} registered · ${stats.pendingSync} pending sync")
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlSearchBar(value = uiState.searchQuery, onValueChange = viewModel::onSearchChange, placeholder = "Search by name, NRC, ID…")
                GlFilterPills(
                    options = GrowerFormChoices.statusFilters,
                    selected = uiState.statusFilter,
                    onSelect = viewModel::onStatusFilterChange,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlKpiTile(label = "Synced", value = stats.synced.toString(), tone = GlTone.Primary, icon = "check-circle", modifier = Modifier.weight(1f))
                    GlKpiTile(label = "Pending", value = stats.pendingSync.toString(), tone = GlTone.Gold, icon = "sync", modifier = Modifier.weight(1f))
                    GlKpiTile(label = "Failed", value = stats.failed.toString(), tone = GlTone.Danger, icon = "warning", modifier = Modifier.weight(1f))
                }
            }
            if (growers.isEmpty()) {
                GlEmptyState(title = "No growers found", subtitle = "Try a different search or filter.", icon = "users")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(growers, key = { it.local_id }) { grower ->
                        GrowerCard(grower = grower, onClick = { onOpenDetail(grower.local_id) })
                    }
                }
            }
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
fun GrowerUpdatesScreen(
    onOpenGrower: (String) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val edits by viewModel.edits.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlScreenHeader(title = "Grower updates", subtitle = "${edits.size} pending PATCH edits")
            GlButton(text = "Sync all pending updates", onClick = viewModel::syncAllPending, leadingIcon = "sync")
            if (edits.isEmpty()) {
                GlEmptyState(title = "No pending updates", subtitle = "Grower edits made offline will appear here.", icon = "edit")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(edits, key = { it.local_id }) { edit ->
                        GlCard(onClick = { onOpenGrower(edit.grower_local_id) }) { GrowerEditRow(edit) }
                    }
                }
            }
        }
    }
}

@Composable
fun GrowerDetailScreen(
    localId: String,
    onEdit: (String) -> Unit,
    onCorrection: (String) -> Unit,
    onAddCrop: (String) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val grower by viewModel.observeGrower(localId).collectAsState()
    val c = glColors()

    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            val item = grower
            if (item != null) {
                Row(
                    Modifier.fillMaxWidth().background(c.surface).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (item.status == "RETURNED_FOR_CORRECTION") {
                        GlButton(
                            text = "Fix & Resubmit",
                            onClick = { onCorrection(localId) },
                            variant = GlButtonVariant.Gold,
                            leadingIcon = "warning",
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        GlButton(text = "Edit", onClick = { onEdit(localId) }, variant = GlButtonVariant.Outline, leadingIcon = "edit", modifier = Modifier.weight(1f))
                        GlButton(text = "Add crop", onClick = { onAddCrop(localId) }, leadingIcon = "leaf", modifier = Modifier.weight(1f))
                    }
                }
            }
        },
    ) { padding ->
        val item = grower
        if (item == null) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                GlScreenHeader(title = "Grower")
                GlEmptyState(title = "Grower not found", icon = "users")
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(c.primaryDeep, c.primary)))) {
                    Text(
                        "Grower",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        GlAvatar(name = "${item.first_name} ${item.last_name}", size = 64.dp, gold = true)
                        Column(Modifier.weight(1f)) {
                            Text("${item.first_name} ${item.last_name}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            item.tbz_id?.let {
                                Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val tone = when (item.status) {
                                    "RETURNED_FOR_CORRECTION" -> GlTone.Gold
                                    "ACTIVE", "APPROVED" -> GlTone.Success
                                    "REJECTED", "SUSPENDED" -> GlTone.Danger
                                    else -> GlTone.Default
                                }
                                GlPill(text = item.status.replace('_', ' '), tone = tone)
                            }
                        }
                    }
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    GlSyncChip(status = item.sync_status)
                    item.last_sync_error?.let {
                        GlFieldRow(label = "Sync error", value = it)
                    }
                    GlSectionHeader(title = "Identity")
                    GlCard {
                        Column {
                            GlFieldRow(label = "NRC", value = item.nrc_number, mono = true)
                            GlFieldRow(label = "Phone", value = item.phone_number.orEmpty().ifBlank { "—" })
                            GlFieldRow(label = "Email", value = item.email.orEmpty().ifBlank { "—" })
                        }
                    }
                    GlSectionHeader(title = "Farm")
                    GlCard {
                        Column {
                            GlFieldRow(label = "Province · District", value = "${item.province.orEmpty().ifBlank { "—" }} · ${item.district.orEmpty().ifBlank { "—" }}")
                            GlFieldRow(label = "Town / village", value = item.town_village.orEmpty().ifBlank { "—" })
                            val gps = if (item.gps_latitude != null && item.gps_longitude != null) {
                                "${item.gps_latitude}, ${item.gps_longitude}"
                            } else "—"
                            GlFieldRow(label = "GPS", value = gps, mono = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowerCard(grower: GrowerEntity, onClick: () -> Unit) {
    GlCard(onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlAvatar(name = "${grower.first_name} ${grower.last_name}", size = 44.dp)
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${grower.first_name} ${grower.last_name}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = glColors().text)
                        grower.tbz_id?.let { Text(it, fontSize = 11.sp, color = glColors().textMuted) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val tone = when (grower.status) {
                            "ACTIVE", "APPROVED" -> GlTone.Success
                            "RETURNED_FOR_CORRECTION" -> GlTone.Gold
                            "REJECTED", "SUSPENDED" -> GlTone.Danger
                            else -> GlTone.Default
                        }
                        GlPill(text = grower.status.replace('_', ' '), tone = tone)
                        if (grower.sync_status != "synced") {
                            Spacer(Modifier.height(2.dp))
                            GlSyncChip(status = grower.sync_status)
                        }
                    }
                }
                Text("NRC ${grower.nrc_number}", fontSize = 12.sp, color = glColors().textMuted)
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
