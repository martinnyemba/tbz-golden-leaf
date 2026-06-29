package zm.co.tbz.goldenleaf.ui.permits

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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import zm.co.tbz.goldenleaf.core.rbac.AccessControlService
import zm.co.tbz.goldenleaf.data.local.entity.PermitRequestEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.local.entity.TransportPermitEntity
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.remote.dto.VerifyQrRequest
import zm.co.tbz.goldenleaf.data.repository.GrowerRepository
import zm.co.tbz.goldenleaf.data.repository.PermitRepository
import zm.co.tbz.goldenleaf.data.repository.ReferenceRepository
import zm.co.tbz.goldenleaf.ui.marketing.VerifiedPermitInfo
import javax.inject.Inject

@HiltViewModel
class PermitViewModel @Inject constructor(
    private val permitRepository: PermitRepository,
    private val growerRepository: GrowerRepository,
    private val referenceRepository: ReferenceRepository,
    private val api: TrmcsApi,
    private val json: Json,
    private val accessControl: AccessControlService,
) : ViewModel() {

    private val _requestState = MutableStateFlow(PermitRequestUiState())
    val requestState: StateFlow<PermitRequestUiState> = _requestState.asStateFlow()

    private val _validateState = MutableStateFlow(PermitValidateState())
    val validateState: StateFlow<PermitValidateState> = _validateState.asStateFlow()

    private val _reviewState = MutableStateFlow(PermitReviewUiState())
    val reviewState: StateFlow<PermitReviewUiState> = _reviewState.asStateFlow()

    private val _correctionState = MutableStateFlow(PermitCorrectionUiState())
    val correctionState: StateFlow<PermitCorrectionUiState> = _correctionState.asStateFlow()

    val canApprovePermit = MutableStateFlow(false)

    val transportPermits = permitRepository.observeTransportPermits().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val groupPermits = permitRepository.observeGroupPermits().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val permitRequests = permitRepository.observePermitRequests().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val growers = growerRepository.observeGrowers().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val provinces = referenceRepository.observeProvinces().stateIn(
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

    val hubStats = combine(transportPermits, permitRequests, groupPermits) { permits, requests, groups ->
        PermitsHubStats(
            transportPermits = permits.size,
            pendingRequests = requests.count { it.sync_status == SyncStatuses.PENDING },
            groupPermits = groups.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PermitsHubStats())

    init {
        viewModelScope.launch {
            canApprovePermit.value = accessControl.hasPermissionAsync("permits.approve_permit")
        }
        viewModelScope.launch { referenceRepository.refreshReference() }
    }

    val filteredPermits = combine(transportPermits, _requestState) { list, state ->
        list.filter { permit ->
            val matchesSearch = state.searchQuery.isBlank() ||
                permit.permit_number?.contains(state.searchQuery, ignoreCase = true) == true ||
                permit.grower_name?.contains(state.searchQuery, ignoreCase = true) == true ||
                permit.license_plate.contains(state.searchQuery, ignoreCase = true)
            val matchesStatus = state.statusFilter == "All" || permit.status == state.statusFilter
            matchesSearch && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun districtsForProvince(provinceId: String) =
        if (provinceId.isBlank()) flowOf(emptyList()) else referenceRepository.observeDistricts(provinceId)

    fun observeTransportPermit(localId: String): StateFlow<TransportPermitEntity?> =
        transportPermits.map { list -> list.firstOrNull { it.local_id == localId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun observePermitRequest(localId: String): StateFlow<PermitRequestEntity?> =
        permitRequests.map { list -> list.firstOrNull { it.local_id == localId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onSearchChange(value: String) = _requestState.update { it.copy(searchQuery = value) }
    fun onStatusFilterChange(value: String) = _requestState.update { it.copy(statusFilter = value) }

    fun updateForm(transform: (PermitRequestForm) -> PermitRequestForm) {
        _requestState.update { it.copy(form = transform(it.form), fieldErrors = emptyMap()) }
    }

    fun resetRequestForm() {
        _requestState.value = PermitRequestUiState()
    }

    fun goToMovementStep(): Boolean {
        val errors = validateGrowerStep(_requestState.value.form)
        if (errors.isNotEmpty()) {
            _requestState.update { it.copy(fieldErrors = errors) }
            return false
        }
        _requestState.update { it.copy(step = 2) }
        return true
    }

    fun goToBuyerStep(): Boolean {
        val errors = validateMovementStep(_requestState.value.form)
        if (errors.isNotEmpty()) {
            _requestState.update { it.copy(fieldErrors = errors) }
            return false
        }
        _requestState.update { it.copy(step = 3) }
        return true
    }

    fun goToGrowerStep() = _requestState.update { it.copy(step = 1) }
    fun goToMovementStepBack() = _requestState.update { it.copy(step = 2) }

    fun lookupGrowerByTbzId() {
        val query = _requestState.value.form.growerSearch.trim()
        if (query.isBlank()) return
        val match = growers.value.firstOrNull {
            it.tbz_id?.equals(query, ignoreCase = true) == true ||
                it.nrc_number.equals(query, ignoreCase = true)
        }
        if (match != null) {
            _requestState.update {
                it.copy(
                    form = it.form.copy(
                        growerId = match.remote_id ?: match.local_id,
                        growerCategory = match.category ?: it.form.growerCategory,
                        originProvince = match.province.orEmpty(),
                        originDistrict = match.district.orEmpty(),
                    ),
                    fieldErrors = emptyMap(),
                )
            }
        } else {
            _requestState.update {
                it.copy(fieldErrors = mapOf("growerSearch" to "No grower found for that TBZ ID / NRC"))
            }
        }
    }

    fun submitPermitRequest(onSubmitted: (String) -> Unit) {
        val buyerErrors = validateBuyerStep(_requestState.value.form)
        if (buyerErrors.isNotEmpty()) {
            _requestState.update { it.copy(fieldErrors = buyerErrors) }
            return
        }
        viewModelScope.launch {
            _requestState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val payloadJson = buildPermitPayload(_requestState.value.form)
                val localId = permitRepository.submitPermitRequest(payloadJson)
                resetRequestForm()
                onSubmitted(localId)
            } catch (e: Exception) {
                _requestState.update { it.copy(saveError = e.message ?: "Submit failed") }
            } finally {
                _requestState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deletePermitRequest(request: PermitRequestEntity) {
        viewModelScope.launch { permitRepository.deletePermitRequest(request) }
    }

    fun syncAllPending() {
        viewModelScope.launch { permitRepository.triggerSync() }
    }

    fun updateValidateToken(value: String) {
        _validateState.update { it.copy(permitToken = value, verifyError = null, result = null) }
    }

    fun updateValidateSalesfloor(value: String) {
        _validateState.update { it.copy(salesfloorId = value, verifyError = null, result = null) }
    }

    fun verifyPermit() {
        val state = _validateState.value
        if (state.permitToken.isBlank()) {
            _validateState.update { it.copy(verifyError = "Permit token is required") }
            return
        }
        viewModelScope.launch {
            _validateState.update { it.copy(isVerifying = true, verifyError = null) }
            try {
                val response = api.verifyQr(
                    VerifyQrRequest(
                        permit_token = state.permitToken.trim(),
                        salesfloor_id = state.salesfloorId.ifBlank { null },
                    ),
                )
                _validateState.update {
                    it.copy(
                        isVerifying = false,
                        result = VerifiedPermitInfo(
                            valid = response.valid,
                            permitNumber = response.permit_number,
                            growerName = response.grower_name,
                            tbzId = response.tbz_id,
                            totalBales = response.total_bales,
                            remainingBales = response.remaining_bales,
                            status = response.status,
                        ),
                        verifyError = if (!response.valid) "Permit is not valid" else null,
                    )
                }
            } catch (e: Exception) {
                _validateState.update {
                    it.copy(isVerifying = false, verifyError = e.message ?: "Verification failed")
                }
            }
        }
    }

    fun updateReviewForm(transform: (PermitReviewForm) -> PermitReviewForm) {
        _reviewState.update { it.copy(form = transform(it.form), fieldErrors = emptyMap()) }
    }

    fun resetReviewForm() {
        _reviewState.value = PermitReviewUiState()
    }

    fun submitTransportPermitReview(localId: String, remoteId: String) {
        val errors = validateReview(_reviewState.value.form)
        if (errors.isNotEmpty()) {
            _reviewState.update { it.copy(fieldErrors = errors) }
            return
        }
        viewModelScope.launch {
            _reviewState.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                val payload = buildReviewPayload(_reviewState.value.form)
                permitRepository.queueTransportPermitReview(localId, remoteId, payload)
                resetReviewForm()
                _reviewState.update { it.copy(isSubmitting = false, submitSuccess = true) }
                permitRepository.triggerSync()
            } catch (e: Exception) {
                _reviewState.update {
                    it.copy(isSubmitting = false, submitError = e.message ?: "Review submit failed")
                }
            }
        }
    }

    private fun buildReviewPayload(form: PermitReviewForm): String {
        val payload = buildJsonObject {
            put("action", form.action)
            if (form.action == PermitReviewActions.APPROVE) {
                put("valid_from", form.validFrom)
                put("valid_to", form.validTo)
            }
            if (form.action != PermitReviewActions.APPROVE && form.reason.isNotBlank()) {
                put("reason", form.reason.trim())
            }
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun validateReview(form: PermitReviewForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        when (form.action) {
            PermitReviewActions.APPROVE -> {
                if (form.validFrom.isBlank()) errors["validFrom"] = "Valid from is required"
                if (form.validTo.isBlank()) errors["validTo"] = "Valid to is required"
            }
            PermitReviewActions.REJECT, PermitReviewActions.RETURN -> {
                if (form.reason.isBlank()) errors["reason"] = "Reason is required"
            }
        }
        return errors
    }

    private fun buildPermitPayload(form: PermitRequestForm): String {
        val payload = buildJsonObject {
            put("grower", form.growerId)
            put("grower_category", form.growerCategory)
            put("total_bales", form.totalBales.toIntOrNull() ?: 0)
            put("total_weight_kg", form.totalWeightKg.toDoubleOrNull() ?: 0.0)
            put("license_plate", form.licensePlate.trim().uppercase())
            put("origin_province", form.originProvince)
            put("origin_district", form.originDistrict)
            put("destination_sales_floor", form.destinationSalesFloor)
            put("purpose", form.purpose)
            if (form.buyerId.isNotBlank()) put("buyer", form.buyerId)
            put("is_bought", form.isBought)
            put("buyer_accepted", form.buyerAccepted)
            put("comments", form.comments.trim())
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun validateGrowerStep(form: PermitRequestForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.growerId.isBlank()) errors["growerId"] = "Select or look up a grower"
        if (form.growerCategory.isBlank()) errors["growerCategory"] = "Grower category is required"
        return errors
    }

    private fun validateMovementStep(form: PermitRequestForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val bales = form.totalBales.toIntOrNull()
        if (bales == null || bales < 1) errors["totalBales"] = "Total bales must be at least 1"
        val weight = form.totalWeightKg.toDoubleOrNull()
        if (weight == null || weight <= 0.0) errors["totalWeightKg"] = "Total weight must be greater than 0"
        if (form.licensePlate.isBlank()) errors["licensePlate"] = "License plate is required"
        if (form.originProvince.isBlank()) errors["originProvince"] = "Origin province is required"
        if (form.originDistrict.isBlank()) errors["originDistrict"] = "Origin district is required"
        if (form.destinationSalesFloor.isBlank()) errors["destinationSalesFloor"] = "Destination sales floor is required"
        if (form.purpose.isBlank()) errors["purpose"] = "Purpose is required"
        return errors
    }

    private fun validateBuyerStep(form: PermitRequestForm): Map<String, String> {
        val errors = (validateMovementStep(form) + validateGrowerStep(form)).toMutableMap()
        if (form.isBought && form.buyerId.isBlank()) {
            errors["buyerId"] = "Buyer is required when tobacco is already bought"
        }
        return errors
    }

    fun loadCorrectionFromPermit(permit: TransportPermitEntity) {
        _correctionState.value = PermitCorrectionUiState(
            form = PermitCorrectionForm(
                growerId = permit.grower_id,
                growerCategory = permit.grower_category,
                totalBales = permit.total_bales.toString(),
                totalWeightKg = permit.total_weight_kg.toString(),
                licensePlate = permit.license_plate,
                originProvince = permit.origin_province,
                originDistrict = permit.origin_district,
                destinationSalesFloor = permit.destination_sales_floor,
                purpose = permit.purpose,
                buyerId = permit.buyer_id.orEmpty(),
                isBought = permit.is_bought,
                buyerAccepted = permit.buyer_accepted,
                comments = permit.comments.orEmpty(),
            ),
        )
    }

    fun updateCorrectionForm(transform: (PermitCorrectionForm) -> PermitCorrectionForm) {
        _correctionState.update { it.copy(form = transform(it.form), fieldErrors = emptyMap()) }
    }

    fun saveTransportPermitCorrection(localId: String, remoteId: String, onSaved: () -> Unit) {
        val errors = validateCorrectionForm(_correctionState.value.form)
        if (errors.isNotEmpty()) {
            _correctionState.update { it.copy(fieldErrors = errors) }
            return
        }
        viewModelScope.launch {
            _correctionState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val payload = buildCorrectionPayload(_correctionState.value.form)
                permitRepository.patchTransportPermit(localId, remoteId, payload)
                _correctionState.update { it.copy(isSaving = false, saveSuccess = true) }
                permitRepository.triggerSync()
                onSaved()
            } catch (e: Exception) {
                _correctionState.update {
                    it.copy(isSaving = false, saveError = e.message ?: "Save failed")
                }
            }
        }
    }

    fun resubmitTransportPermitCorrection(localId: String, remoteId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _correctionState.update { it.copy(isSaving = true, saveError = null) }
            try {
                permitRepository.resubmitTransportPermitCorrections(localId, remoteId)
                _correctionState.update { it.copy(isSaving = false, saveSuccess = true) }
                permitRepository.triggerSync()
                onDone()
            } catch (e: Exception) {
                _correctionState.update {
                    it.copy(isSaving = false, saveError = e.message ?: "Resubmit failed")
                }
            }
        }
    }

    private fun buildCorrectionPayload(form: PermitCorrectionForm): String {
        val payload = buildJsonObject {
            put("grower", form.growerId)
            put("grower_category", form.growerCategory)
            put("total_bales", form.totalBales.toIntOrNull() ?: 0)
            put("total_weight_kg", form.totalWeightKg.toDoubleOrNull() ?: 0.0)
            put("license_plate", form.licensePlate.trim().uppercase())
            put("origin_province", form.originProvince)
            put("origin_district", form.originDistrict)
            put("destination_sales_floor", form.destinationSalesFloor)
            put("purpose", form.purpose)
            if (form.buyerId.isNotBlank()) put("buyer", form.buyerId)
            put("is_bought", form.isBought)
            put("buyer_accepted", form.buyerAccepted)
            put("comments", form.comments.trim())
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun validateCorrectionForm(form: PermitCorrectionForm): Map<String, String> {
        val requestForm = PermitRequestForm(
            growerId = form.growerId,
            growerCategory = form.growerCategory,
            totalBales = form.totalBales,
            totalWeightKg = form.totalWeightKg,
            licensePlate = form.licensePlate,
            originProvince = form.originProvince,
            originDistrict = form.originDistrict,
            destinationSalesFloor = form.destinationSalesFloor,
            purpose = form.purpose,
            buyerId = form.buyerId,
            isBought = form.isBought,
            buyerAccepted = form.buyerAccepted,
            comments = form.comments,
        )
        return validateBuyerStep(requestForm)
    }
}
