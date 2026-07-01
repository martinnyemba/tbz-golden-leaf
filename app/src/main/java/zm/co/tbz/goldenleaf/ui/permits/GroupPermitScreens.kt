package zm.co.tbz.goldenleaf.ui.permits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitEntity
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDropdownField
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFieldRow
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlKpiTile
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlStepper
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.QrCodeImage
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.QrScanButton
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn

@Composable
fun GroupPermitHubScreen(
    onCreate: () -> Unit,
    onValidate: () -> Unit,
    onStatusList: () -> Unit,
    viewModel: GroupPermitViewModel = hiltViewModel(),
) {
    val stats by viewModel.hubStats.collectAsState()
    val permits by viewModel.filteredGroupPermits.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Group permits", subtitle = "Multi-grower transport permits")
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlKpiTile("Total", stats.total.toString(), Modifier.weight(1f), icon = "users")
                    GlKpiTile("Pending", stats.pending.toString(), Modifier.weight(1f), tone = GlTone.Gold, icon = "clock")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlKpiTile("Approved", stats.approved.toString(), Modifier.weight(1f), tone = GlTone.Primary, icon = "check-circle")
                    GlKpiTile("Returned", stats.returned.toString(), Modifier.weight(1f), tone = GlTone.Warning, icon = "warning")
                }
                GlSectionHeader(title = "Quick actions")
                GlCard(contentPadding = 4.dp) {
                    Column {
                        GlRow("New group permit", subtitle = "Header + grower entries (min 2)", leadingIcon = "users", onClick = onCreate)
                        GlRow("Validate group permit", subtitle = "Scan / verify group QR token", leadingIcon = "qr", onClick = onValidate)
                        GlRow("Status list", subtitle = "Filter by permit status", leadingIcon = "clipboard", onClick = onStatusList)
                    }
                }
                if (permits.isNotEmpty()) {
                    GlSectionHeader(title = "Recent group permits")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        permits.take(3).forEach { permit -> GroupPermitRow(permit, onClick = {}) }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupPermitCreateScreen(
    onSubmitted: () -> Unit,
    viewModel: GroupPermitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.createState.collectAsState()
    val header = uiState.header
    val provinces by viewModel.provinces.collectAsState()
    val salesFloors by viewModel.salesFloors.collectAsState()
    val growers by viewModel.growers.collectAsState()
    val districts by viewModel.districtsForProvince(header.originProvince)
        .collectAsState(initial = emptyList())
    var entryForm by remember { mutableStateOf(GroupPermitEntryForm()) }
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = if (uiState.step == 1) "Group permit — Header" else "Group permit — Entries")
            GlStepper(step = uiState.step, total = 2)
            when (uiState.step) {
                1 -> ScrollableFormColumn {
                    GlSectionHeader(title = "Step 1 — Header")
                    GlTextField(
                        value = header.licensePlate,
                        onValueChange = { viewModel.updateHeader { h -> h.copy(licensePlate = it) } },
                        label = "License plate",
                        error = uiState.fieldErrors["licensePlate"],
                    )
                    GlDropdownField(
                        label = "Origin province",
                        options = provinces.map { it.id to it.name },
                        selectedId = header.originProvince,
                        onSelected = {
                            viewModel.updateHeader { h -> h.copy(originProvince = it, originDistrict = "") }
                        },
                        error = uiState.fieldErrors["originProvince"],
                    )
                    GlDropdownField(
                        label = "Origin district",
                        options = districts.map { it.id to it.name },
                        selectedId = header.originDistrict,
                        onSelected = { viewModel.updateHeader { h -> h.copy(originDistrict = it) } },
                        error = uiState.fieldErrors["originDistrict"],
                    )
                    GlDropdownField(
                        label = "Destination sales floor",
                        options = salesFloors.map { it.id to it.name },
                        selectedId = header.destinationSalesFloor,
                        onSelected = { viewModel.updateHeader { h -> h.copy(destinationSalesFloor = it) } },
                        error = uiState.fieldErrors["destinationSalesFloor"],
                    )
                    GlDropdownField(
                        label = "Purpose",
                        options = PermitFormChoices.purposes,
                        selectedId = header.purpose,
                        onSelected = { viewModel.updateHeader { h -> h.copy(purpose = it) } },
                    )
                    GlTextField(
                        value = header.comments,
                        onValueChange = { viewModel.updateHeader { h -> h.copy(comments = it) } },
                        label = "Comments",
                        singleLine = false,
                        minLines = 3,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlButton(text = "Cancel", onClick = onSubmitted, variant = GlButtonVariant.Outline, modifier = Modifier.weight(1f))
                        GlButton(text = "Next", onClick = { viewModel.goToEntriesStep() }, modifier = Modifier.weight(1f))
                    }
                }
                else -> ScrollableFormColumn {
                    GlSectionHeader(title = "Step 2 — Grower entries (${uiState.entries.size})")
                    GlBanner(title = "Add at least 2 growers before submitting.", tone = GlTone.Info, icon = "info")
                    uiState.entries.forEach { entry ->
                        GlCard(contentPadding = 12.dp) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.growerLabel.ifBlank { entry.growerId }, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("${entry.totalBales} bales · ${entry.totalWeightKg} kg", color = c.textMuted, fontSize = 12.sp)
                                }
                                GlButton(
                                    text = "Remove",
                                    onClick = { viewModel.removeEntry(entry.localKey) },
                                    variant = GlButtonVariant.DangerOutline,
                                    size = GlButtonSize.Sm,
                                    fillMaxWidth = false,
                                )
                            }
                        }
                    }
                    GlSectionHeader(title = "Add grower entry")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlTextField(
                            value = entryForm.growerSearch,
                            onValueChange = { entryForm = entryForm.copy(growerSearch = it) },
                            label = "TBZ ID / NRC lookup",
                            modifier = Modifier.weight(1f),
                            error = uiState.fieldErrors["growerSearch"],
                        )
                        GlButton(
                            text = "Look up",
                            onClick = {
                                viewModel.lookupGrowerByQuery(entryForm.growerSearch)?.let { (id, label) ->
                                    entryForm = entryForm.copy(growerId = id, growerLabel = label)
                                } ?: viewModel.markGrowerLookupFailed()
                            },
                            fillMaxWidth = false,
                            size = GlButtonSize.Md,
                        )
                    }
                    GlDropdownField(
                        label = "Grower",
                        options = growers.map { (it.remote_id ?: it.local_id) to "${it.first_name} ${it.last_name}" },
                        selectedId = entryForm.growerId,
                        onSelected = { id ->
                            val label = growers.firstOrNull {
                                (it.remote_id ?: it.local_id) == id
                            }?.let { "${it.first_name} ${it.last_name}" }.orEmpty()
                            entryForm = entryForm.copy(growerId = id, growerLabel = label)
                        },
                        error = uiState.fieldErrors["growerId"],
                    )
                    GlDropdownField(
                        label = "Grower category",
                        options = PermitFormChoices.growerCategories,
                        selectedId = entryForm.growerCategory,
                        onSelected = { entryForm = entryForm.copy(growerCategory = it) },
                    )
                    GlTextField(
                        value = entryForm.totalBales,
                        onValueChange = { entryForm = entryForm.copy(totalBales = it) },
                        label = "Total bales",
                        error = uiState.fieldErrors["totalBales"],
                    )
                    GlTextField(
                        value = entryForm.totalWeightKg,
                        onValueChange = { entryForm = entryForm.copy(totalWeightKg = it) },
                        label = "Total weight (kg)",
                        error = uiState.fieldErrors["totalWeightKg"],
                    )
                    GlTextField(
                        value = entryForm.notes,
                        onValueChange = { entryForm = entryForm.copy(notes = it) },
                        label = "Notes",
                        singleLine = false,
                        minLines = 3,
                    )
                    GlButton(
                        text = "Add entry",
                        onClick = {
                            if (viewModel.addEntryFromForm(entryForm)) {
                                entryForm = GroupPermitEntryForm()
                            }
                        },
                        variant = GlButtonVariant.Secondary,
                    )
                    uiState.saveError?.let { ErrorText(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlButton(
                            text = "Back",
                            onClick = { viewModel.goToHeaderStep() },
                            variant = GlButtonVariant.Outline,
                            modifier = Modifier.weight(1f),
                        )
                        GlButton(
                            text = if (uiState.isSaving) "Submitting…" else "Submit for approval",
                            onClick = { viewModel.submitGroupPermit(onSubmitted) },
                            enabled = !uiState.isSaving && uiState.entries.size >= 2,
                            modifier = Modifier.weight(1f),
                            variant = GlButtonVariant.Gold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupPermitValidateScreen(
    viewModel: GroupPermitViewModel = hiltViewModel(),
) {
    val state by viewModel.validateState.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Validate group permit")
            ScrollableFormColumn {
                GlTextField(
                    value = state.permitToken,
                    onValueChange = viewModel::updateValidateToken,
                    label = "Group permit token / QR code",
                    leadingIcon = "qr",
                )
                QrScanButton(
                    onScan = viewModel::updateValidateToken,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.verifyError?.let { ErrorText(it) }
                state.result?.let { result ->
                    if (result.valid) {
                        GlBanner(
                            title = "Valid group permit ${result.permitNumber.orEmpty()}",
                            subtitle = result.status.orEmpty(),
                            tone = GlTone.Success,
                            icon = "check-circle",
                        )
                        GlCard(contentPadding = 14.dp) {
                            Column {
                                GlFieldRow(label = "Plate", value = result.licensePlate.orEmpty())
                                GlFieldRow(label = "Destination", value = result.destination.orEmpty())
                                GlFieldRow(label = "Bales · Weight", value = "${result.totalBales ?: 0} bales · ${result.totalWeightKg ?: 0.0} kg")
                                result.validFrom?.let { GlFieldRow(label = "Valid from", value = it) }
                                result.validTo?.let { GlFieldRow(label = "Valid to", value = it) }
                            }
                        }
                        if (result.entries.isNotEmpty()) {
                            GlSectionHeader(title = "Entries")
                            GlCard(contentPadding = 4.dp) {
                                Column {
                                    result.entries.forEach { entry ->
                                        GlRow(
                                            title = entry.grower_name.orEmpty().ifBlank { "Grower" },
                                            subtitle = "${entry.total_bales} bales",
                                            trailing = { GlPill(text = entry.status, size = GlPillSize.Sm) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                GlButton(
                    text = if (state.isVerifying) "Validating…" else "Validate",
                    onClick = viewModel::validateGroupPermit,
                    enabled = !state.isVerifying,
                )
            }
        }
    }
}

@Composable
fun GroupPermitStatusListScreen(
    onOpenDetail: (String) -> Unit,
    initialStatus: String = "All",
    viewModel: GroupPermitViewModel = hiltViewModel(),
) {
    val permits by viewModel.filteredGroupPermits.collectAsState()
    val filter by viewModel.statusFilter.collectAsState()
    val c = glColors()

    LaunchedEffect(initialStatus) {
        if (initialStatus != "All") viewModel.onStatusFilterChange(initialStatus)
    }

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Group permit list")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlFilterPills(
                    options = listOf("All", "PENDING", "APPROVED", "RETURNED_FOR_CORRECTION", "DRAFT"),
                    selected = filter,
                    onSelect = viewModel::onStatusFilterChange,
                )
                GlButton(text = "Sync group permits", onClick = viewModel::syncAll, variant = GlButtonVariant.Outline, leadingIcon = "sync")
            }
            if (permits.isEmpty()) {
                GlEmptyState(title = "No group permits yet", subtitle = "Created group permits will appear here.", icon = "users")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(permits, key = { it.local_id }) { permit ->
                        GroupPermitRow(permit, onClick = { onOpenDetail(permit.local_id) })
                    }
                }
            }
        }
    }
}

@Composable
fun GroupPermitDetailScreen(
    localId: String,
    onOpenCorrection: (String) -> Unit,
    viewModel: GroupPermitViewModel = hiltViewModel(),
) {
    val permit by viewModel.observeGroupPermit(localId).collectAsState()
    val canApprove by viewModel.canApprovePermit.collectAsState()
    val reviewState by viewModel.reviewState.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Group permit detail")
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (permit == null) {
                    GlEmptyState(title = "Group permit not found", icon = "search")
                } else {
                    GroupPermitDetailContent(permit!!)
                    if (permit!!.status == "PENDING" && canApprove) {
                        PermitReviewPanel(
                            state = reviewState,
                            onFormChange = { updated -> viewModel.updateReviewForm { updated } },
                            onSubmit = {
                                val remoteId = permit!!.remote_id ?: permit!!.local_id
                                viewModel.submitGroupPermitReview(localId, remoteId)
                            },
                        )
                    }
                    if (permit!!.status == "RETURNED_FOR_CORRECTION") {
                        permit!!.correction_reason?.let {
                            GlBanner(title = "Correction reason", subtitle = it, tone = GlTone.Warning, icon = "warning")
                        }
                        GlButton(text = "Fix & resubmit", onClick = { onOpenCorrection(localId) }, variant = GlButtonVariant.Gold)
                    }
                    if (reviewState.submitSuccess) {
                        GlBanner(title = "Review queued for sync", tone = GlTone.Success, icon = "cloud-up")
                    }
                }
            }
        }
    }
}

@Composable
fun GroupPermitCorrectionScreen(
    localId: String,
    onDone: () -> Unit,
    viewModel: GroupPermitViewModel = hiltViewModel(),
) {
    val permit by viewModel.observeGroupPermit(localId).collectAsState()
    val uiState by viewModel.createState.collectAsState()
    val header = uiState.header
    val provinces by viewModel.provinces.collectAsState()
    val salesFloors by viewModel.salesFloors.collectAsState()
    val growers by viewModel.growers.collectAsState()
    val districts by viewModel.districtsForProvince(header.originProvince)
        .collectAsState(initial = emptyList())
    var entryForm by remember { mutableStateOf(GroupPermitEntryForm()) }
    val c = glColors()

    LaunchedEffect(permit?.local_id) {
        permit?.let { viewModel.loadCorrectionFromPermit(it) }
    }

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Group permit correction")
            if (permit == null) {
                GlEmptyState(title = "Group permit not found", icon = "search")
            } else {
                ScrollableFormColumn {
                    permit!!.correction_reason?.let {
                        GlBanner(title = "TBZ correction reason", subtitle = it, tone = GlTone.Warning, icon = "warning")
                    }
                    GlBanner(title = "Manifest must keep at least 2 growers before resubmit.", tone = GlTone.Info, icon = "info")
                    GlSectionHeader(title = "Header")
                    GlTextField(
                        value = header.licensePlate,
                        onValueChange = { viewModel.updateHeader { h -> h.copy(licensePlate = it) } },
                        label = "License plate",
                        error = uiState.fieldErrors["licensePlate"],
                    )
                    GlDropdownField(
                        label = "Origin province",
                        options = provinces.map { it.id to it.name },
                        selectedId = header.originProvince,
                        onSelected = {
                            viewModel.updateHeader { h -> h.copy(originProvince = it, originDistrict = "") }
                        },
                    )
                    GlDropdownField(
                        label = "Origin district",
                        options = districts.map { it.id to it.name },
                        selectedId = header.originDistrict,
                        onSelected = { viewModel.updateHeader { h -> h.copy(originDistrict = it) } },
                    )
                    GlDropdownField(
                        label = "Destination sales floor",
                        options = salesFloors.map { it.id to it.name },
                        selectedId = header.destinationSalesFloor,
                        onSelected = { viewModel.updateHeader { h -> h.copy(destinationSalesFloor = it) } },
                    )
                    GlDropdownField(
                        label = "Purpose",
                        options = PermitFormChoices.purposes,
                        selectedId = header.purpose,
                        onSelected = { viewModel.updateHeader { h -> h.copy(purpose = it) } },
                    )
                    GlTextField(
                        value = header.comments,
                        onValueChange = { viewModel.updateHeader { h -> h.copy(comments = it) } },
                        label = "Comments",
                        singleLine = false,
                        minLines = 3,
                    )
                    GlSectionHeader(title = "Grower manifest (${uiState.entries.size})")
                    uiState.entries.forEach { entry ->
                        GlCard(contentPadding = 12.dp) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        entry.growerLabel.ifBlank { "Existing entry" },
                                        color = c.text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text("${entry.totalBales} bales · ${entry.totalWeightKg} kg", color = c.textMuted, fontSize = 12.sp)
                                    if (entry.isExisting) {
                                        GlPill(text = "Synced entry", size = GlPillSize.Sm, tone = GlTone.Synced)
                                    }
                                }
                                if (!entry.isExisting) {
                                    GlButton(
                                        text = "Remove",
                                        onClick = { viewModel.removeEntry(entry.localKey) },
                                        variant = GlButtonVariant.DangerOutline,
                                        size = GlButtonSize.Sm,
                                        fillMaxWidth = false,
                                    )
                                }
                            }
                        }
                    }
                    GlSectionHeader(title = "Add grower entry")
                    GlDropdownField(
                        label = "Grower",
                        options = growers.map { (it.remote_id ?: it.local_id) to "${it.first_name} ${it.last_name}" },
                        selectedId = entryForm.growerId,
                        onSelected = { id ->
                            val label = growers.firstOrNull {
                                (it.remote_id ?: it.local_id) == id
                            }?.let { "${it.first_name} ${it.last_name}" }.orEmpty()
                            entryForm = entryForm.copy(growerId = id, growerLabel = label)
                        },
                    )
                    GlTextField(
                        value = entryForm.totalBales,
                        onValueChange = { entryForm = entryForm.copy(totalBales = it) },
                        label = "Total bales",
                        error = uiState.fieldErrors["totalBales"],
                    )
                    GlTextField(
                        value = entryForm.totalWeightKg,
                        onValueChange = { entryForm = entryForm.copy(totalWeightKg = it) },
                        label = "Total weight (kg)",
                        error = uiState.fieldErrors["totalWeightKg"],
                    )
                    GlButton(
                        text = "Add entry",
                        onClick = {
                            if (viewModel.addEntryFromForm(entryForm)) {
                                entryForm = GroupPermitEntryForm()
                            }
                        },
                        variant = GlButtonVariant.Secondary,
                    )
                    uiState.saveError?.let { ErrorText(it) }
                    val remoteId = permit!!.remote_id ?: permit!!.local_id
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlButton(
                            text = if (uiState.isSaving) "Saving…" else "Save corrections",
                            onClick = { viewModel.saveGroupPermitCorrection(localId, remoteId) {} },
                            enabled = !uiState.isSaving && uiState.entries.size >= 2,
                            modifier = Modifier.weight(1f),
                        )
                        GlButton(
                            text = "Resubmit for review",
                            onClick = {
                                viewModel.resubmitGroupCorrections(localId, remoteId)
                                onDone()
                            },
                            variant = GlButtonVariant.Gold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    GlButton(text = "Back", onClick = onDone, variant = GlButtonVariant.Outline, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun GroupPermitRow(permit: GroupPermitEntity, onClick: () -> Unit) {
    val c = glColors()
    val tone = when (permit.status) {
        "APPROVED" -> GlTone.Primary
        "PENDING" -> GlTone.Gold
        "REJECTED" -> GlTone.Danger
        else -> GlTone.Default
    }
    GlCard(onClick = onClick, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    permit.permit_number ?: "Draft / pending #",
                    color = c.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                GlPill(text = permit.status, size = GlPillSize.Sm, tone = tone)
            }
            GlFieldRow(label = "Plate", value = "${permit.license_plate} · ${permit.entry_count} growers")
            GlFieldRow(label = "Bales · Weight", value = "${permit.total_bales} bales · ${permit.total_weight_kg} kg")
            GlSyncChip(status = permit.sync_status)
        }
    }
}

@Composable
private fun GroupPermitDetailContent(permit: GroupPermitEntity) {
    val c = glColors()
    val tone = when (permit.status) {
        "APPROVED" -> GlTone.Primary
        "PENDING" -> GlTone.Gold
        "REJECTED" -> GlTone.Danger
        else -> GlTone.Default
    }
    GlCard(contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    permit.permit_number ?: "Group permit",
                    color = c.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                GlPill(text = permit.status, tone = tone)
            }
        }
    }
    val token = permit.qr_token
    if (!token.isNullOrBlank()) {
        GlCard(contentPadding = 16.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(184.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    QrCodeImage(data = token, modifier = Modifier.size(164.dp))
                }
                Text(
                    "Show this QR at the sales floor to validate",
                    color = c.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
    GlCard(contentPadding = 14.dp) {
        Column {
            GlFieldRow(label = "Plate", value = permit.license_plate)
            GlFieldRow(label = "From", value = "${permit.origin_province} / ${permit.origin_district}")
            GlFieldRow(label = "To", value = permit.destination_sales_floor)
            GlFieldRow(label = "Purpose", value = permit.purpose)
            GlFieldRow(label = "Bales · Weight", value = "${permit.total_bales} bales · ${permit.total_weight_kg} kg")
            GlFieldRow(label = "Growers", value = permit.entry_count.toString())
            permit.valid_from?.let { GlFieldRow(label = "Valid from", value = it) }
            permit.valid_to?.let { GlFieldRow(label = "Valid to", value = it) }
        }
    }
    permit.rejection_reason?.let {
        GlBanner(title = "Rejection reason", subtitle = it, tone = GlTone.Danger, icon = "x")
    }
    GlSyncChip(status = permit.sync_status)
}
