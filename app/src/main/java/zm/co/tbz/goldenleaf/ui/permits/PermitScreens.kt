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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import zm.co.tbz.goldenleaf.data.local.entity.PermitRequestEntity
import zm.co.tbz.goldenleaf.data.local.entity.TransportPermitEntity
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonSize
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDropdownField
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFab
import zm.co.tbz.goldenleaf.ui.components.GlFieldRow
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlIcon
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
import zm.co.tbz.goldenleaf.ui.components.glVerticalScroll
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

    GlScaffold(containerColor = c.bg) { padding ->
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

    GlScaffold(containerColor = c.bg) { padding ->
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

    GlScaffold(containerColor = c.bg) { padding ->
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
                val result = state.result
                if (result != null) {
                    PermitValidationResult(result)
                } else {
                    state.verifyError?.let { ErrorText(it) }
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
    onValidate: () -> Unit = {},
    onNew: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val permits by viewModel.filteredPermits.collectAsState()
    val allPermits by viewModel.transportPermits.collectAsState()
    val requests by viewModel.permitRequests.collectAsState()
    val uiState by viewModel.requestState.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg, modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                GlScreenHeader(
                    title = "Permits",
                    subtitle = "${allPermits.size} cached",
                    actions = {
                        GlButton(
                            text = "Scan",
                            onClick = onValidate,
                            variant = GlButtonVariant.Gold,
                            size = GlButtonSize.Sm,
                            fillMaxWidth = false,
                            leadingIcon = "qr",
                        )
                    },
                )
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlSearchBar(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchChange,
                        placeholder = "Search permit #, grower, plate",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlKpiTile("Active", allPermits.count { it.status == "APPROVED" }.toString(), Modifier.weight(1f), tone = GlTone.Primary)
                        GlKpiTile("Pending", allPermits.count { it.status == "PENDING" }.toString(), Modifier.weight(1f), tone = GlTone.Gold)
                        GlKpiTile("Bales", allPermits.sumOf { it.total_bales }.toString(), Modifier.weight(1f))
                    }
                    GlFilterPills(
                        options = PermitStatusFilterLabels.keys.toList(),
                        selected = PermitStatusFilterLabels.entries.firstOrNull { it.value == uiState.statusFilter }?.key ?: "All",
                        onSelect = { label -> viewModel.onStatusFilterChange(PermitStatusFilterLabels[label] ?: "All") },
                    )
                }
                if (permits.isEmpty() && requests.isEmpty()) {
                    GlEmptyState(title = "No permits yet", subtitle = "Requested and cached permits will appear here.", icon = "permit")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (requests.isNotEmpty()) {
                            item {
                                GlSectionHeader(
                                    title = "Local requests",
                                    action = "Sync",
                                    onAction = viewModel::syncAllPending,
                                )
                            }
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
            GlFab(
                onClick = onNew,
                icon = "plus",
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            )
        }
    }
}

/** Friendly filter labels mapped to the real permit status values used by the API. */
private val PermitStatusFilterLabels = linkedMapOf(
    "All" to "All",
    "Active" to "APPROVED",
    "Pending" to "PENDING",
    "Returned" to "RETURNED_FOR_CORRECTION",
    "Rejected" to "REJECTED",
)

private fun prettyPermitStatus(status: String): String = when (status) {
    "APPROVED" -> "Active"
    "PENDING" -> "Pending"
    "REJECTED" -> "Rejected"
    "RETURNED_FOR_CORRECTION" -> "Returned"
    else -> status.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
fun PermitDetailScreen(
    localId: String,
    onOpenCorrection: (String) -> Unit = {},
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val transport by remember(localId) { viewModel.observeTransportPermit(localId) }.collectAsState()
    val request by remember(localId) { viewModel.observePermitRequest(localId) }.collectAsState()
    val canApprove by viewModel.canApprovePermit.collectAsState()
    val reviewState by viewModel.reviewState.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Permit detail")
            Column(
                Modifier.fillMaxSize().glVerticalScroll().padding(horizontal = 20.dp).padding(bottom = 24.dp),
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
    val c = glColors()
    val tone = when (permit.status) {
        "APPROVED" -> GlTone.Primary
        "PENDING" -> GlTone.Gold
        "REJECTED" -> GlTone.Danger
        else -> GlTone.Default
    }
    val tileBg = when (permit.status) {
        "APPROVED" -> c.primarySoft
        "PENDING" -> c.goldSoft
        else -> c.surfaceAlt
    }
    val tileFg = when (permit.status) {
        "APPROVED" -> c.primary
        "PENDING" -> c.goldDeep
        else -> c.textMuted
    }
    val validity = when {
        permit.valid_from != null && permit.valid_to != null -> "Valid · ${permit.valid_from} → ${permit.valid_to}"
        permit.status == "PENDING" -> "Pending approval"
        permit.status == "REJECTED" -> permit.rejection_reason?.let { "Rejected · $it" } ?: "Rejected"
        permit.status == "RETURNED_FOR_CORRECTION" -> "Returned for correction"
        else -> prettyPermitStatus(permit.status)
    }
    GlCard(onClick = onClick, contentPadding = 0.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(tileBg),
                    contentAlignment = Alignment.Center,
                ) {
                    GlIcon("permit", size = 26.dp, tint = tileFg)
                }
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            permit.permit_number ?: "Pending #",
                            color = c.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                        )
                        GlPill(text = prettyPermitStatus(permit.status), size = GlPillSize.Sm, tone = tone)
                    }
                    permit.grower_name?.let {
                        Text(it, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(
                        "${permit.total_bales} bales · ${permit.total_weight_kg} kg · ${permit.purpose.lowercase().replaceFirstChar { ch -> ch.uppercase() }}",
                        color = c.textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            GlDivider()
            Row(
                Modifier.fillMaxWidth().background(c.surfaceAlt).padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GlIcon("calendar", size = 12.dp, tint = c.textMuted)
                    Text(validity, color = c.textMuted, fontSize = 11.sp)
                }
                GlIcon("chevron-right", size = 16.dp, tint = c.textSubtle)
            }
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
    PermitTicketCard(permit)
    GlSectionHeader(title = "Movement")
    PermitMovementCard(permit)
    if (!permit.comments.isNullOrBlank() || permit.is_bought || permit.buyer_accepted) {
        GlSectionHeader(title = "Buyer & notes")
        GlCard(contentPadding = 14.dp) {
            Column {
                GlFieldRow(label = "Tobacco bought", value = if (permit.is_bought) "Yes" else "No")
                GlFieldRow(label = "Buyer confirmed", value = if (permit.buyer_accepted) "Yes" else "No")
                permit.comments?.takeIf { it.isNotBlank() }?.let { GlFieldRow(label = "Comments", value = it) }
            }
        }
    }
    permit.rejection_reason?.let {
        GlBanner(title = "Rejection reason", subtitle = it, tone = GlTone.Danger, icon = "x")
    }
    GlSyncChip(status = permit.sync_status)
    permit.last_sync_error?.let { ErrorText("Sync error: $it") }
}

/** Gradient transport-permit "ticket" card mirroring the design handoff. */
@Composable
private fun PermitTicketCard(permit: TransportPermitEntity) {
    val c = glColors()
    val validity = when {
        permit.valid_from != null && permit.valid_to != null -> "${permit.valid_from} → ${permit.valid_to}"
        else -> "Not yet set"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(c.primaryDeep, c.primary)))
            .padding(20.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(
                        "TRANSPORT PERMIT",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        permit.permit_number ?: "Pending number",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                GlPill(text = prettyPermitStatus(permit.status), tone = GlTone.Gold)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(108.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    GlIcon("qr", size = 72.dp, tint = c.primaryDeep)
                }
                Column(Modifier.weight(1f)) {
                    Text("Grower", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(
                        permit.grower_name ?: "—",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        TicketStat("Bales", permit.total_bales.toString())
                        TicketStat("Weight", "${permit.total_weight_kg.toInt()} kg")
                        TicketStat("Type", permit.purpose.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Valid · ", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Text(validity, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TicketStat(label: String, value: String) {
    Column {
        Text(
            label.uppercase(),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** From → To movement card with vehicle / purpose details. */
@Composable
private fun PermitMovementCard(permit: TransportPermitEntity) {
    val c = glColors()
    GlCard(contentPadding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.primarySoft),
                    contentAlignment = Alignment.Center,
                ) { GlIcon("map-pin", size = 18.dp, tint = c.primary) }
                Column {
                    Text("From", color = c.textMuted, fontSize = 12.sp)
                    Text(
                        "${permit.origin_district}, ${permit.origin_province}",
                        color = c.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.goldSoft),
                    contentAlignment = Alignment.Center,
                ) { GlIcon("building", size = 18.dp, tint = c.goldDeep) }
                Column {
                    Text("To", color = c.textMuted, fontSize = 12.sp)
                    Text(
                        permit.destination_sales_floor,
                        color = c.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            GlDivider(Modifier.padding(vertical = 14.dp))
            GlFieldRow(label = "Vehicle", value = permit.license_plate)
            GlFieldRow(label = "Purpose", value = permit.purpose.lowercase().replaceFirstChar { it.uppercase() })
        }
    }
}

/** Prominent valid / rejected result card for QR validation, mirroring the design handoff. */
@Composable
private fun PermitValidationResult(result: zm.co.tbz.goldenleaf.ui.marketing.VerifiedPermitInfo) {
    val c = glColors()
    val valid = result.valid
    val accent = if (valid) c.success else c.danger
    val soft = if (valid) c.successSoft else c.dangerSoft
    GlCard(contentPadding = 20.dp) {
        Column(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(soft, c.surface))),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                GlIcon(if (valid) "check" else "x", size = 48.dp, tint = Color.White)
            }
            Text(
                if (valid) "Permit is valid" else "Permit rejected",
                color = c.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                if (valid) "Cleared for movement" else "Validation failed — see details below",
                color = c.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
    GlCard(contentPadding = 14.dp) {
        Column {
            result.permitNumber?.takeIf { it.isNotBlank() }?.let { GlFieldRow(label = "Permit", value = it, mono = true) }
            result.growerName?.takeIf { it.isNotBlank() }?.let { GlFieldRow(label = "Grower", value = it) }
            result.tbzId?.takeIf { it.isNotBlank() }?.let { GlFieldRow(label = "TBZ ID", value = it, mono = true) }
            GlFieldRow(label = "Bales", value = "${result.totalBales ?: 0}")
            GlFieldRow(label = "Remaining", value = "${result.remainingBales ?: 0}")
            result.status?.takeIf { it.isNotBlank() }?.let { GlFieldRow(label = "Status", value = prettyPermitStatus(it)) }
        }
    }
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
