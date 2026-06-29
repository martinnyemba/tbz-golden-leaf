package zm.co.tbz.goldenleaf.ui.permits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.PermitRequestEntity
import zm.co.tbz.goldenleaf.data.local.entity.TransportPermitEntity
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
import zm.co.tbz.goldenleaf.ui.components.GlSearchBar
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlStepper
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.QrScanButton
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn

@Composable
fun PermitsHubScreen(
    onRequest: () -> Unit,
    onValidate: () -> Unit,
    onList: () -> Unit,
    onGroupPermits: () -> Unit,
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val stats by viewModel.hubStats.collectAsState()
    val requests by viewModel.permitRequests.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Permits", subtitle = "Transport & group permits")
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlKpiTile("Transport", stats.transportPermits.toString(), Modifier.weight(1f), tone = GlTone.Primary, icon = "permit")
                    GlKpiTile("Pending", stats.pendingRequests.toString(), Modifier.weight(1f), tone = GlTone.Gold, icon = "clock")
                    GlKpiTile("Group", stats.groupPermits.toString(), Modifier.weight(1f), icon = "users")
                }
                GlSectionHeader(title = "Quick actions")
                GlCard(contentPadding = 4.dp) {
                    Column {
                        GlRow("Request permit", subtitle = "3-step grower, movement, buyer", leadingIcon = "permit", onClick = onRequest)
                        GlRow("Validate permit", subtitle = "Scan / verify QR token", leadingIcon = "qr", onClick = onValidate)
                        GlRow("Permit list", subtitle = "Cached transport permits", leadingIcon = "clipboard", onClick = onList)
                        GlRow("Group permits", subtitle = "Multi-grower transport permits", leadingIcon = "users", onClick = onGroupPermits)
                    }
                }
                if (requests.isNotEmpty()) {
                    GlSectionHeader(title = "Recent requests")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        requests.take(3).forEach { PermitRequestRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun PermitRequestScreen(
    onSubmitted: () -> Unit,
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.requestState.collectAsState()
    val form = uiState.form
    val provinces by viewModel.provinces.collectAsState()
    val salesFloors by viewModel.salesFloors.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val growers by viewModel.growers.collectAsState()
    val districts by viewModel.districtsForProvince(form.originProvince).collectAsState(initial = emptyList())
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(
                title = when (uiState.step) {
                    1 -> "Permit — Grower"
                    2 -> "Permit — Movement"
                    else -> "Permit — Buyer"
                },
            )
            GlStepper(step = uiState.step, total = 3)
            when (uiState.step) {
                1 -> GrowerStep(
                    form = form,
                    growers = growers.map { (it.remote_id ?: it.local_id) to "${it.first_name} ${it.last_name}" },
                    errors = uiState.fieldErrors,
                    onFormChange = { viewModel.updateForm { it } },
                    onLookup = viewModel::lookupGrowerByTbzId,
                    onNext = { viewModel.goToMovementStep() },
                    onCancel = onSubmitted,
                )
                2 -> MovementStep(
                    form = form,
                    provinces = provinces.map { it.id to it.name },
                    districts = districts.map { it.id to it.name },
                    salesFloors = salesFloors.map { it.id to it.name },
                    errors = uiState.fieldErrors,
                    onFormChange = { viewModel.updateForm { it } },
                    onBack = { viewModel.goToGrowerStep() },
                    onNext = { viewModel.goToBuyerStep() },
                )
                else -> BuyerStep(
                    form = form,
                    buyers = buyers.map { it.id to it.name },
                    errors = uiState.fieldErrors,
                    isSaving = uiState.isSaving,
                    saveError = uiState.saveError,
                    onFormChange = { viewModel.updateForm { it } },
                    onBack = { viewModel.goToMovementStepBack() },
                    onSubmit = { viewModel.submitPermitRequest { onSubmitted() } },
                )
            }
        }
    }
}

@Composable
fun PermitValidateScreen(
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val state by viewModel.validateState.collectAsState()
    val salesFloors by viewModel.salesFloors.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Validate permit")
            ScrollableFormColumn {
                GlTextField(
                    value = state.permitToken,
                    onValueChange = viewModel::updateValidateToken,
                    label = "Permit token / QR code",
                    leadingIcon = "qr",
                )
                QrScanButton(
                    onScan = viewModel::updateValidateToken,
                    modifier = Modifier.fillMaxWidth(),
                )
                GlDropdownField(
                    label = "Sales floor (optional)",
                    options = listOf("" to "Any") + salesFloors.map { it.id to it.name },
                    selectedId = state.salesfloorId,
                    onSelected = viewModel::updateValidateSalesfloor,
                )
                state.verifyError?.let { ErrorText(it) }
                state.result?.let { result ->
                    if (result.valid) {
                        GlBanner(
                            title = "Valid permit ${result.permitNumber.orEmpty()}",
                            subtitle = "${result.growerName.orEmpty()} (${result.status.orEmpty()})",
                            tone = GlTone.Success,
                            icon = "check-circle",
                        )
                        GlCard(contentPadding = 14.dp) {
                            Column {
                                GlFieldRow(label = "TBZ ID", value = result.tbzId.orEmpty().ifBlank { "—" }, mono = true)
                                GlFieldRow(label = "Total bales", value = "${result.totalBales ?: 0}")
                                GlFieldRow(label = "Remaining", value = "${result.remainingBales ?: 0}")
                            }
                        }
                    }
                }
                GlButton(
                    text = if (state.isVerifying) "Verifying…" else "Verify",
                    onClick = viewModel::verifyPermit,
                    enabled = !state.isVerifying,
                )
            }
        }
    }
}

@Composable
fun PermitListScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val permits by viewModel.filteredPermits.collectAsState()
    val requests by viewModel.permitRequests.collectAsState()
    val uiState by viewModel.requestState.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Permit list")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlSearchBar(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    placeholder = "Search permit #, grower, plate",
                )
                GlFilterPills(
                    options = PermitFormChoices.statusFilters.take(4),
                    selected = uiState.statusFilter,
                    onSelect = viewModel::onStatusFilterChange,
                )
                GlButton(text = "Sync pending requests", onClick = viewModel::syncAllPending, variant = GlButtonVariant.Outline, leadingIcon = "sync")
            }
            if (permits.isEmpty() && requests.isEmpty()) {
                GlEmptyState(title = "No permits yet", subtitle = "Requested and cached permits will appear here.", icon = "permit")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (requests.isNotEmpty()) {
                        item { GlSectionHeader(title = "Local requests") }
                        items(requests, key = { "req-${it.local_id}" }) { req ->
                            PermitRequestRow(req, onClick = { onOpenDetail(req.local_id) })
                        }
                    }
                    item { GlSectionHeader(title = "Transport permits") }
                    items(permits, key = { it.local_id }) { permit ->
                        TransportPermitRow(permit, onClick = { onOpenDetail(permit.local_id) })
                    }
                }
            }
        }
    }
}

@Composable
fun PermitDetailScreen(
    localId: String,
    onOpenCorrection: (String) -> Unit = {},
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val transport by viewModel.observeTransportPermit(localId).collectAsState()
    val request by viewModel.observePermitRequest(localId).collectAsState()
    val canApprove by viewModel.canApprovePermit.collectAsState()
    val reviewState by viewModel.reviewState.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Permit detail")
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    transport != null -> {
                        TransportPermitDetail(transport!!)
                        if (transport!!.status == "PENDING" && canApprove) {
                            PermitReviewPanel(
                                state = reviewState,
                                onFormChange = { viewModel.updateReviewForm { it } },
                                onSubmit = {
                                    val remoteId = transport!!.remote_id ?: transport!!.local_id
                                    viewModel.submitTransportPermitReview(localId, remoteId)
                                },
                            )
                        }
                        if (transport!!.status == "RETURNED_FOR_CORRECTION") {
                            transport!!.correction_reason?.let {
                                GlBanner(title = "Correction reason", subtitle = it, tone = GlTone.Warning, icon = "warning")
                            }
                            GlButton(text = "Fix & resubmit", onClick = { onOpenCorrection(localId) }, variant = GlButtonVariant.Gold)
                        }
                        if (reviewState.submitSuccess) {
                            GlBanner(title = "Review queued for sync", tone = GlTone.Success, icon = "cloud-up")
                        }
                    }
                    request != null -> PermitRequestDetail(request!!, onDelete = { viewModel.deletePermitRequest(it) })
                    else -> GlEmptyState(title = "Permit not found", icon = "search")
                }
            }
        }
    }
}

@Composable
private fun GrowerStep(
    form: PermitRequestForm,
    growers: List<Pair<String, String>>,
    errors: Map<String, String>,
    onFormChange: (PermitRequestForm) -> Unit,
    onLookup: () -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
) {
    ScrollableFormColumn {
        GlSectionHeader(title = "Step 1 — Grower")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlTextField(
                value = form.growerSearch,
                onValueChange = { onFormChange(form.copy(growerSearch = it)) },
                label = "TBZ ID / NRC lookup",
                modifier = Modifier.weight(1f),
                error = errors["growerSearch"],
            )
            GlButton(text = "Look up", onClick = onLookup, fillMaxWidth = false, size = GlButtonSize.Md)
        }
        GlDropdownField(
            label = "Grower",
            options = growers,
            selectedId = form.growerId,
            onSelected = { onFormChange(form.copy(growerId = it)) },
            error = errors["growerId"],
        )
        GlDropdownField(
            label = "Grower category",
            options = PermitFormChoices.growerCategories,
            selectedId = form.growerCategory,
            onSelected = { onFormChange(form.copy(growerCategory = it)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlButton(text = "Cancel", onClick = onCancel, variant = GlButtonVariant.Outline, modifier = Modifier.weight(1f))
            GlButton(text = "Next", onClick = onNext, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MovementStep(
    form: PermitRequestForm,
    provinces: List<Pair<String, String>>,
    districts: List<Pair<String, String>>,
    salesFloors: List<Pair<String, String>>,
    errors: Map<String, String>,
    onFormChange: (PermitRequestForm) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    ScrollableFormColumn {
        GlSectionHeader(title = "Step 2 — Movement")
        GlTextField(
            value = form.totalBales,
            onValueChange = { onFormChange(form.copy(totalBales = it)) },
            label = "Total bales",
            error = errors["totalBales"],
        )
        GlTextField(
            value = form.totalWeightKg,
            onValueChange = { onFormChange(form.copy(totalWeightKg = it)) },
            label = "Total weight (kg)",
            error = errors["totalWeightKg"],
        )
        GlTextField(
            value = form.licensePlate,
            onValueChange = { onFormChange(form.copy(licensePlate = it)) },
            label = "License plate",
            error = errors["licensePlate"],
        )
        GlDropdownField(
            label = "Origin province",
            options = provinces,
            selectedId = form.originProvince,
            onSelected = { onFormChange(form.copy(originProvince = it, originDistrict = "")) },
            error = errors["originProvince"],
        )
        GlDropdownField(
            label = "Origin district",
            options = districts,
            selectedId = form.originDistrict,
            onSelected = { onFormChange(form.copy(originDistrict = it)) },
            error = errors["originDistrict"],
        )
        GlDropdownField(
            label = "Destination sales floor",
            options = salesFloors,
            selectedId = form.destinationSalesFloor,
            onSelected = { onFormChange(form.copy(destinationSalesFloor = it)) },
            error = errors["destinationSalesFloor"],
        )
        GlDropdownField(
            label = "Purpose",
            options = PermitFormChoices.purposes,
            selectedId = form.purpose,
            onSelected = { onFormChange(form.copy(purpose = it)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlButton(text = "Back", onClick = onBack, variant = GlButtonVariant.Outline, modifier = Modifier.weight(1f))
            GlButton(text = "Next", onClick = onNext, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BuyerStep(
    form: PermitRequestForm,
    buyers: List<Pair<String, String>>,
    errors: Map<String, String>,
    isSaving: Boolean,
    saveError: String?,
    onFormChange: (PermitRequestForm) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    ScrollableFormColumn {
        GlSectionHeader(title = "Step 3 — Buyer & notes")
        GlDropdownField(
            label = "Buyer",
            options = listOf("" to "None") + buyers,
            selectedId = form.buyerId,
            onSelected = { onFormChange(form.copy(buyerId = it)) },
            error = errors["buyerId"],
        )
        GlCard(contentPadding = 12.dp) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = form.isBought, onCheckedChange = { onFormChange(form.copy(isBought = it)) })
                    Text("Tobacco already bought", fontSize = 14.sp, color = glColors().text)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = form.buyerAccepted, onCheckedChange = { onFormChange(form.copy(buyerAccepted = it)) })
                    Text("Buyer accepted / confirmed", fontSize = 14.sp, color = glColors().text)
                }
            }
        }
        GlTextField(
            value = form.comments,
            onValueChange = { onFormChange(form.copy(comments = it)) },
            label = "Comments",
            singleLine = false,
            minLines = 3,
        )
        saveError?.let { ErrorText(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlButton(text = "Back", onClick = onBack, variant = GlButtonVariant.Outline, modifier = Modifier.weight(1f), enabled = !isSaving)
            GlButton(
                text = if (isSaving) "Submitting…" else "Submit request",
                onClick = onSubmit,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                variant = GlButtonVariant.Gold,
            )
        }
    }
}

@Composable
private fun TransportPermitRow(permit: TransportPermitEntity, onClick: () -> Unit) {
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
                    permit.permit_number ?: "Pending #",
                    color = glColors().text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                GlPill(text = permit.status, size = GlPillSize.Sm, tone = tone)
            }
            permit.grower_name?.let { GlFieldRow(label = "Grower", value = it) }
            GlFieldRow(label = "Bales · Weight", value = "${permit.total_bales} bales · ${permit.total_weight_kg} kg")
            GlFieldRow(label = "Plate", value = "${permit.license_plate} · ${permit.purpose}")
            GlSyncChip(status = permit.sync_status)
        }
    }
}

@Composable
private fun PermitRequestRow(
    request: PermitRequestEntity,
    onClick: (() -> Unit)? = null,
) {
    GlCard(onClick = onClick, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Local request ${request.local_id.take(8)}…",
                color = glColors().text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            GlFieldRow(label = "Created", value = request.created_at.toString())
            GlSyncChip(status = request.sync_status)
        }
    }
}

@Composable
private fun TransportPermitDetail(permit: TransportPermitEntity) {
    val c = glColors()
    val tone = when (permit.status) {
        "APPROVED" -> GlTone.Primary
        "PENDING" -> GlTone.Gold
        "REJECTED" -> GlTone.Danger
        else -> GlTone.Default
    }
    GlCard(accent = GlAccentFor(permit.status), contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    permit.permit_number ?: "Transport permit",
                    color = c.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                GlPill(text = permit.status, tone = tone)
            }
            permit.grower_name?.let { GlFieldRow(label = "Grower", value = it) }
        }
    }
    GlCard(contentPadding = 14.dp) {
        Column {
            GlFieldRow(label = "Bales · Weight", value = "${permit.total_bales} bales · ${permit.total_weight_kg} kg")
            GlFieldRow(label = "Plate", value = permit.license_plate)
            GlFieldRow(label = "From", value = "${permit.origin_province} / ${permit.origin_district}")
            GlFieldRow(label = "To", value = permit.destination_sales_floor)
            GlFieldRow(label = "Purpose", value = permit.purpose)
            permit.valid_from?.let { GlFieldRow(label = "Valid from", value = it) }
            permit.valid_to?.let { GlFieldRow(label = "Valid to", value = it) }
            permit.comments?.let { GlFieldRow(label = "Comments", value = it) }
        }
    }
    permit.rejection_reason?.let {
        GlBanner(title = "Rejection reason", subtitle = it, tone = GlTone.Danger, icon = "x")
    }
    GlSyncChip(status = permit.sync_status)
    permit.last_sync_error?.let { ErrorText("Sync error: $it") }
}

private fun GlAccentFor(status: String) = when (status) {
    "PENDING" -> zm.co.tbz.goldenleaf.ui.components.GlAccent.Gold
    "APPROVED" -> zm.co.tbz.goldenleaf.ui.components.GlAccent.Primary
    else -> zm.co.tbz.goldenleaf.ui.components.GlAccent.None
}

@Composable
private fun PermitRequestDetail(
    request: PermitRequestEntity,
    onDelete: (PermitRequestEntity) -> Unit,
) {
    val c = glColors()
    GlCard(contentPadding = 14.dp) {
        Column {
            Text("Local permit request", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            GlFieldRow(label = "Local ID", value = request.local_id, mono = true)
            GlFieldRow(label = "Created", value = request.created_at.toString())
            GlSyncChip(status = request.sync_status)
        }
    }
    GlCard(contentPadding = 14.dp) {
        Column {
            GlSectionHeader(title = "Payload preview")
            Text(request.data_json, color = c.textMuted, fontSize = 12.sp)
        }
    }
    GlButton(text = "Delete local request", onClick = { onDelete(request) }, variant = GlButtonVariant.DangerOutline)
}
