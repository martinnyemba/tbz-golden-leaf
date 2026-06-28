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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.PermitRequestEntity
import zm.co.tbz.goldenleaf.data.local.entity.TransportPermitEntity
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.QrScanButton
import zm.co.tbz.goldenleaf.ui.components.InfoBanner
import zm.co.tbz.goldenleaf.ui.components.ModuleHubCard
import zm.co.tbz.goldenleaf.ui.components.SyncStatusChip
import zm.co.tbz.goldenleaf.ui.components.TbzStatCard
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar
import zm.co.tbz.goldenleaf.ui.registration.FormActionRow
import zm.co.tbz.goldenleaf.ui.registration.FormSectionTitle
import zm.co.tbz.goldenleaf.ui.registration.FormTextField
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn
import zm.co.tbz.goldenleaf.ui.registration.TbzDropdownField

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

    Scaffold(topBar = { TbzTopBar("Permits") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TbzStatCard("Transport", stats.transportPermits.toString(), Modifier.weight(1f))
                TbzStatCard("Pending", stats.pendingRequests.toString(), Modifier.weight(1f))
            }
            TbzStatCard("Group", stats.groupPermits.toString())
            ModuleHubCard("Request permit", "3-step grower, movement, buyer", onRequest)
            ModuleHubCard("Validate permit", "Scan / verify QR token", onValidate)
            ModuleHubCard("Permit list", "Cached transport permits", onList)
            ModuleHubCard("Group permits", "Multi-grower transport permits", onGroupPermits)
            if (requests.isNotEmpty()) {
                Text("Recent requests", fontWeight = FontWeight.SemiBold)
                requests.take(3).forEach { PermitRequestRow(it) }
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

    Scaffold(topBar = {
        TbzTopBar(
            when (uiState.step) {
                1 -> "Permit — Grower"
                2 -> "Permit — Movement"
                else -> "Permit — Buyer"
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { uiState.step / 3f },
                modifier = Modifier.fillMaxWidth(),
            )
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

    Scaffold(topBar = { TbzTopBar("Validate permit") }) { padding ->
        ScrollableFormColumn(Modifier.padding(padding)) {
            FormTextField(
                state.permitToken,
                viewModel::updateValidateToken,
                "Permit token / QR code",
            )
            QrScanButton(
                onScan = viewModel::updateValidateToken,
                modifier = Modifier.fillMaxWidth(),
            )
            TbzDropdownField(
                label = "Sales floor (optional)",
                options = listOf("" to "Any") + salesFloors.map { it.id to it.name },
                selectedId = state.salesfloorId,
                onSelected = viewModel::updateValidateSalesfloor,
            )
            state.verifyError?.let { ErrorText(it) }
            state.result?.let { result ->
                if (result.valid) {
                    InfoBanner(
                        "Valid permit ${result.permitNumber.orEmpty()} — " +
                            "${result.growerName.orEmpty()} (${result.status.orEmpty()})",
                    )
                    Text("TBZ ID: ${result.tbzId.orEmpty()}")
                    Text("Total bales: ${result.totalBales ?: 0}")
                    Text("Remaining: ${result.remainingBales ?: 0}")
                }
            }
            Button(
                onClick = viewModel::verifyPermit,
                enabled = !state.isVerifying,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isVerifying) "Verifying…" else "Verify")
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

    Scaffold(topBar = { TbzTopBar("Permit list") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text("Search permit #, grower, plate") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PermitFormChoices.statusFilters.take(4).forEach { status ->
                    FilterChip(
                        selected = uiState.statusFilter == status,
                        onClick = { viewModel.onStatusFilterChange(status) },
                        label = { Text(status.replace('_', ' ')) },
                    )
                }
            }
            Button(
                onClick = viewModel::syncAllPending,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) { Text("Sync pending requests") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (requests.isNotEmpty()) {
                    item { Text("Local requests", fontWeight = FontWeight.SemiBold) }
                    items(requests, key = { "req-${it.local_id}" }) { req ->
                        PermitRequestRow(req, onClick = { onOpenDetail(req.local_id) })
                    }
                }
                item { Text("Transport permits", fontWeight = FontWeight.SemiBold) }
                items(permits, key = { it.local_id }) { permit ->
                    TransportPermitRow(permit, onClick = { onOpenDetail(permit.local_id) })
                }
            }
        }
    }
}

@Composable
fun PermitDetailScreen(
    localId: String,
    viewModel: PermitViewModel = hiltViewModel(),
) {
    val transport by viewModel.observeTransportPermit(localId).collectAsState()
    val request by viewModel.observePermitRequest(localId).collectAsState()
    val canApprove by viewModel.canApprovePermit.collectAsState()
    val reviewState by viewModel.reviewState.collectAsState()

    Scaffold(topBar = { TbzTopBar("Permit detail") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        transport!!.correction_reason?.let { InfoBanner("Correction reason: $it") }
                        Text("Use the portal or sync to resubmit corrections after editing.")
                    }
                    if (reviewState.submitSuccess) {
                        InfoBanner("Review queued for sync")
                    }
                }
                request != null -> PermitRequestDetail(request!!, onDelete = { viewModel.deletePermitRequest(it) })
                else -> Text("Permit not found")
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
        FormSectionTitle("Step 1 — Grower")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormTextField(
                form.growerSearch,
                { onFormChange(form.copy(growerSearch = it)) },
                "TBZ ID / NRC lookup",
                modifier = Modifier.weight(1f),
                error = errors["growerSearch"],
            )
            Button(onClick = onLookup) { Text("Look up") }
        }
        TbzDropdownField(
            label = "Grower",
            options = growers,
            selectedId = form.growerId,
            onSelected = { onFormChange(form.copy(growerId = it)) },
        )
        errors["growerId"]?.let { ErrorText(it) }
        TbzDropdownField(
            label = "Grower category",
            options = PermitFormChoices.growerCategories,
            selectedId = form.growerCategory,
            onSelected = { onFormChange(form.copy(growerCategory = it)) },
        )
        FormActionRow(
            primaryLabel = "Next",
            onPrimary = onNext,
            secondaryLabel = "Cancel",
            onSecondary = onCancel,
        )
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
        FormSectionTitle("Step 2 — Movement")
        FormTextField(
            form.totalBales,
            { onFormChange(form.copy(totalBales = it)) },
            "Total bales",
            error = errors["totalBales"],
        )
        FormTextField(
            form.totalWeightKg,
            { onFormChange(form.copy(totalWeightKg = it)) },
            "Total weight (kg)",
            error = errors["totalWeightKg"],
        )
        FormTextField(
            form.licensePlate,
            { onFormChange(form.copy(licensePlate = it)) },
            "License plate",
            error = errors["licensePlate"],
        )
        TbzDropdownField(
            label = "Origin province",
            options = provinces,
            selectedId = form.originProvince,
            onSelected = { onFormChange(form.copy(originProvince = it, originDistrict = "")) },
        )
        errors["originProvince"]?.let { ErrorText(it) }
        TbzDropdownField(
            label = "Origin district",
            options = districts,
            selectedId = form.originDistrict,
            onSelected = { onFormChange(form.copy(originDistrict = it)) },
        )
        errors["originDistrict"]?.let { ErrorText(it) }
        TbzDropdownField(
            label = "Destination sales floor",
            options = salesFloors,
            selectedId = form.destinationSalesFloor,
            onSelected = { onFormChange(form.copy(destinationSalesFloor = it)) },
        )
        errors["destinationSalesFloor"]?.let { ErrorText(it) }
        TbzDropdownField(
            label = "Purpose",
            options = PermitFormChoices.purposes,
            selectedId = form.purpose,
            onSelected = { onFormChange(form.copy(purpose = it)) },
        )
        FormActionRow(
            primaryLabel = "Next",
            onPrimary = onNext,
            secondaryLabel = "Back",
            onSecondary = onBack,
        )
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
        FormSectionTitle("Step 3 — Buyer & notes")
        TbzDropdownField(
            label = "Buyer",
            options = listOf("" to "None") + buyers,
            selectedId = form.buyerId,
            onSelected = { onFormChange(form.copy(buyerId = it)) },
        )
        errors["buyerId"]?.let { ErrorText(it) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = form.isBought, onCheckedChange = { onFormChange(form.copy(isBought = it)) })
            Text("Tobacco already bought")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = form.buyerAccepted, onCheckedChange = { onFormChange(form.copy(buyerAccepted = it)) })
            Text("Buyer accepted / confirmed")
        }
        FormTextField(
            form.comments,
            { onFormChange(form.copy(comments = it)) },
            "Comments",
            singleLine = false,
        )
        saveError?.let { ErrorText(it) }
        FormActionRow(
            primaryLabel = if (isSaving) "Submitting…" else "Submit request",
            onPrimary = onSubmit,
            secondaryLabel = "Back",
            onSecondary = onBack,
            primaryEnabled = !isSaving,
        )
    }
}

@Composable
private fun TransportPermitRow(permit: TransportPermitEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(permit.permit_number ?: "Pending #", fontWeight = FontWeight.SemiBold)
            permit.grower_name?.let { Text(it) }
            Text("${permit.total_bales} bales · ${permit.total_weight_kg} kg")
            Text("Plate: ${permit.license_plate} · ${permit.purpose}")
            Text("Status: ${permit.status}")
            SyncStatusChip(permit.sync_status)
        }
    }
}

@Composable
private fun PermitRequestRow(
    request: PermitRequestEntity,
    onClick: (() -> Unit)? = null,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Local request ${request.local_id.take(8)}…", fontWeight = FontWeight.SemiBold)
            Text("Created: ${request.created_at}")
            SyncStatusChip(request.sync_status)
        }
    }
}

@Composable
private fun TransportPermitDetail(permit: TransportPermitEntity) {
    Text(permit.permit_number ?: "Transport permit", fontWeight = FontWeight.Bold)
    permit.grower_name?.let { Text("Grower: $it") }
    Text("Bales: ${permit.total_bales} · Weight: ${permit.total_weight_kg} kg")
    Text("Plate: ${permit.license_plate}")
    Text("From: ${permit.origin_province} / ${permit.origin_district}")
    Text("To: ${permit.destination_sales_floor}")
    Text("Purpose: ${permit.purpose}")
    Text("Status: ${permit.status}")
    permit.valid_from?.let { Text("Valid from: $it") }
    permit.valid_to?.let { Text("Valid to: $it") }
    permit.correction_reason?.let { Text("Correction: $it") }
    permit.rejection_reason?.let { Text("Rejection: $it") }
    permit.comments?.let { Text("Comments: $it") }
    SyncStatusChip(permit.sync_status)
    permit.last_sync_error?.let { ErrorText("Sync error: $it") }
}

@Composable
private fun PermitRequestDetail(
    request: PermitRequestEntity,
    onDelete: (PermitRequestEntity) -> Unit,
) {
    Text("Local permit request", fontWeight = FontWeight.Bold)
    Text("Local ID: ${request.local_id}")
    Text("Created: ${request.created_at}")
    SyncStatusChip(request.sync_status)
    Text("Payload preview:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
    Text(request.data_json)
    OutlinedButton(onClick = { onDelete(request) }, modifier = Modifier.fillMaxWidth()) {
        Text("Delete local request")
    }
}
