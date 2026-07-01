package zm.co.tbz.goldenleaf.ui.marketing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import zm.co.tbz.goldenleaf.data.local.entity.PendingSaleEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.remote.dto.VerifyQrRequest
import zm.co.tbz.goldenleaf.data.repository.MarketingRepository
import zm.co.tbz.goldenleaf.data.repository.ReferenceRepository
import javax.inject.Inject

@HiltViewModel
class MarketingViewModel @Inject constructor(
    private val marketingRepository: MarketingRepository,
    private val referenceRepository: ReferenceRepository,
    private val api: TrmcsApi,
    private val json: Json,
) : ViewModel() {

    private val _captureState = MutableStateFlow(SalesCaptureState())
    val captureState: StateFlow<SalesCaptureState> = _captureState.asStateFlow()

    val pendingSales = marketingRepository.observePendingSales().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val salesFloors = referenceRepository.observeSalesFloors().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val buyers = referenceRepository.observeBuyers().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val hubStats = pendingSales.map { sales ->
        MarketingHubStats(
            pendingSales = sales.count { it.sync_status == SyncStatuses.PENDING },
            syncedBales = sales.count { it.sync_status == SyncStatuses.SYNCED },
            failedSales = sales.count { it.sync_status == SyncStatuses.FAILED },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MarketingHubStats())

    init {
        viewModelScope.launch { runCatching { referenceRepository.refreshReference() } }
    }

    fun districtsForProvince(provinceId: String) =
        if (provinceId.isBlank()) flowOf(emptyList()) else referenceRepository.observeDistricts(provinceId)

    fun observePendingSale(localId: String): StateFlow<PendingSaleEntity?> =
        pendingSales.map { sales -> sales.firstOrNull { it.local_id == localId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateCapture(transform: (SalesCaptureState) -> SalesCaptureState) {
        _captureState.update { transform(it).copy(stepErrors = emptyMap(), rowErrors = emptyMap()) }
    }

    fun resetCaptureForm() {
        _captureState.value = SalesCaptureState()
    }

    fun verifyPermitStep1(): Boolean {
        val state = _captureState.value
        val errors = mutableMapOf<String, String>()
        if (state.permitToken.isBlank()) errors["permitToken"] = "Permit token / QR code is required"
        if (state.salesfloorId.isBlank()) errors["salesfloorId"] = "Sales floor is required"
        if (errors.isNotEmpty()) {
            _captureState.update { it.copy(stepErrors = errors) }
            return false
        }
        viewModelScope.launch {
            _captureState.update { it.copy(isVerifying = true, verifyError = null) }
            try {
                val response = api.verifyQr(
                    VerifyQrRequest(
                        permit_token = state.permitToken.trim(),
                        salesfloor_id = state.salesfloorId,
                    ),
                )
                if (!response.valid) {
                    _captureState.update {
                        it.copy(
                            isVerifying = false,
                            verifyError = "Permit is not valid for this sales floor",
                        )
                    }
                    return@launch
                }
                _captureState.update {
                    it.copy(
                        isVerifying = false,
                        step = 2,
                        verifiedPermit = VerifiedPermitInfo(
                            valid = true,
                            permitNumber = response.permit_number,
                            growerName = response.grower_name,
                            tbzId = response.tbz_id,
                            totalBales = response.total_bales,
                            remainingBales = response.remaining_bales,
                            status = response.status,
                        ),
                    )
                }
            } catch (e: Exception) {
                _captureState.update {
                    it.copy(
                        isVerifying = false,
                        verifyError = e.message ?: "Permit verification failed",
                    )
                }
            }
        }
        return true
    }

    fun goToBaleStep(): Boolean {
        val errors = validateBatch(_captureState.value.batch)
        if (errors.isNotEmpty()) {
            _captureState.update { it.copy(stepErrors = errors) }
            return false
        }
        _captureState.update { it.copy(step = 3) }
        loadPriceMatrix()
        return true
    }

    /**
     * Loads the buyer + season price matrix so the bale grade picker shows the
     * grades that buyer has had approved (grouped by tobacco type) and can
     * auto-fill matrix prices — mirroring the web capture wizard. Best-effort:
     * offline or no-buyer leaves the matrix empty and grade entry stays manual.
     */
    private fun loadPriceMatrix() {
        val batch = _captureState.value.batch
        if (batch.buyerId.isBlank() || batch.season.isBlank()) {
            _captureState.update { it.copy(gradePriceMatrix = emptyMap()) }
            return
        }
        viewModelScope.launch {
            runCatching { api.priceMatrix(batch.buyerId, batch.season.trim()) }
                .onSuccess { response ->
                    val matrix = response.grades
                        .groupBy { it.tobacco_type }
                        .mapValues { (_, rows) -> rows.associate { it.grade to it.price_per_kg } }
                    _captureState.update { it.copy(gradePriceMatrix = matrix) }
                }
        }
    }

    /** Picks a grade for a bale row and auto-fills its matrix price when one exists. */
    fun selectGrade(rowId: String, grade: String) {
        updateBaleRow(rowId) { row ->
            val price = _captureState.value.gradePriceMatrix[row.tobaccoType]?.get(grade)
            row.copy(gradeMark = grade, pricePerKg = price ?: row.pricePerKg)
        }
    }

    fun goToBatchStep() = _captureState.update { it.copy(step = 2) }
    fun goToPermitStep() = _captureState.update { it.copy(step = 1) }

    fun addBaleRow() {
        _captureState.update { it.copy(bales = it.bales + BaleRowForm()) }
    }

    fun removeBaleRow(rowId: String) {
        _captureState.update { state ->
            if (state.bales.size <= 1) state
            else state.copy(bales = state.bales.filterNot { it.localRowId == rowId })
        }
    }

    fun updateBaleRow(rowId: String, transform: (BaleRowForm) -> BaleRowForm) {
        _captureState.update { state ->
            state.copy(bales = state.bales.map { if (it.localRowId == rowId) transform(it) else it })
        }
    }

    fun loadPendingSaleForEdit(sale: PendingSaleEntity) {
        try {
            val payload = json.parseToJsonElement(sale.data_json) as JsonObject
            val batch = SalesBatchForm(
                growerId = payload.stringOrEmpty("grower"),
                season = payload.stringOrEmpty("season"),
                salesfloorId = payload.stringOrEmpty("salesfloor"),
                buyerId = payload.stringOrEmpty("buyer"),
            )
            val balesArray = payload["bales"] as? JsonArray
            val bales = balesArray?.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                BaleRowForm(
                    baleTicketNumber = obj.stringOrEmpty("bale_ticket_number"),
                    gradeMark = obj.stringOrEmpty("grade_mark"),
                    weightKg = obj.stringOrEmpty("weight_kg"),
                    status = obj.stringOrEmpty("status").ifBlank { "BOUGHT" },
                    rejectionReason = obj.stringOrEmpty("rejection_reason"),
                    buyerId = obj.stringOrEmpty("buyer"),
                    tobaccoType = obj.stringOrEmpty("tobacco_type").ifBlank { "FLUE_CURED" },
                    pricePerKg = obj.stringOrEmpty("price_per_kg"),
                )
            }?.takeIf { it.isNotEmpty() } ?: listOf(BaleRowForm())
            _captureState.value = SalesCaptureState(
                step = 3,
                permitToken = sale.permit_token,
                batch = batch,
                bales = bales,
            )
            loadPriceMatrix()
        } catch (_: Exception) {
            _captureState.value = SalesCaptureState(
                step = 1,
                permitToken = sale.permit_token,
            )
        }
    }

    fun queuePendingSale(onQueued: (String) -> Unit) {
        val rowErrors = validateBales(_captureState.value.bales)
        if (rowErrors.isNotEmpty()) {
            _captureState.update { it.copy(rowErrors = rowErrors) }
            return
        }
        val state = _captureState.value
        viewModelScope.launch {
            _captureState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val payloadJson = buildBulkPayload(state)
                val localId = marketingRepository.queuePendingSale(state.permitToken.trim(), payloadJson)
                resetCaptureForm()
                onQueued(localId)
            } catch (e: Exception) {
                _captureState.update { it.copy(saveError = e.message ?: "Queue failed") }
            } finally {
                _captureState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun updatePendingSale(localId: String, onSaved: () -> Unit) {
        val rowErrors = validateBales(_captureState.value.bales)
        if (rowErrors.isNotEmpty()) {
            _captureState.update { it.copy(rowErrors = rowErrors) }
            return
        }
        val state = _captureState.value
        viewModelScope.launch {
            _captureState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val payloadJson = buildBulkPayload(state)
                marketingRepository.updatePendingSale(localId, state.permitToken.trim(), payloadJson)
                resetCaptureForm()
                onSaved()
            } catch (e: Exception) {
                _captureState.update { it.copy(saveError = e.message ?: "Update failed") }
            } finally {
                _captureState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deletePendingSale(sale: PendingSaleEntity) {
        viewModelScope.launch { marketingRepository.deletePendingSale(sale) }
    }

    fun syncAllPending() {
        viewModelScope.launch { marketingRepository.triggerSync() }
    }

    private fun buildBulkPayload(state: SalesCaptureState): String {
        val batch = state.batch
        val payload = buildJsonObject {
            put("grower", batch.growerId)
            put("season", batch.season)
            put("permit_token", state.permitToken.trim())
            if (batch.salesfloorId.isNotBlank()) put("salesfloor", batch.salesfloorId)
            if (batch.buyerId.isNotBlank()) put("buyer", batch.buyerId)
            put(
                "bales",
                buildJsonArray {
                    state.bales.forEach { row ->
                        add(
                            buildJsonObject {
                                put("bale_ticket_number", row.baleTicketNumber.trim())
                                put("grade_mark", row.gradeMark.trim())
                                put("weight_kg", row.weightKg.toDoubleOrNull() ?: 0.0)
                                put("status", row.status)
                                put("tobacco_type", row.tobaccoType)
                                if (row.status == "REJECTED" && row.rejectionReason.isNotBlank()) {
                                    put("rejection_reason", row.rejectionReason)
                                }
                                if (row.buyerId.isNotBlank()) put("buyer", row.buyerId)
                                if (row.pricePerKg.isNotBlank()) {
                                    put("price_per_kg", row.pricePerKg.toDoubleOrNull() ?: 0.0)
                                }
                                if (batch.saleDate.isNotBlank()) put("sale_date", batch.saleDate)
                                if (batch.salesfloorId.isNotBlank()) put("salesfloor", batch.salesfloorId)
                                if (batch.growerId.isNotBlank()) put("grower", batch.growerId)
                                if (batch.season.isNotBlank()) put("season", batch.season)
                            },
                        )
                    }
                },
            )
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun JsonObject.stringOrEmpty(key: String): String =
        this[key]?.toString()?.trim('"').orEmpty()

    private fun validateBatch(batch: SalesBatchForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (batch.growerId.isBlank()) errors["growerId"] = "Grower is required"
        if (batch.season.isBlank()) errors["season"] = "Season is required (e.g. 2025/2026)"
        if (batch.salesfloorId.isBlank()) errors["salesfloorId"] = "Sales floor is required"
        return errors
    }

    private fun validateBales(bales: List<BaleRowForm>): Map<String, Map<String, String>> {
        val errors = mutableMapOf<String, Map<String, String>>()
        bales.forEach { row ->
            val rowErr = mutableMapOf<String, String>()
            if (row.baleTicketNumber.isBlank()) rowErr["baleTicketNumber"] = "Ticket number required"
            if (row.gradeMark.isBlank()) rowErr["gradeMark"] = "Grade mark required"
            val weight = row.weightKg.toDoubleOrNull()
            if (weight == null || weight < 20.0 || weight > 135.0) {
                rowErr["weightKg"] = "Weight must be 20–135 kg"
            }
            if (row.status == "REJECTED" && row.rejectionReason.isBlank()) {
                rowErr["rejectionReason"] = "Rejection reason required"
            }
            if (row.status == "BOUGHT" && row.pricePerKg.isBlank()) {
                rowErr["pricePerKg"] = "Price per kg required for bought bales"
            }
            if (rowErr.isNotEmpty()) errors[row.localRowId] = rowErr
        }
        if (bales.isEmpty()) errors["__global"] = mapOf("bales" to "At least one bale is required")
        return errors
    }
}
