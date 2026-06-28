package zm.co.tbz.goldenleaf.ui.inspection

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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.putJsonArray
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.InspectionEntity
import zm.co.tbz.goldenleaf.data.local.entity.InspectionReportEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.local.entity.ValidationEntity
import zm.co.tbz.goldenleaf.data.repository.GrowerRepository
import zm.co.tbz.goldenleaf.data.repository.InspectionRepository
import zm.co.tbz.goldenleaf.data.repository.ProfileRepository
import zm.co.tbz.goldenleaf.data.repository.ReferenceRepository
import zm.co.tbz.goldenleaf.ui.registration.calculateYieldPerHa
import javax.inject.Inject

data class InspectionHubStats(
    val scheduled: Int = 0,
    val inProgress: Int = 0,
    val completed: Int = 0,
    val highRisk: Int = 0,
    val schedulesPendingSync: Int = 0,
    val reportsPendingSync: Int = 0,
)

data class HighRiskGrowerRow(
    val validationLocalId: String,
    val growerId: String,
    val growerName: String,
    val nrcNumber: String,
    val province: String,
    val district: String,
    val riskScore: Int,
)

data class InspectionUiState(
    val schedule: ScheduleInspectionForm = ScheduleInspectionForm(),
    val scheduleErrors: Map<String, String> = emptyMap(),
    val field: FieldInspectionForm = FieldInspectionForm(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val nursery: NurseryInspectionForm = NurseryInspectionForm(),
    val nurseryErrors: Map<String, String> = emptyMap(),
    val curing: CuringInspectionForm = CuringInspectionForm(),
    val curingErrors: Map<String, String> = emptyMap(),
    val validation: ValidationForm = ValidationForm(),
    val validationErrors: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val typeFilter: String = "All",
    val statusFilter: String = "All",
    val syncFilter: String = "All",
    val highRiskSearch: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
class InspectionViewModel @Inject constructor(
    private val inspectionRepository: InspectionRepository,
    private val growerRepository: GrowerRepository,
    private val referenceRepository: ReferenceRepository,
    private val profileRepository: ProfileRepository,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionUiState())
    val uiState: StateFlow<InspectionUiState> = _uiState.asStateFlow()

    val inspections = inspectionRepository.observeInspections().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val reports = inspectionRepository.observeReports().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val validations = inspectionRepository.observeValidations().stateIn(
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

    val tobaccoTypes = referenceRepository.observeTobaccoTypes().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val barnTypes = referenceRepository.observeBarnTypes().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val sponsors = referenceRepository.observeSponsors().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val profile = profileRepository.profile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val hubStats = combine(inspections, reports, validations) { inspectionList, reportList, validationList ->
        InspectionHubStats(
            scheduled = inspectionList.count { it.status == "SCHEDULED" },
            inProgress = inspectionList.count { it.status == "IN_PROGRESS" },
            completed = inspectionList.count { it.status == "COMPLETED" },
            highRisk = validationList.count { (it.risk_score ?: 0) >= 70 },
            schedulesPendingSync = inspectionList.count { it.sync_status == SyncStatuses.PENDING },
            reportsPendingSync = reportList.count { it.sync_status == SyncStatuses.PENDING },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InspectionHubStats())

    val highRiskValidations = combine(validations, growers, _uiState) { validationList, growerList, state ->
        validationList
            .filter { (it.risk_score ?: 0) >= 70 }
            .mapNotNull { validation ->
                val grower = growerList.find {
                    it.remote_id == validation.grower_id || it.local_id == validation.grower_id
                }
                HighRiskGrowerRow(
                    validationLocalId = validation.local_id,
                    growerId = validation.grower_id,
                    growerName = grower?.let { "${it.first_name} ${it.last_name}".trim() } ?: validation.grower_id,
                    nrcNumber = grower?.nrc_number.orEmpty(),
                    province = grower?.province.orEmpty(),
                    district = grower?.district.orEmpty(),
                    riskScore = validation.risk_score ?: 0,
                )
            }
            .filter { row ->
                state.highRiskSearch.isBlank() ||
                    row.growerName.contains(state.highRiskSearch, ignoreCase = true) ||
                    row.nrcNumber.contains(state.highRiskSearch, ignoreCase = true)
            }
            .sortedByDescending { it.riskScore }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredInspections = combine(inspections, _uiState) { list, state ->
        list.filter { inspection ->
            val matchesSearch = state.searchQuery.isBlank() ||
                inspection.grower_name?.contains(state.searchQuery, ignoreCase = true) == true ||
                inspection.province?.contains(state.searchQuery, ignoreCase = true) == true ||
                inspection.district?.contains(state.searchQuery, ignoreCase = true) == true
            val matchesType = state.typeFilter == "All" || inspection.inspection_type == state.typeFilter
            val matchesStatus = state.statusFilter == "All" || inspection.status == state.statusFilter
            matchesSearch && matchesType && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val localSchedules = combine(inspections, _uiState) { list, state ->
        list.filter { inspection ->
            state.syncFilter == "All" || inspection.sync_status == state.syncFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredReports = combine(reports, _uiState) { list, state ->
        list.filter { report ->
            state.syncFilter == "All" || report.sync_status == state.syncFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val growerSearchResults = combine(growers, _uiState) { list, state ->
        val query = state.schedule.growerSearch.ifBlank { state.searchQuery }
        if (query.isBlank()) emptyList() else list.filter { growerMatchesQuery(it, query) }.take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun districtsForProvince(provinceId: String) =
        if (provinceId.isBlank()) flowOf(emptyList()) else referenceRepository.observeDistricts(provinceId)

    fun observeInspection(localId: String): StateFlow<InspectionEntity?> =
        inspections.map { list -> list.firstOrNull { it.local_id == localId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onSearchChange(value: String) = _uiState.update { it.copy(searchQuery = value) }
    fun onTypeFilterChange(value: String) = _uiState.update { it.copy(typeFilter = value) }
    fun onStatusFilterChange(value: String) = _uiState.update { it.copy(statusFilter = value) }
    fun onSyncFilterChange(value: String) = _uiState.update { it.copy(syncFilter = value) }
    fun onHighRiskSearchChange(value: String) = _uiState.update { it.copy(highRiskSearch = value) }

    fun updateSchedule(transform: (ScheduleInspectionForm) -> ScheduleInspectionForm) {
        _uiState.update { it.copy(schedule = transform(it.schedule), scheduleErrors = emptyMap()) }
    }

    fun updateField(transform: (FieldInspectionForm) -> FieldInspectionForm) {
        _uiState.update { it.copy(field = transform(it.field), fieldErrors = emptyMap()) }
    }

    fun updateNursery(transform: (NurseryInspectionForm) -> NurseryInspectionForm) {
        _uiState.update { it.copy(nursery = transform(it.nursery), nurseryErrors = emptyMap()) }
    }

    fun updateCuring(transform: (CuringInspectionForm) -> CuringInspectionForm) {
        _uiState.update { it.copy(curing = transform(it.curing), curingErrors = emptyMap()) }
    }

    fun updateValidation(transform: (ValidationForm) -> ValidationForm) {
        _uiState.update { it.copy(validation = transform(it.validation), validationErrors = emptyMap()) }
    }

    fun onValidatedHectarageChange(value: String) {
        val yield = value.toDoubleOrNull()?.let { calculateYieldPerHa(it).toString() }.orEmpty()
        updateValidation { it.copy(validatedHectarage = value, yieldPerHa = yield) }
    }

    fun toggleStakeholder(id: String) {
        updateValidation { form ->
            val next = form.stakeholdersPresent.toMutableSet()
            if (next.contains(id)) next.remove(id) else next.add(id)
            form.copy(stakeholdersPresent = next)
        }
    }

    fun initScheduleForm() {
        val user = profile.value
        _uiState.update {
            it.copy(
                schedule = ScheduleInspectionForm(
                    inspectorId = user?.id.orEmpty(),
                    inspectorName = user?.full_name.orEmpty(),
                    scheduledDate = java.time.LocalDate.now().toString(),
                ),
            )
        }
    }

    fun selectGrowerForSchedule(grower: GrowerEntity) {
        updateSchedule {
            it.copy(
                growerId = grower.remote_id ?: grower.local_id,
                growerName = "${grower.first_name} ${grower.last_name}".trim(),
                growerSearch = grower.tbz_id ?: grower.nrc_number,
                provinceId = grower.province.orEmpty(),
                districtId = grower.district.orEmpty(),
            )
        }
    }

    fun clearScheduleGrower() {
        updateSchedule {
            it.copy(growerId = "", growerName = "", growerSearch = "", provinceId = "", districtId = "")
        }
    }

    fun loadFormsForInspection(inspection: InspectionEntity) {
        val growerId = inspection.grower_id
        val growerName = inspection.grower_name.orEmpty()
        val inspectionId = inspection.local_id
        _uiState.update {
            it.copy(
                field = FieldInspectionForm(
                    growerId = growerId,
                    growerName = growerName,
                    inspectionLocalId = inspectionId,
                ),
                nursery = NurseryInspectionForm(
                    growerId = growerId,
                    growerName = growerName,
                    inspectionLocalId = inspectionId,
                ),
                curing = CuringInspectionForm(
                    growerId = growerId,
                    growerName = growerName,
                    inspectionLocalId = inspectionId,
                ),
                validation = ValidationForm(
                    growerId = growerId,
                    growerName = growerName,
                    inspectionLocalId = inspectionId,
                    provinceId = inspection.province.orEmpty(),
                    districtId = inspection.district.orEmpty(),
                ),
            )
        }
        viewModelScope.launch {
            val grower = growers.value.find {
                it.remote_id == growerId || it.local_id == growerId
            }
            if (grower != null) {
                updateValidation {
                    it.copy(
                        growerNrc = grower.nrc_number,
                        sex = grower.sex ?: it.sex,
                        gpsLatitude = grower.gps_latitude?.toString().orEmpty(),
                        gpsLongitude = grower.gps_longitude?.toString().orEmpty(),
                    )
                }
            }
        }
    }

    fun scheduleInspection(onScheduled: (String) -> Unit) {
        val form = _uiState.value.schedule
        val errors = validateSchedule(form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(scheduleErrors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val inspectorId = form.inspectorId.ifBlank { profile.value?.id.orEmpty() }
                val district = form.districtId.ifBlank { form.districtText.trim() }
                val id = inspectionRepository.scheduleInspection(
                    growerId = form.growerId,
                    growerName = form.growerName,
                    inspectorId = inspectorId,
                    inspectionType = form.inspectionType,
                    scheduledDate = form.scheduledDate,
                    province = form.provinceId,
                    district = district,
                    notes = form.notes.ifBlank { null },
                )
                initScheduleForm()
                onScheduled(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Schedule failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveFieldReport(onSaved: () -> Unit) {
        val form = _uiState.value.field
        val errors = validateField(form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val inspectionRemoteId = resolveInspectionRemoteId(form.inspectionLocalId)
                val payload = buildJsonObject {
                    inspectionRemoteId?.let { put("inspection", it) }
                    put("grower", form.growerId)
                    put("transplanted_hectarage", form.transplantedHectarage.toDouble())
                    put("crop_stage", form.cropStage)
                    put("plant_population", form.plantPopulation)
                    put("crop_uniformity", form.cropUniformity)
                    put("fertilizer_application", form.fertilizerApplication)
                    put("pest_disease_status", form.pestDiseaseStatus)
                    put("weed_control", form.weedControl)
                    put("irrigation_status", form.irrigationStatus)
                    form.gpsLatitude.toDoubleOrNull()?.let { put("gps_latitude", it) }
                    form.gpsLongitude.toDoubleOrNull()?.let { put("gps_longitude", it) }
                    if (form.deviceId.isNotBlank()) put("device_id", form.deviceId)
                    put("inspector_remarks", form.inspectorRemarks.trim())
                }
                inspectionRepository.saveInspectionReport(
                    type = "FIELD",
                    endpoint = "inspectorate/field-inspections/",
                    payloadJson = json.encodeToString(payload),
                    inspectionLocalId = form.inspectionLocalId.ifBlank { null },
                )
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Save failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveNurseryReport(onSaved: () -> Unit) {
        val form = _uiState.value.nursery
        val errors = validateNursery(form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(nurseryErrors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val inspectionRemoteId = resolveInspectionRemoteId(form.inspectionLocalId)
                val payload = buildJsonObject {
                    inspectionRemoteId?.let { put("inspection", it) }
                    put("grower", form.growerId)
                    put("seed_variety", form.seedVariety.trim())
                    put("nursery_size_beds", form.nurserySizeBeds.toInt())
                    put("date_of_sowing", form.dateOfSowing.trim())
                    put("germination_status", form.germinationStatus)
                    put("seedling_condition", form.seedlingCondition)
                    put("water_source", form.waterSource)
                    put("pest_disease_present", form.pestDiseasePresent)
                    if (form.pestDiseaseNotes.isNotBlank()) put("pest_disease_notes", form.pestDiseaseNotes.trim())
                    put("fertilizer_used", form.fertilizerUsed)
                    if (form.fertilizerNotes.isNotBlank()) put("fertilizer_notes", form.fertilizerNotes.trim())
                    put("chemicals_used", form.chemicalsUsed)
                    if (form.chemicalsNotes.isNotBlank()) put("chemicals_notes", form.chemicalsNotes.trim())
                    form.gpsLatitude.toDoubleOrNull()?.let { put("gps_latitude", it) }
                    form.gpsLongitude.toDoubleOrNull()?.let { put("gps_longitude", it) }
                    if (form.deviceId.isNotBlank()) put("device_id", form.deviceId)
                    put("inspector_remarks", form.inspectorRemarks.trim())
                }
                inspectionRepository.saveInspectionReport(
                    type = "NURSERY",
                    endpoint = "inspectorate/nursery-inspections/",
                    payloadJson = json.encodeToString(payload),
                    inspectionLocalId = form.inspectionLocalId.ifBlank { null },
                )
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Save failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveCuringReport(onSaved: () -> Unit) {
        val form = _uiState.value.curing
        val errors = validateCuring(form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(curingErrors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val inspectionRemoteId = resolveInspectionRemoteId(form.inspectionLocalId)
                val payload = buildJsonObject {
                    inspectionRemoteId?.let { put("inspection", it) }
                    put("grower", form.growerId)
                    put("number_of_barns", form.numberOfBarns.toInt())
                    put("barn_type", form.barnType)
                    put("curing_cycles", form.curingCycles.toInt())
                    put("fuel_source", form.fuelSource)
                    put("curing_status", form.curingStatus)
                    put("leaf_quality", form.leafQuality)
                    put("grading_status", form.gradingStatus)
                    form.gpsLatitude.toDoubleOrNull()?.let { put("gps_latitude", it) }
                    form.gpsLongitude.toDoubleOrNull()?.let { put("gps_longitude", it) }
                    if (form.deviceId.isNotBlank()) put("device_id", form.deviceId)
                    put("inspector_remarks", form.inspectorRemarks.trim())
                }
                inspectionRepository.saveInspectionReport(
                    type = "CURING",
                    endpoint = "inspectorate/curing-inspections/",
                    payloadJson = json.encodeToString(payload),
                    inspectionLocalId = form.inspectionLocalId.ifBlank { null },
                )
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Save failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveValidation(onSaved: () -> Unit) {
        val form = _uiState.value.validation
        val errors = validateValidation(form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val inspectionRemoteId = resolveInspectionRemoteId(form.inspectionLocalId)
                val district = form.districtId.ifBlank { form.districtText.trim() }
                val payload = buildJsonObject {
                    inspectionRemoteId?.let { put("inspection", it) }
                    put("grower", form.growerId)
                    if (form.cropAllocationId.isNotBlank()) put("crop_allocation", form.cropAllocationId)
                    if (form.growerNrc.isNotBlank()) put("nrc_number", form.growerNrc.trim().uppercase())
                    put("sex", form.sex)
                    put("gps_latitude", form.gpsLatitude.toDouble())
                    put("gps_longitude", form.gpsLongitude.toDouble())
                    put("crop_stage", form.cropStage)
                    put("tobacco_type", form.tobaccoType)
                    put("tobacco_variety", form.tobaccoVariety.trim())
                    put("validated_hectarage", form.validatedHectarage.toDouble())
                    put("yield_per_hectare", form.yieldPerHa.toDouble())
                    if (form.sponsorId.isNotBlank()) put("sponsor", form.sponsorId)
                    put("barn_type", form.barnType)
                    put("number_of_barns", form.numberOfBarns.toInt())
                    put("barn_capacity_sufficient", form.barnCapacitySufficient)
                    if (form.stakeholdersPresent.isNotEmpty()) {
                        putJsonArray("stakeholders_present") {
                            form.stakeholdersPresent.forEach { add(JsonPrimitive(it)) }
                        }
                    }
                    put("province", form.provinceId)
                    put("district", district)
                    if (form.deviceId.isNotBlank()) put("device_id", form.deviceId)
                    if (form.inspectorRemarks.isNotBlank()) put("inspector_remarks", form.inspectorRemarks.trim())
                }
                inspectionRepository.saveInspectionReport(
                    type = "VALIDATION",
                    endpoint = "inspectorate/validations/",
                    payloadJson = json.encodeToString(payload),
                    inspectionLocalId = form.inspectionLocalId.ifBlank { null },
                )
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Save failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteReport(report: InspectionReportEntity) {
        viewModelScope.launch { inspectionRepository.deleteReport(report) }
    }

    fun triggerSync() {
        viewModelScope.launch { inspectionRepository.triggerSync() }
    }

    private fun resolveInspectionRemoteId(localId: String): String? {
        if (localId.isBlank()) return null
        return inspections.value.firstOrNull { it.local_id == localId }?.remote_id
    }

    private fun growerMatchesQuery(grower: GrowerEntity, query: String): Boolean {
        val q = query.trim()
        return grower.first_name.contains(q, ignoreCase = true) ||
            grower.last_name.contains(q, ignoreCase = true) ||
            grower.nrc_number.contains(q, ignoreCase = true) ||
            grower.tbz_id?.contains(q, ignoreCase = true) == true
    }

    private fun validateSchedule(form: ScheduleInspectionForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.growerId.isBlank()) errors["growerId"] = "Select a grower"
        if (form.inspectionType.isBlank()) errors["inspectionType"] = "Inspection type is required"
        if (form.scheduledDate.isBlank()) errors["scheduledDate"] = "Scheduled date is required"
        if (form.provinceId.isBlank()) errors["provinceId"] = "Province is required"
        if (form.districtId.isBlank() && form.districtText.isBlank()) {
            errors["districtId"] = "District is required"
        }
        return errors
    }

    private fun validateField(form: FieldInspectionForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.growerId.isBlank()) errors["growerId"] = "Grower is required"
        val ha = form.transplantedHectarage.toDoubleOrNull()
        if (ha == null || ha <= 0) errors["transplantedHectarage"] = "Enter valid transplanted hectarage"
        if (form.inspectorRemarks.isBlank()) errors["inspectorRemarks"] = "Inspector remarks are required"
        return errors
    }

    private fun validateNursery(form: NurseryInspectionForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.seedVariety.isBlank()) errors["seedVariety"] = "Seed variety is required"
        val beds = form.nurserySizeBeds.toIntOrNull()
        if (beds == null || beds <= 0) errors["nurserySizeBeds"] = "Enter valid nursery bed count"
        if (form.dateOfSowing.isBlank()) errors["dateOfSowing"] = "Date of sowing is required"
        if (form.inspectorRemarks.isBlank()) errors["inspectorRemarks"] = "Inspector remarks are required"
        return errors
    }

    private fun validateCuring(form: CuringInspectionForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val barns = form.numberOfBarns.toIntOrNull()
        if (barns == null || barns !in 1..20) errors["numberOfBarns"] = "Barns must be 1–20"
        val cycles = form.curingCycles.toIntOrNull()
        if (cycles == null || cycles <= 0) errors["curingCycles"] = "Enter valid curing cycles"
        if (form.inspectorRemarks.isBlank()) errors["inspectorRemarks"] = "Inspector remarks are required"
        return errors
    }

    private fun validateValidation(form: ValidationForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.growerId.isBlank()) errors["growerId"] = "Grower is required"
        form.gpsLatitude.toDoubleOrNull() ?: run { errors["gpsLatitude"] = "GPS latitude is required" }
        form.gpsLongitude.toDoubleOrNull() ?: run { errors["gpsLongitude"] = "GPS longitude is required" }
        if (form.tobaccoVariety.isBlank()) errors["tobaccoVariety"] = "Tobacco variety is required"
        val ha = form.validatedHectarage.toDoubleOrNull()
        if (ha == null || ha < 0.5 || ha > 300) errors["validatedHectarage"] = "Hectarage must be 0.5–300"
        val yield = form.yieldPerHa.toDoubleOrNull()
        if (yield == null || yield <= 0) errors["yieldPerHa"] = "Yield per ha is required"
        val barns = form.numberOfBarns.toIntOrNull()
        if (barns == null || barns !in 1..20) errors["numberOfBarns"] = "Barns must be 1–20"
        if (form.provinceId.isBlank()) errors["provinceId"] = "Province is required"
        if (form.districtId.isBlank() && form.districtText.isBlank()) {
            errors["districtId"] = "District is required"
        }
        return errors
    }
}
