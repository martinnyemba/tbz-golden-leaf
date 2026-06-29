package zm.co.tbz.goldenleaf.ui.marketing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.PendingSaleEntity
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDropdownField
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFieldRow
import zm.co.tbz.goldenleaf.ui.components.GlKpiTile
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlStepper
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.GlTextField
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.QrScanButton
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn

@Composable
fun MarketingHubScreen(
    onSalesCapture: () -> Unit,
    onPendingSales: () -> Unit,
    viewModel: MarketingViewModel = hiltViewModel(),
) {
    val stats by viewModel.hubStats.collectAsState()
    val pending by viewModel.pendingSales.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Marketing")
            ScrollableFormColumn {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlKpiTile(label = "Pending", value = stats.pendingSales.toString(), modifier = Modifier.weight(1f))
                    GlKpiTile(
                        label = "Synced",
                        value = stats.syncedBales.toString(),
                        tone = GlTone.Success,
                        modifier = Modifier.weight(1f),
                    )
                    GlKpiTile(
                        label = "Failed",
                        value = stats.failedSales.toString(),
                        tone = GlTone.Danger,
                        modifier = Modifier.weight(1f),
                    )
                }
                GlSectionHeader(title = "Quick actions")
                GlCard(contentPadding = 0.dp) {
                    Column {
                        GlRow(
                            title = "Sales capture",
                            subtitle = "3-step permit validate, batch, bales",
                            leadingIcon = "bale",
                            onClick = onSalesCapture,
                        )
                        GlRow(
                            title = "Pending sales",
                            subtitle = "Offline queue for bulk-create sync",
                            leadingIcon = "cloud-up",
                            onClick = onPendingSales,
                        )
                    }
                }
                if (pending.isNotEmpty()) {
                    GlSectionHeader(title = "Recent pending")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pending.take(3).forEach { PendingSaleRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesCaptureScreen(
    onDone: () -> Unit,
    viewModel: MarketingViewModel = hiltViewModel(),
) {
    val state by viewModel.captureState.collectAsState()
    val salesFloors by viewModel.salesFloors.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(
                title = when (state.step) {
                    1 -> "Sales — Permit"
                    2 -> "Sales — Batch"
                    else -> "Sales — Bales"
                },
            )
            GlStepper(step = state.step, total = 3, modifier = Modifier.padding(horizontal = 16.dp))
            when (state.step) {
                1 -> PermitValidateStep(
                    permitToken = state.permitToken,
                    salesfloorId = state.salesfloorId,
                    salesFloors = salesFloors.map { it.id to it.name },
                    errors = state.stepErrors,
                    isVerifying = state.isVerifying,
                    verifyError = state.verifyError,
                    verified = state.verifiedPermit,
                    onPermitTokenChange = { viewModel.updateCapture { s -> s.copy(permitToken = it) } },
                    onSalesfloorChange = { viewModel.updateCapture { s -> s.copy(salesfloorId = it) } },
                    onVerify = { viewModel.verifyPermitStep1() },
                    onCancel = onDone,
                )
                2 -> BatchInfoStep(
                    batch = state.batch,
                    verified = state.verifiedPermit,
                    salesFloors = salesFloors.map { it.id to it.name },
                    buyers = buyers.map { it.id to it.name },
                    errors = state.stepErrors,
                    onBatchChange = { viewModel.updateCapture { s -> s.copy(batch = it) } },
                    onBack = { viewModel.goToPermitStep() },
                    onNext = { viewModel.goToBaleStep() },
                )
                else -> BaleRowsStep(
                    bales = state.bales,
                    buyers = buyers.map { it.id to it.name },
                    rowErrors = state.rowErrors,
                    isSaving = state.isSaving,
                    saveError = state.saveError,
                    onBaleChange = viewModel::updateBaleRow,
                    onAddRow = viewModel::addBaleRow,
                    onRemoveRow = viewModel::removeBaleRow,
                    onBack = { viewModel.goToBatchStep() },
                    onSubmit = { viewModel.queuePendingSale { onDone() } },
                )
            }
        }
    }
}

@Composable
fun PendingSalesScreen(
    onEdit: (String) -> Unit,
    viewModel: MarketingViewModel = hiltViewModel(),
) {
    val pending by viewModel.pendingSales.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Pending sales")
            Column(Modifier.padding(horizontal = 16.dp)) {
                GlButton(
                    text = "Sync all pending",
                    onClick = viewModel::syncAllPending,
                    variant = GlButtonVariant.Outline,
                    leadingIcon = "sync",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            if (pending.isEmpty()) {
                GlEmptyState(title = "No pending sales", icon = "bale", subtitle = "Captured sales will queue here for sync")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(pending, key = { it.local_id }) { sale ->
                        PendingSaleRow(
                            sale = sale,
                            onClick = { onEdit(sale.local_id) },
                            onDelete = { viewModel.deletePendingSale(sale) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditPendingSaleScreen(
    localId: String,
    onSaved: () -> Unit,
    viewModel: MarketingViewModel = hiltViewModel(),
) {
    val sale by viewModel.observePendingSale(localId).collectAsState()
    val state by viewModel.captureState.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val c = glColors()

    LaunchedEffect(sale?.local_id) {
        sale?.let { viewModel.loadPendingSaleForEdit(it) }
    }

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Edit pending sale")
            if (sale == null) {
                GlEmptyState(title = "Pending sale not found", icon = "search")
            } else {
                GlBanner(
                    title = "Permit: ${sale!!.permit_token.take(12)}…",
                    tone = GlTone.Info,
                    icon = "info",
                )
                BaleRowsStep(
                    bales = state.bales,
                    buyers = buyers.map { it.id to it.name },
                    rowErrors = state.rowErrors,
                    isSaving = state.isSaving,
                    saveError = state.saveError,
                    onBaleChange = viewModel::updateBaleRow,
                    onAddRow = viewModel::addBaleRow,
                    onRemoveRow = viewModel::removeBaleRow,
                    onBack = onSaved,
                    onSubmit = { viewModel.updatePendingSale(localId, onSaved) },
                    backLabel = "Cancel",
                )
            }
        }
    }
}

@Composable
private fun PermitValidateStep(
    permitToken: String,
    salesfloorId: String,
    salesFloors: List<Pair<String, String>>,
    errors: Map<String, String>,
    isVerifying: Boolean,
    verifyError: String?,
    verified: VerifiedPermitInfo?,
    onPermitTokenChange: (String) -> Unit,
    onSalesfloorChange: (String) -> Unit,
    onVerify: () -> Unit,
    onCancel: () -> Unit,
) {
    ScrollableFormColumn {
        GlSectionHeader(title = "Step 1 — Validate permit")
        GlTextField(
            value = permitToken,
            onValueChange = onPermitTokenChange,
            label = "Permit token / QR code",
            error = errors["permitToken"],
        )
        QrScanButton(
            onScan = onPermitTokenChange,
            modifier = Modifier.fillMaxWidth(),
        )
        GlDropdownField(
            label = "Sales floor",
            options = salesFloors,
            selectedId = salesfloorId,
            onSelected = onSalesfloorChange,
            error = errors["salesfloorId"],
        )
        verifyError?.let { ErrorText(it) }
        verified?.takeIf { it.valid }?.let {
            GlBanner(
                title = "Permit valid",
                subtitle = "${it.permitNumber.orEmpty()} — ${it.growerName.orEmpty()} " +
                    "(${it.remainingBales ?: 0} bales remaining)",
                tone = GlTone.Success,
                icon = "check-circle",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlButton(
                text = "Cancel",
                onClick = onCancel,
                variant = GlButtonVariant.Outline,
                modifier = Modifier.weight(1f),
            )
            GlButton(
                text = if (isVerifying) "Verifying…" else "Verify & continue",
                onClick = onVerify,
                enabled = !isVerifying,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BatchInfoStep(
    batch: SalesBatchForm,
    verified: VerifiedPermitInfo?,
    salesFloors: List<Pair<String, String>>,
    buyers: List<Pair<String, String>>,
    errors: Map<String, String>,
    onBatchChange: (SalesBatchForm) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    ScrollableFormColumn {
        GlSectionHeader(title = "Step 2 — Batch info")
        verified?.let {
            GlBanner(
                title = it.growerName.orEmpty(),
                subtitle = it.tbzId.orEmpty(),
                tone = GlTone.Info,
                icon = "info",
            )
        }
        GlTextField(
            value = batch.growerId,
            onValueChange = { onBatchChange(batch.copy(growerId = it)) },
            label = "Grower ID (UUID)",
            error = errors["growerId"],
        )
        GlTextField(
            value = batch.season,
            onValueChange = { onBatchChange(batch.copy(season = it)) },
            label = "Season (e.g. 2025/2026)",
            error = errors["season"],
        )
        GlDropdownField(
            label = "Sales floor",
            options = salesFloors,
            selectedId = batch.salesfloorId,
            onSelected = { onBatchChange(batch.copy(salesfloorId = it)) },
            error = errors["salesfloorId"],
        )
        GlDropdownField(
            label = "Buyer (optional)",
            options = listOf("" to "None") + buyers,
            selectedId = batch.buyerId,
            onSelected = { onBatchChange(batch.copy(buyerId = it)) },
        )
        GlTextField(
            value = batch.saleDate,
            onValueChange = { onBatchChange(batch.copy(saleDate = it)) },
            label = "Sale date (YYYY-MM-DD)",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlButton(
                text = "Back",
                onClick = onBack,
                variant = GlButtonVariant.Outline,
                modifier = Modifier.weight(1f),
            )
            GlButton(
                text = "Next",
                onClick = onNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BaleRowsStep(
    bales: List<BaleRowForm>,
    buyers: List<Pair<String, String>>,
    rowErrors: Map<String, Map<String, String>>,
    isSaving: Boolean,
    saveError: String?,
    onBaleChange: (String, (BaleRowForm) -> BaleRowForm) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    backLabel: String = "Back",
) {
    val c = glColors()
    ScrollableFormColumn {
        GlSectionHeader(title = "Step 3 — Bale rows")
        rowErrors["__global"]?.values?.forEach { ErrorText(it) }
        bales.forEachIndexed { index, row ->
            val errs = rowErrors[row.localRowId].orEmpty()
            GlCard(contentPadding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bale ${index + 1}", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    GlTextField(
                        value = row.baleTicketNumber,
                        onValueChange = { onBaleChange(row.localRowId) { r -> r.copy(baleTicketNumber = it) } },
                        label = "Ticket number",
                        error = errs["baleTicketNumber"],
                    )
                    GlTextField(
                        value = row.gradeMark,
                        onValueChange = { onBaleChange(row.localRowId) { r -> r.copy(gradeMark = it) } },
                        label = "Grade mark",
                        error = errs["gradeMark"],
                    )
                    GlTextField(
                        value = row.weightKg,
                        onValueChange = { onBaleChange(row.localRowId) { r -> r.copy(weightKg = it) } },
                        label = "Weight (kg)",
                        error = errs["weightKg"],
                    )
                    GlDropdownField(
                        label = "Status",
                        options = MarketingFormChoices.baleStatuses,
                        selectedId = row.status,
                        onSelected = { onBaleChange(row.localRowId) { r -> r.copy(status = it) } },
                    )
                    if (row.status == "REJECTED") {
                        GlDropdownField(
                            label = "Rejection reason",
                            options = MarketingFormChoices.rejectionReasons,
                            selectedId = row.rejectionReason,
                            onSelected = { onBaleChange(row.localRowId) { r -> r.copy(rejectionReason = it) } },
                            error = errs["rejectionReason"],
                        )
                    }
                    if (row.status == "BOUGHT") {
                        GlTextField(
                            value = row.pricePerKg,
                            onValueChange = { onBaleChange(row.localRowId) { r -> r.copy(pricePerKg = it) } },
                            label = "Price per kg (USD)",
                            error = errs["pricePerKg"],
                        )
                        GlDropdownField(
                            label = "Buyer",
                            options = listOf("" to "None") + buyers,
                            selectedId = row.buyerId,
                            onSelected = { onBaleChange(row.localRowId) { r -> r.copy(buyerId = it) } },
                        )
                    }
                    GlDropdownField(
                        label = "Tobacco type",
                        options = MarketingFormChoices.tobaccoTypes,
                        selectedId = row.tobaccoType,
                        onSelected = { onBaleChange(row.localRowId) { r -> r.copy(tobaccoType = it) } },
                    )
                    if (bales.size > 1) {
                        GlButton(
                            text = "Remove bale",
                            onClick = { onRemoveRow(row.localRowId) },
                            variant = GlButtonVariant.DangerOutline,
                        )
                    }
                }
            }
        }
        GlButton(
            text = "Add bale row",
            onClick = onAddRow,
            variant = GlButtonVariant.Outline,
            leadingIcon = "plus",
            modifier = Modifier.fillMaxWidth(),
        )
        saveError?.let { ErrorText(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlButton(
                text = backLabel,
                onClick = onBack,
                variant = GlButtonVariant.Outline,
                modifier = Modifier.weight(1f),
            )
            GlButton(
                text = if (isSaving) "Saving…" else "Queue for sync",
                onClick = onSubmit,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PendingSaleRow(
    sale: PendingSaleEntity,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val c = glColors()
    GlCard(onClick = onClick, contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Permit: ${sale.permit_token.take(16)}…",
                color = c.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            GlFieldRow(label = "Local ID", value = "${sale.local_id.take(8)}…")
            GlSyncChip(status = sale.sync_status)
            onDelete?.let {
                GlButton(
                    text = "Delete",
                    onClick = it,
                    variant = GlButtonVariant.DangerOutline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
