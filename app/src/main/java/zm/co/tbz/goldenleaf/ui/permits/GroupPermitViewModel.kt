package zm.co.tbz.goldenleaf.ui.permits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import zm.co.tbz.goldenleaf.core.rbac.AccessControlService
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitDraftEntity
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitEntity
import zm.co.tbz.goldenleaf.data.remote.dto.GroupPermitEntryDto
import zm.co.tbz.goldenleaf.data.repository.GrowerRepository
import zm.co.tbz.goldenleaf.data.repository.PermitRepository
import zm.co.tbz.goldenleaf.data.repository.ReferenceRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupPermitViewModel @Inject constructor(
    private val permitRepository: PermitRepository,
    private val growerRepository: GrowerRepository,
    private val referenceRepository: ReferenceRepository,
    private val accessControl: AccessControlService,
    private val json: Json,
) : ViewModel() {

    private val _createState = MutableStateFlow(GroupPermitCreateUiState())
    val createState: StateFlow<GroupPermitCreateUiState> = _createState.asStateFlow()

    private val _validateState = MutableStateFlow(GroupPermitValidateState())
    val validateState: StateFlow<GroupPermitValidateState> = _validateState.asStateFlow()

    private val _reviewState = MutableStateFlow(PermitReviewUiState())
    val reviewState: StateFlow<PermitReviewUiState> = _reviewState.asStateFlow()

    private val _statusFilter = MutableStateFlow("All")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    val canApprovePermit = MutableStateFlow(false)

    val groupPermits = permitRepository.observeGroupPermits().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val groupDrafts = permitRepository.observeGroupPermitDrafts().stateIn(
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

    val hubStats = combine(groupPermits, groupDrafts) { permits, drafts ->
        GroupPermitsHubStats(
            total = permits.size,
            pending = permits.count { it.status == "PENDING" },
            approved = permits.count { it.status == "APPROVED" },
            returned = permits.count { it.status == "RETURNED_FOR_CORRECTION" },
            drafts = drafts.count { it.phase == GroupPermitDraftEntity.PHASE_DRAFT },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupPermitsHubStats())

    val filteredGroupPermits = combine(groupPermits, _statusFilter) { list, filter ->
        if (filter == "All") list else list.filter { it.status == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            canApprovePermit.value = accessControl.hasPermissionAsync("permits.approve_permit")
        }
    }

    fun observeGroupPermit(localId: String): StateFlow<GroupPermitEntity?> =
        permitRepository.observeGroupPermit(localId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    fun districtsForProvince(provinceId: String) =
        if (provinceId.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
        else referenceRepository.observeDistricts(provinceId)

    fun onStatusFilterChange(value: String) {
        _statusFilter.value = value
    }

    fun updateHeader(transform: (GroupPermitHeaderForm) -> GroupPermitHeaderForm) {
        _createState.update { it.copy(header = transform(it.header), fieldErrors = emptyMap()) }
    }

    fun goToEntriesStep(): Boolean {
        val errors = validateHeader(_createState.value.header)
        if (errors.isNotEmpty()) {
            _createState.update { it.copy(fieldErrors = errors) }
            return false
        }
        _createState.update { it.copy(step = 2) }
        return true
    }

    fun goToHeaderStep() = _createState.update { it.copy(step = 1) }

    fun resetCreateForm() {
        _createState.value = GroupPermitCreateUiState()
    }

    fun lookupGrowerByQuery(query: String): Pair<String, String>? {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return null
        val match = growers.value.firstOrNull {
            it.tbz_id?.equals(trimmed, ignoreCase = true) == true ||
                it.nrc_number.equals(trimmed, ignoreCase = true)
        } ?: return null
        _createState.update { it.copy(fieldErrors = emptyMap()) }
        return (match.remote_id ?: match.local_id) to "${match.first_name} ${match.last_name}"
    }

    fun markGrowerLookupFailed() {
        _createState.update {
            it.copy(fieldErrors = mapOf("growerSearch" to "No grower found for that TBZ ID / NRC"))
        }
    }

    fun addEntryFromForm(form: GroupPermitEntryForm): Boolean {
        val errors = validateEntry(form)
        if (errors.isNotEmpty()) {
            _createState.update { it.copy(fieldErrors = errors) }
            return false
        }
        val entry = form.copy(localKey = UUID.randomUUID().toString(), isExisting = false)
        _createState.update {
            it.copy(entries = it.entries + entry, fieldErrors = emptyMap())
        }
        return true
    }

    fun removeEntry(localKey: String) {
        _createState.update { it.copy(entries = it.entries.filter { e -> e.localKey != localKey }) }
    }

    fun submitGroupPermit(onSubmitted: () -> Unit) {
        val state = _createState.value
        if (state.entries.size < 2) {
            _createState.update {
                it.copy(saveError = "Group permit requires at least 2 growers")
            }
            return
        }
        viewModelScope.launch {
            _createState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val headerJson = buildHeaderJson(state.header)
                val entriesJson = json.encodeToString(
                    ListSerializer(JsonObject.serializer()),
                    state.entries.map { buildEntryJson(it) },
                )
                val draftId = permitRepository.saveGroupPermitDraft(
                    localId = state.draftLocalId,
                    headerJson = headerJson,
                    entriesJson = entriesJson,
                )
                val result = permitRepository.submitGroupPermitDraft(draftId)
                result.fold(
                    onSuccess = {
                        resetCreateForm()
                        onSubmitted()
                    },
                    onFailure = { e ->
                        _createState.update { it.copy(saveError = e.message ?: "Submit failed") }
                    },
                )
            } catch (e: Exception) {
                _createState.update { it.copy(saveError = e.message ?: "Submit failed") }
            } finally {
                _createState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun updateValidateToken(value: String) {
        _validateState.update { it.copy(permitToken = value, verifyError = null, result = null) }
    }

    fun validateGroupPermit() {
        val token = _validateState.value.permitToken.trim()
        if (token.isBlank()) {
            _validateState.update { it.copy(verifyError = "Permit token is required") }
            return
        }
        viewModelScope.launch {
            _validateState.update { it.copy(isVerifying = true, verifyError = null) }
            try {
                val response = permitRepository.validateGroupPermitQr(token)
                _validateState.update {
                    it.copy(
                        isVerifying = false,
                        result = GroupPermitValidateResult(
                            valid = response.valid,
                            permitNumber = response.group_permit_number,
                            licensePlate = response.license_plate,
                            destination = response.destination_sales_floor,
                            purpose = response.purpose,
                            totalBales = response.total_bales,
                            totalWeightKg = response.total_weight_kg,
                            validFrom = response.valid_from,
                            validTo = response.valid_to,
                            status = response.status,
                            entries = response.entries.orEmpty(),
                            detail = response.detail,
                        ),
                        verifyError = if (!response.valid) {
                            response.detail ?: "Group permit is not valid"
                        } else {
                            null
                        },
                    )
                }
            } catch (e: Exception) {
                _validateState.update {
                    it.copy(isVerifying = false, verifyError = e.message ?: "Validation failed")
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

    fun submitGroupPermitReview(localId: String, remoteId: String) {
        val errors = validateReview(_reviewState.value.form)
        if (errors.isNotEmpty()) {
            _reviewState.update { it.copy(fieldErrors = errors) }
            return
        }
        viewModelScope.launch {
            _reviewState.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                val payload = buildReviewPayload(_reviewState.value.form)
                permitRepository.queueGroupPermitReview(localId, remoteId, payload)
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

    fun resubmitGroupCorrections(localId: String, remoteId: String) {
        viewModelScope.launch {
            permitRepository.resubmitGroupPermitCorrections(localId, remoteId)
            permitRepository.triggerSync()
        }
    }

    fun loadCorrectionFromPermit(permit: GroupPermitEntity) {
        val entries = parseCachedEntries(permit.entries_json)
        _createState.value = GroupPermitCreateUiState(
            step = 1,
            header = GroupPermitHeaderForm(
                licensePlate = permit.license_plate,
                originProvince = permit.origin_province,
                originDistrict = permit.origin_district,
                destinationSalesFloor = permit.destination_sales_floor,
                purpose = permit.purpose.ifBlank { "SALES" },
                comments = permit.comments.orEmpty(),
            ),
            entries = entries,
        )
    }

    fun saveGroupPermitCorrection(localId: String, remoteId: String, onSaved: () -> Unit) {
        val state = _createState.value
        val headerErrors = validateHeader(state.header)
        if (headerErrors.isNotEmpty()) {
            _createState.update { it.copy(fieldErrors = headerErrors) }
            return
        }
        if (state.entries.size < 2) {
            _createState.update { it.copy(saveError = "Group permit requires at least 2 growers") }
            return
        }
        viewModelScope.launch {
            _createState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val headerJson = buildHeaderJson(state.header)
                permitRepository.patchGroupPermitHeader(localId, remoteId, headerJson)
                state.entries.filter { !it.isExisting }.forEach { entry ->
                    val entryJson = json.encodeToString(JsonObject.serializer(), buildEntryJson(entry))
                    permitRepository.queueGroupPermitEntry(remoteId, entryJson)
                }
                permitRepository.triggerSync()
                _createState.update { it.copy(isSaving = false, saveError = null) }
                onSaved()
            } catch (e: Exception) {
                _createState.update { it.copy(isSaving = false, saveError = e.message ?: "Save failed") }
            }
        }
    }

    private fun parseCachedEntries(entriesJson: String?): List<GroupPermitEntryForm> {
        if (entriesJson.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(GroupPermitEntryDto.serializer()), entriesJson)
        }.getOrDefault(emptyList()).map { dto ->
            GroupPermitEntryForm(
                localKey = dto.id,
                growerLabel = dto.grower_name.orEmpty(),
                growerCategory = dto.grower_category ?: "SMALL_SCALE",
                totalBales = dto.total_bales.toString(),
                totalWeightKg = dto.total_weight_kg.toString(),
                isExisting = true,
            )
        }
    }

    fun syncAll() {
        viewModelScope.launch { permitRepository.triggerSync() }
    }

    private fun buildHeaderJson(header: GroupPermitHeaderForm): String {
        val payload = buildJsonObject {
            put("license_plate", header.licensePlate.trim().uppercase())
            put("origin_province", header.originProvince)
            put("origin_district", header.originDistrict)
            put("destination_sales_floor", header.destinationSalesFloor)
            put("purpose", header.purpose)
            put("comments", header.comments.trim())
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun buildEntryJson(entry: GroupPermitEntryForm): JsonObject = buildJsonObject {
        put("grower", entry.growerId)
        put("grower_category", entry.growerCategory)
        put("total_bales", entry.totalBales.toIntOrNull() ?: 0)
        put("total_weight_kg", entry.totalWeightKg.toDoubleOrNull() ?: 0.0)
        put("notes", entry.notes.trim())
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

    private fun validateHeader(header: GroupPermitHeaderForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (header.licensePlate.isBlank()) errors["licensePlate"] = "License plate is required"
        if (header.originProvince.isBlank()) errors["originProvince"] = "Origin province is required"
        if (header.originDistrict.isBlank()) errors["originDistrict"] = "Origin district is required"
        if (header.destinationSalesFloor.isBlank()) {
            errors["destinationSalesFloor"] = "Destination sales floor is required"
        }
        if (header.purpose.isBlank()) errors["purpose"] = "Purpose is required"
        return errors
    }

    private fun validateEntry(entry: GroupPermitEntryForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (entry.growerId.isBlank()) errors["growerId"] = "Select or look up a grower"
        val bales = entry.totalBales.toIntOrNull()
        if (bales == null || bales < 1) errors["totalBales"] = "Total bales must be at least 1"
        val weight = entry.totalWeightKg.toDoubleOrNull()
        if (weight == null || weight <= 0.0) errors["totalWeightKg"] = "Total weight must be greater than 0"
        return errors
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
}
