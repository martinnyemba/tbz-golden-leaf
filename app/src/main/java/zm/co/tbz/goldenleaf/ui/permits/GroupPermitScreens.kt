package zm.co.tbz.goldenleaf.ui.permits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitEntity
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.InfoBanner
import zm.co.tbz.goldenleaf.ui.components.ModuleHubCard
import zm.co.tbz.goldenleaf.ui.components.QrScanButton
import zm.co.tbz.goldenleaf.ui.components.SyncStatusChip
import zm.co.tbz.goldenleaf.ui.components.TbzStatCard
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar
import zm.co.tbz.goldenleaf.ui.registration.FormActionRow
import zm.co.tbz.goldenleaf.ui.registration.FormSectionTitle
import zm.co.tbz.goldenleaf.ui.registration.FormTextField
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn
import zm.co.tbz.goldenleaf.ui.registration.TbzDropdownField

@Composable
fun GroupPermitHubScreen(
    onCreate: () -> Unit,
    onValidate: () -> Unit,
    onStatusList: () -> Unit,
    viewModel: GroupPermitViewModel = hiltViewModel(),
) {
    val stats by viewModel.hubStats.collectAsState()
    val permits by viewModel.filteredGroupPermits.collectAsState()

    Scaffold(topBar = { TbzTopBar("Group permits") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TbzStatCard("Total", stats.total.toString(), Modifier.weight(1f))
                TbzStatCard("Pending", stats.pending.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TbzStatCard("Approved", stats.approved.toString(), Modifier.weight(1f))
                TbzStatCard("Returned", stats.returned.toString(), Modifier.weight(1f))
            }
            ModuleHubCard("New group permit", "Header + grower entries (min 2)", onCreate)
            ModuleHubCard("Validate group permit", "Scan / verify group QR token", onValidate)
            ModuleHubCard("Status list", "Filter by permit status", onStatusList)
            if (permits.isNotEmpty()) {
                Text("Recent group permits", fontWeight = FontWeight.SemiBold)
                permits.take(3).forEach { permit ->
                    GroupPermitRow(permit, onClick = {})
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

    Scaffold(topBar = {
        TbzTopBar(if (uiState.step == 1) "Group permit — Header" else "Group permit — Entries")
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { uiState.step / 2f },
                modifier = Modifier.fillMaxWidth(),
            )
            when (uiState.step) {
                1 -> ScrollableFormColumn {
                    FormSectionTitle("Step 1 — Header")
                    FormTextField(
                        header.licensePlate,
                        { viewModel.updateHeader { h -> h.copy(licensePlate = it) } },
                        "License plate",
                        error = uiState.fieldErrors["licensePlate"],
                    )
                    TbzDropdownField(
                        label = "Origin province",
                        options = provinces.map { it.id to it.name },
                        selectedId = header.originProvince,
                        onSelected = {
                            viewModel.updateHeader { h -> h.copy(originProvince = it, originDistrict = "") }
                        },
                    )
                    errorsOr(uiState.fieldErrors["originProvince"])
                    TbzDropdownField(
                        label = "Origin district",
                        options = districts.map { it.id to it.name },
                        selectedId = header.originDistrict,
                        onSelected = { viewModel.updateHeader { h -> h.copy(originDistrict = it) } },
                    )
                    errorsOr(uiState.fieldErrors["originDistrict"])
                    TbzDropdownField(
                        label = "Destination sales floor",
                        options = salesFloors.map { it.id to it.name },
                        selectedId = header.destinationSalesFloor,
                        onSelected = { viewModel.updateHeader { h -> h.copy(destinationSalesFloor = it) } },
                    )
                    errorsOr(uiState.fieldErrors["destinationSalesFloor"])
                    TbzDropdownField(
                        label = "Purpose",
                        options = PermitFormChoices.purposes,
                        selectedId = header.purpose,
                        onSelected = { viewModel.updateHeader { h -> h.copy(purpose = it) } },
                    )
                    FormTextField(
                        header.comments,
                        { viewModel.updateHeader { h -> h.copy(comments = it) } },
                        "Comments",
                        singleLine = false,
                    )
                    FormActionRow(
                        primaryLabel = "Next",
                        onPrimary = { viewModel.goToEntriesStep() },
                        secondaryLabel = "Cancel",
                        onSecondary = onSubmitted,
                    )
                }
                else -> ScrollableFormColumn {
                    FormSectionTitle("Step 2 — Grower entries (${uiState.entries.size})")
                    InfoBanner("Add at least 2 growers before submitting.")
                    uiState.entries.forEach { entry ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.growerLabel.ifBlank { entry.growerId }, fontWeight = FontWeight.SemiBold)
                                    Text("${entry.totalBales} bales · ${entry.totalWeightKg} kg")
                                }
                                OutlinedButton(onClick = { viewModel.removeEntry(entry.localKey) }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                    FormSectionTitle("Add grower entry")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FormTextField(
                            entryForm.growerSearch,
                            { entryForm = entryForm.copy(growerSearch = it) },
                            "TBZ ID / NRC lookup",
                            modifier = Modifier.weight(1f),
                            error = uiState.fieldErrors["growerSearch"],
                        )
                        Button(onClick = {
                            viewModel.lookupGrowerByQuery(entryForm.growerSearch)?.let { (id, label) ->
                                entryForm = entryForm.copy(growerId = id, growerLabel = label)
                            } ?: viewModel.markGrowerLookupFailed()
                        }) { Text("Look up") }
                    }
                    TbzDropdownField(
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
                    errorsOr(uiState.fieldErrors["growerId"])
                    TbzDropdownField(
                        label = "Grower category",
                        options = PermitFormChoices.growerCategories,
                        selectedId = entryForm.growerCategory,
                        onSelected = { entryForm = entryForm.copy(growerCategory = it) },
                    )
                    FormTextField(
                        entryForm.totalBales,
                        { entryForm = entryForm.copy(totalBales = it) },
                        "Total bales",
                        error = uiState.fieldErrors["totalBales"],
                    )
                    FormTextField(
                        entryForm.totalWeightKg,
                        { entryForm = entryForm.copy(totalWeightKg = it) },
                        "Total weight (kg)",
                        error = uiState.fieldErrors["totalWeightKg"],
                    )
                    FormTextField(
                        entryForm.notes,
                        { entryForm = entryForm.copy(notes = it) },
                        "Notes",
                        singleLine = false,
                    )
                    Button(
                        onClick = {
                            if (viewModel.addEntryFromForm(entryForm)) {
                                entryForm = GroupPermitEntryForm()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add entry") }
                    uiState.saveError?.let { ErrorText(it) }
                    FormActionRow(
                        primaryLabel = if (uiState.isSaving) "Submitting…" else "Submit for approval",
                        onPrimary = { viewModel.submitGroupPermit(onSubmitted) },
                        secondaryLabel = "Back",
                        onSecondary = { viewModel.goToHeaderStep() },
                        primaryEnabled = !uiState.isSaving && uiState.entries.size >= 2,
                    )
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

    Scaffold(topBar = { TbzTopBar("Validate group permit") }) { padding ->
        ScrollableFormColumn(Modifier.padding(padding)) {
            FormTextField(
                state.permitToken,
                viewModel::updateValidateToken,
                "Group permit token / QR code",
            )
            QrScanButton(
                onScan = viewModel::updateValidateToken,
                modifier = Modifier.fillMaxWidth(),
            )
            state.verifyError?.let { ErrorText(it) }
            state.result?.let { result ->
                if (result.valid) {
                    InfoBanner(
                        "Valid group permit ${result.permitNumber.orEmpty()} — ${result.status.orEmpty()}",
                    )
                    Text("Plate: ${result.licensePlate.orEmpty()}")
                    Text("Destination: ${result.destination.orEmpty()}")
                    Text("Bales: ${result.totalBales ?: 0} · Weight: ${result.totalWeightKg ?: 0.0} kg")
                    result.validFrom?.let { Text("Valid from: $it") }
                    result.validTo?.let { Text("Valid to: $it") }
                    if (result.entries.isNotEmpty()) {
                        Text("Entries", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        result.entries.forEach { entry ->
                            Text(
                                "${entry.grower_name.orEmpty()} — ${entry.total_bales} bales (${entry.status})",
                            )
                        }
                    }
                }
            }
            Button(
                onClick = viewModel::validateGroupPermit,
                enabled = !state.isVerifying,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isVerifying) "Validating…" else "Validate")
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

    androidx.compose.runtime.LaunchedEffect(initialStatus) {
        if (initialStatus != "All") viewModel.onStatusFilterChange(initialStatus)
    }

    Scaffold(topBar = { TbzTopBar("Group permit list") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("All", "PENDING", "APPROVED", "RETURNED_FOR_CORRECTION", "DRAFT").forEach { status ->
                    FilterChip(
                        selected = filter == status,
                        onClick = { viewModel.onStatusFilterChange(status) },
                        label = { Text(status.replace('_', ' ')) },
                    )
                }
            }
            Button(
                onClick = viewModel::syncAll,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) { Text("Sync group permits") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(permits, key = { it.local_id }) { permit ->
                    GroupPermitRow(permit, onClick = { onOpenDetail(permit.local_id) })
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

    Scaffold(topBar = { TbzTopBar("Group permit detail") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (permit == null) {
                Text("Group permit not found")
            } else {
                GroupPermitDetailContent(permit!!)
                if (permit!!.status == "PENDING" && canApprove) {
                    PermitReviewPanel(
                        state = reviewState,
                        onFormChange = { viewModel.updateReviewForm { it } },
                        onSubmit = {
                            val remoteId = permit!!.remote_id ?: permit!!.local_id
                            viewModel.submitGroupPermitReview(localId, remoteId)
                        },
                    )
                }
                if (permit!!.status == "RETURNED_FOR_CORRECTION") {
                    permit!!.correction_reason?.let { InfoBanner("Correction reason: $it") }
                    Button(
                        onClick = { onOpenCorrection(localId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Fix & resubmit") }
                }
                if (reviewState.submitSuccess) {
                    InfoBanner("Review queued for sync")
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

    LaunchedEffect(permit?.local_id) {
        permit?.let { viewModel.loadCorrectionFromPermit(it) }
    }

    Scaffold(topBar = { TbzTopBar("Group permit correction") }) { padding ->
        if (permit == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Group permit not found")
            }
        } else {
            ScrollableFormColumn(Modifier.padding(padding)) {
                permit!!.correction_reason?.let { InfoBanner("TBZ correction reason: $it") }
                InfoBanner("Manifest must keep at least 2 growers before resubmit.")
                FormSectionTitle("Header")
                FormTextField(
                    header.licensePlate,
                    { viewModel.updateHeader { h -> h.copy(licensePlate = it) } },
                    "License plate",
                    error = uiState.fieldErrors["licensePlate"],
                )
                TbzDropdownField(
                    label = "Origin province",
                    options = provinces.map { it.id to it.name },
                    selectedId = header.originProvince,
                    onSelected = {
                        viewModel.updateHeader { h -> h.copy(originProvince = it, originDistrict = "") }
                    },
                )
                TbzDropdownField(
                    label = "Origin district",
                    options = districts.map { it.id to it.name },
                    selectedId = header.originDistrict,
                    onSelected = { viewModel.updateHeader { h -> h.copy(originDistrict = it) } },
                )
                TbzDropdownField(
                    label = "Destination sales floor",
                    options = salesFloors.map { it.id to it.name },
                    selectedId = header.destinationSalesFloor,
                    onSelected = { viewModel.updateHeader { h -> h.copy(destinationSalesFloor = it) } },
                )
                TbzDropdownField(
                    label = "Purpose",
                    options = PermitFormChoices.purposes,
                    selectedId = header.purpose,
                    onSelected = { viewModel.updateHeader { h -> h.copy(purpose = it) } },
                )
                FormTextField(
                    header.comments,
                    { viewModel.updateHeader { h -> h.copy(comments = it) } },
                    "Comments",
                    singleLine = false,
                )
                FormSectionTitle("Grower manifest (${uiState.entries.size})")
                uiState.entries.forEach { entry ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.growerLabel.ifBlank { "Existing entry" },
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text("${entry.totalBales} bales · ${entry.totalWeightKg} kg")
                                if (entry.isExisting) Text("Synced entry")
                            }
                            if (!entry.isExisting) {
                                OutlinedButton(onClick = { viewModel.removeEntry(entry.localKey) }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
                FormSectionTitle("Add grower entry")
                TbzDropdownField(
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
                FormTextField(
                    entryForm.totalBales,
                    { entryForm = entryForm.copy(totalBales = it) },
                    "Total bales",
                    error = uiState.fieldErrors["totalBales"],
                )
                FormTextField(
                    entryForm.totalWeightKg,
                    { entryForm = entryForm.copy(totalWeightKg = it) },
                    "Total weight (kg)",
                    error = uiState.fieldErrors["totalWeightKg"],
                )
                Button(
                    onClick = {
                        if (viewModel.addEntryFromForm(entryForm)) {
                            entryForm = GroupPermitEntryForm()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add entry") }
                uiState.saveError?.let { ErrorText(it) }
                val remoteId = permit!!.remote_id ?: permit!!.local_id
                FormActionRow(
                    primaryLabel = if (uiState.isSaving) "Saving…" else "Save corrections",
                    onPrimary = { viewModel.saveGroupPermitCorrection(localId, remoteId) {} },
                    secondaryLabel = "Resubmit for review",
                    onSecondary = {
                        viewModel.resubmitGroupCorrections(localId, remoteId)
                        onDone()
                    },
                    primaryEnabled = !uiState.isSaving && uiState.entries.size >= 2,
                )
                OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
private fun GroupPermitRow(permit: GroupPermitEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(permit.permit_number ?: "Draft / pending #", fontWeight = FontWeight.SemiBold)
            Text("Plate: ${permit.license_plate} · ${permit.entry_count} growers")
            Text("${permit.total_bales} bales · ${permit.total_weight_kg} kg")
            Text("Status: ${permit.status}")
            SyncStatusChip(permit.sync_status)
        }
    }
}

@Composable
private fun GroupPermitDetailContent(permit: GroupPermitEntity) {
    Text(permit.permit_number ?: "Group permit", fontWeight = FontWeight.Bold)
    Text("Plate: ${permit.license_plate}")
    Text("From: ${permit.origin_province} / ${permit.origin_district}")
    Text("To: ${permit.destination_sales_floor}")
    Text("Purpose: ${permit.purpose}")
    Text("Bales: ${permit.total_bales} · Weight: ${permit.total_weight_kg} kg")
    Text("Growers: ${permit.entry_count}")
    Text("Status: ${permit.status}")
    permit.valid_from?.let { Text("Valid from: $it") }
    permit.valid_to?.let { Text("Valid to: $it") }
    permit.correction_reason?.let { Text("Correction: $it") }
    permit.rejection_reason?.let { Text("Rejection: $it") }
    SyncStatusChip(permit.sync_status)
}

@Composable
private fun errorsOr(message: String?) {
    message?.let { ErrorText(it) }
}
