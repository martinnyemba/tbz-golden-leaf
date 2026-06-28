package zm.co.tbz.goldenleaf.ui.marketing

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.PendingSaleEntity
import zm.co.tbz.goldenleaf.ui.components.ErrorText
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
fun MarketingHubScreen(
    onSalesCapture: () -> Unit,
    onPendingSales: () -> Unit,
    viewModel: MarketingViewModel = hiltViewModel(),
) {
    val stats by viewModel.hubStats.collectAsState()
    val pending by viewModel.pendingSales.collectAsState()

    Scaffold(topBar = { TbzTopBar("Marketing") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TbzStatCard("Pending", stats.pendingSales.toString(), Modifier.weight(1f))
                TbzStatCard("Synced", stats.syncedBales.toString(), Modifier.weight(1f))
            }
            TbzStatCard("Failed", stats.failedSales.toString())
            ModuleHubCard("Sales capture", "3-step permit validate, batch, bales", onSalesCapture)
            ModuleHubCard("Pending sales", "Offline queue for bulk-create sync", onPendingSales)
            if (pending.isNotEmpty()) {
                Text("Recent pending", fontWeight = FontWeight.SemiBold)
                pending.take(3).forEach { PendingSaleRow(it) }
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

    Scaffold(topBar = {
        TbzTopBar(
            when (state.step) {
                1 -> "Sales — Permit"
                2 -> "Sales — Batch"
                else -> "Sales — Bales"
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { state.step / 3f },
                modifier = Modifier.fillMaxWidth(),
            )
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

    Scaffold(topBar = { TbzTopBar("Pending sales") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(
                onClick = viewModel::syncAllPending,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) { Text("Sync all pending") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
fun EditPendingSaleScreen(
    localId: String,
    onSaved: () -> Unit,
    viewModel: MarketingViewModel = hiltViewModel(),
) {
    val sale by viewModel.observePendingSale(localId).collectAsState()
    val state by viewModel.captureState.collectAsState()
    val buyers by viewModel.buyers.collectAsState()

    LaunchedEffect(sale?.local_id) {
        sale?.let { viewModel.loadPendingSaleForEdit(it) }
    }

    Scaffold(topBar = { TbzTopBar("Edit pending sale") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (sale == null) {
                Text("Pending sale not found", Modifier.padding(16.dp))
            } else {
                InfoBanner("Permit: ${sale!!.permit_token.take(12)}…")
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
        FormSectionTitle("Step 1 — Validate permit")
        FormTextField(
            permitToken,
            onPermitTokenChange,
            "Permit token / QR code",
            error = errors["permitToken"],
        )
        zm.co.tbz.goldenleaf.ui.components.QrScanButton(
            onScan = onPermitTokenChange,
            modifier = Modifier.fillMaxWidth(),
        )
        TbzDropdownField(
            label = "Sales floor",
            options = salesFloors,
            selectedId = salesfloorId,
            onSelected = onSalesfloorChange,
        )
        errors["salesfloorId"]?.let { ErrorText(it) }
        verifyError?.let { ErrorText(it) }
        verified?.takeIf { it.valid }?.let {
            InfoBanner(
                "Valid: ${it.permitNumber.orEmpty()} — ${it.growerName.orEmpty()} " +
                    "(${it.remainingBales ?: 0} bales remaining)",
            )
        }
        FormActionRow(
            primaryLabel = if (isVerifying) "Verifying…" else "Verify & continue",
            onPrimary = onVerify,
            secondaryLabel = "Cancel",
            onSecondary = onCancel,
            primaryEnabled = !isVerifying,
        )
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
        FormSectionTitle("Step 2 — Batch info")
        verified?.let {
            InfoBanner("${it.growerName.orEmpty()} (${it.tbzId.orEmpty()})")
        }
        FormTextField(
            batch.growerId,
            { onBatchChange(batch.copy(growerId = it)) },
            "Grower ID (UUID)",
            error = errors["growerId"],
        )
        FormTextField(
            batch.season,
            { onBatchChange(batch.copy(season = it)) },
            "Season (e.g. 2025/2026)",
            error = errors["season"],
        )
        TbzDropdownField(
            label = "Sales floor",
            options = salesFloors,
            selectedId = batch.salesfloorId,
            onSelected = { onBatchChange(batch.copy(salesfloorId = it)) },
        )
        errors["salesfloorId"]?.let { ErrorText(it) }
        TbzDropdownField(
            label = "Buyer (optional)",
            options = listOf("" to "None") + buyers,
            selectedId = batch.buyerId,
            onSelected = { onBatchChange(batch.copy(buyerId = it)) },
        )
        FormTextField(
            batch.saleDate,
            { onBatchChange(batch.copy(saleDate = it)) },
            "Sale date (YYYY-MM-DD)",
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
    ScrollableFormColumn {
        FormSectionTitle("Step 3 — Bale rows")
        rowErrors["__global"]?.values?.forEach { ErrorText(it) }
        bales.forEachIndexed { index, row ->
            val errs = rowErrors[row.localRowId].orEmpty()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bale ${index + 1}", fontWeight = FontWeight.SemiBold)
                    FormTextField(
                        row.baleTicketNumber,
                        { onBaleChange(row.localRowId) { r -> r.copy(baleTicketNumber = it) } },
                        "Ticket number",
                        error = errs["baleTicketNumber"],
                    )
                    FormTextField(
                        row.gradeMark,
                        { onBaleChange(row.localRowId) { r -> r.copy(gradeMark = it) } },
                        "Grade mark",
                        error = errs["gradeMark"],
                    )
                    FormTextField(
                        row.weightKg,
                        { onBaleChange(row.localRowId) { r -> r.copy(weightKg = it) } },
                        "Weight (kg)",
                        error = errs["weightKg"],
                    )
                    TbzDropdownField(
                        label = "Status",
                        options = MarketingFormChoices.baleStatuses,
                        selectedId = row.status,
                        onSelected = { onBaleChange(row.localRowId) { r -> r.copy(status = it) } },
                    )
                    if (row.status == "REJECTED") {
                        TbzDropdownField(
                            label = "Rejection reason",
                            options = MarketingFormChoices.rejectionReasons,
                            selectedId = row.rejectionReason,
                            onSelected = { onBaleChange(row.localRowId) { r -> r.copy(rejectionReason = it) } },
                        )
                        errs["rejectionReason"]?.let { ErrorText(it) }
                    }
                    if (row.status == "BOUGHT") {
                        FormTextField(
                            row.pricePerKg,
                            { onBaleChange(row.localRowId) { r -> r.copy(pricePerKg = it) } },
                            "Price per kg (USD)",
                            error = errs["pricePerKg"],
                        )
                        TbzDropdownField(
                            label = "Buyer",
                            options = listOf("" to "None") + buyers,
                            selectedId = row.buyerId,
                            onSelected = { onBaleChange(row.localRowId) { r -> r.copy(buyerId = it) } },
                        )
                    }
                    TbzDropdownField(
                        label = "Tobacco type",
                        options = MarketingFormChoices.tobaccoTypes,
                        selectedId = row.tobaccoType,
                        onSelected = { onBaleChange(row.localRowId) { r -> r.copy(tobaccoType = it) } },
                    )
                    if (bales.size > 1) {
                        OutlinedButton(onClick = { onRemoveRow(row.localRowId) }) {
                            Text("Remove bale")
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onAddRow, modifier = Modifier.fillMaxWidth()) {
            Text("Add bale row")
        }
        saveError?.let { ErrorText(it) }
        FormActionRow(
            primaryLabel = if (isSaving) "Saving…" else "Queue for sync",
            onPrimary = onSubmit,
            secondaryLabel = backLabel,
            onSecondary = onBack,
            primaryEnabled = !isSaving,
        )
    }
}

@Composable
private fun PendingSaleRow(
    sale: PendingSaleEntity,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Permit: ${sale.permit_token.take(16)}…", fontWeight = FontWeight.SemiBold)
            Text("Local ID: ${sale.local_id.take(8)}…")
            SyncStatusChip(sale.sync_status)
            onDelete?.let {
                OutlinedButton(onClick = it, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete")
                }
            }
        }
    }
}
