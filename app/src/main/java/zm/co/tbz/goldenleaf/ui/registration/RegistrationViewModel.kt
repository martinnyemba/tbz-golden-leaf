package zm.co.tbz.goldenleaf.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerRegistrationEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.repository.GrowerRepository
import zm.co.tbz.goldenleaf.data.repository.ReferenceRepository
import zm.co.tbz.goldenleaf.data.repository.apiStatusForFilter
import zm.co.tbz.goldenleaf.data.repository.changedFieldLabels
import zm.co.tbz.goldenleaf.data.repository.formatEditTimestamp
import zm.co.tbz.goldenleaf.data.repository.growerTypeFromWizardKey
import zm.co.tbz.goldenleaf.data.sync.SyncCoordinator
import zm.co.tbz.goldenleaf.ui.components.GlTone

data class RegistrationHubStats(
    val totalGrowers: Int = 0,
    val pendingSync: Int = 0,
    val synced: Int = 0,
    val failed: Int = 0,
)

data class RegistrationUiState(
    val personal: PersonalDetailsForm = PersonalDetailsForm(),
    val crop: CropDetailsForm = CropDetailsForm(),
    val personalErrors: Map<String, String> = emptyMap(),
    val cropErrors: Map<String, String> = emptyMap(),
    val registrationStep: Int = 1,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val searchQuery: String = "",
    val statusFilter: String = "All",
    val syncFilter: String = "All",
    val updateQueueFilter: String = "All",
    val lastRegisteredLocalId: String? = null,
    val correctionDraftSaved: Boolean = false,
    val isLoadingGrowers: Boolean = false,
    val growersLoadError: String? = null,
    val lastRegisteredName: String? = null,
)

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val growerRepository: GrowerRepository,
    private val referenceRepository: ReferenceRepository,
    private val syncCoordinator: SyncCoordinator,
    private val json: Json,
) : ViewModel() {

    init {
        syncCoordinator.scheduleReferenceRefresh()
        syncCoordinator.scheduleDeltaDownload()
        refreshGrowers()
    }

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    val growers = growerRepository.observeGrowers().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val registrations = growerRepository.observeRegistrations().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val edits = growerRepository.observeEdits().stateIn(
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

    val sponsors = referenceRepository.observeSponsors().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val barnTypes = referenceRepository.observeBarnTypes().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val hubStats = combine(growers, registrations) { growerList, regList ->
        RegistrationHubStats(
            totalGrowers = growerList.size,
            pendingSync = growerList.count { it.sync_status == SyncStatuses.PENDING } +
                regList.count { it.sync_status == SyncStatuses.PENDING },
            synced = growerList.count { it.sync_status == SyncStatuses.SYNCED },
            failed = growerList.count { it.sync_status == SyncStatuses.FAILED },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistrationHubStats())

    val filteredGrowers = combine(growers, _uiState) { list, state ->
        list.filter { grower ->
            val matchesSearch = state.searchQuery.isBlank() ||
                grower.first_name.contains(state.searchQuery, ignoreCase = true) ||
                grower.last_name.contains(state.searchQuery, ignoreCase = true) ||
                grower.middle_name?.contains(state.searchQuery, ignoreCase = true) == true ||
                grower.nrc_number.contains(state.searchQuery, ignoreCase = true) ||
                grower.tbz_id?.contains(state.searchQuery, ignoreCase = true) == true ||
                grower.phone_number?.contains(state.searchQuery, ignoreCase = true) == true
            matchesSearch && matchesPortalStatusFilter(grower, state.statusFilter)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val displayedGrowers = filteredGrowers.map { list ->
        list.map { it.toListItem() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val updateQueueStats = edits.map { editList ->
        Triple(
            editList.count { it.sync_status == SyncStatuses.PENDING },
            editList.count { it.sync_status == SyncStatuses.FAILED },
            editList.count { it.sync_status == SyncStatuses.SYNCED },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Triple(0, 0, 0))

    val displayedGrowerUpdates = combine(edits, growers, _uiState) { editList, growerList, state ->
        val mapped = editList.map { edit ->
            val grower = growerList.firstOrNull { it.local_id == edit.grower_local_id }
            val name = grower?.let { listOfNotNull(it.first_name, it.last_name).joinToString(" ") }
                ?: "Grower ${edit.grower_local_id.take(8)}"
            GrowerUpdateQueueItem(
                localId = edit.local_id,
                growerLocalId = edit.grower_local_id,
                growerName = name,
                tbzId = grower?.tbz_id ?: grower?.nrc_number ?: edit.grower_local_id.take(12),
                changedFields = changedFieldLabels(edit.patch_json, json),
                status = edit.sync_status,
                timestamp = formatEditTimestamp(edit.created_at),
                error = edit.last_sync_error,
            )
        }
        if (state.updateQueueFilter == "All") mapped
        else mapped.filter {
            it.status.equals(state.updateQueueFilter.replace(" ", "_"), ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredRegistrations = combine(registrations, _uiState) { list, state ->
        list.filter { reg ->
            state.syncFilter == "All" || reg.sync_status == state.syncFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val corrections = combine(growers, _uiState) { list, _ ->
        list.filter {
            it.status == "RETURNED_FOR_CORRECTION" ||
                it.sync_status == SyncStatuses.NEEDS_REVIEW
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun districtsForProvince(provinceId: String) =
        if (provinceId.isBlank()) {
            flowOf(emptyList())
        } else {
            referenceRepository.observeDistricts(provinceId)
        }

    fun observeGrower(localId: String): StateFlow<GrowerEntity?> =
        growerRepository.observeGrowerById(localId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    fun observeRegistration(localId: String): StateFlow<GrowerRegistrationEntity?> =
        growerRepository.observeRegistrationById(localId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    fun onSearchChange(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
        refreshGrowers()
    }

    fun onStatusFilterChange(value: String) {
        _uiState.update { it.copy(statusFilter = value) }
        refreshGrowers()
    }
    fun onSyncFilterChange(value: String) = _uiState.update { it.copy(syncFilter = value) }
    fun onUpdateQueueFilterChange(value: String) = _uiState.update { it.copy(updateQueueFilter = value) }

    fun applyGrowerTypeKey(key: String) {
        val (category, growerType) = growerTypeFromWizardKey(key)
        _uiState.update {
            it.copy(
                personal = it.personal.copy(
                    growerTypeKey = key,
                    category = category,
                    growerType = growerType,
                ),
            )
        }
    }

    fun refreshGrowers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGrowers = true, growersLoadError = null) }
            try {
                growerRepository.refreshGrowersFromServer(
                    search = _uiState.value.searchQuery.takeIf { it.isNotBlank() },
                    status = apiStatusForFilter(_uiState.value.statusFilter),
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(growersLoadError = e.message ?: "Could not load growers") }
            } finally {
                _uiState.update { it.copy(isLoadingGrowers = false) }
            }
        }
    }

    fun syncCropFromWizardFields() {
        val personal = _uiState.value.personal
        val types = tobaccoTypes.value
        val barns = barnTypes.value
        val tobaccoId = types.firstOrNull { it.name.equals(personal.tobaccoTypeName, ignoreCase = true) }?.id
            ?: types.firstOrNull { personal.tobaccoTypeName.isBlank() }?.id
            ?: types.firstOrNull()?.id.orEmpty()
        val barnId = barns.firstOrNull {
            it.name.contains(personal.curingStructure, ignoreCase = true) ||
                personal.curingStructure.contains(it.name, ignoreCase = true)
        }?.id ?: barns.firstOrNull()?.id.orEmpty()
        val hectarage = personal.tobaccoAreaHa.ifBlank { personal.totalAreaHa }
        updateCrop { crop ->
            crop.copy(
                tobaccoTypeId = tobaccoId,
                hectarage = hectarage,
                barnTypeId = barnId,
                gpsLatitude = personal.gpsLatitude,
                gpsLongitude = personal.gpsLongitude,
                numberOfBarns = crop.numberOfBarns.ifBlank { "1" },
                stringsPerBarn = crop.stringsPerBarn.ifBlank { "1" },
                isSelfSponsored = crop.sponsorId.isNullOrBlank(),
            )
        }
    }

    fun continueFromIdentity(onSuccess: () -> Unit): Boolean {
        val errors = validateWizardIdentity(_uiState.value.personal)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(personalErrors = errors) }
            return false
        }
        onSuccess()
        return true
    }

    fun continueFromFarm(onSuccess: () -> Unit): Boolean {
        syncCropFromWizardFields()
        val personalErrors = validateWizardFarmLocation(_uiState.value.personal)
        val cropErrors = validateCrop(_uiState.value.crop)
        val errors = personalErrors + cropErrors
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    personalErrors = personalErrors,
                    cropErrors = cropErrors,
                )
            }
            return false
        }
        onSuccess()
        return true
    }

    fun saveCorrectionDraft(growerLocalId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                growerRepository.saveGrowerEdit(growerLocalId, _uiState.value.personal)
                setCorrectionDraftSaved(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Save failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun syncEdit(editLocalId: String) {
        viewModelScope.launch { growerRepository.syncEdit(editLocalId) }
    }

    fun deleteEdit(editLocalId: String) {
        viewModelScope.launch { growerRepository.deleteLocalEdit(editLocalId) }
    }

    fun applyScannedNrc(nrc: String) {
        _uiState.update {
            it.copy(personal = it.personal.copy(nrcNumber = nrc.trim()))
        }
    }

    fun applyScannedIdentity(nrc: String, fullName: String, gender: String, dob: String) {
        val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
        val first = parts.firstOrNull().orEmpty()
        val last = parts.drop(1).joinToString(" ")
        _uiState.update {
            it.copy(
                personal = it.personal.copy(
                    nrcNumber = nrc,
                    firstName = first,
                    lastName = last,
                    sex = if (gender.equals("F", ignoreCase = true) || gender.equals("Female", ignoreCase = true)) "FEMALE" else "MALE",
                    dateOfBirth = dob,
                ),
            )
        }
    }

    fun setCorrectionDraftSaved(saved: Boolean) = _uiState.update { it.copy(correctionDraftSaved = saved) }

    fun updatePersonal(transform: (PersonalDetailsForm) -> PersonalDetailsForm) {
        _uiState.update { it.copy(personal = transform(it.personal), personalErrors = emptyMap()) }
    }

    fun updateCrop(transform: (CropDetailsForm) -> CropDetailsForm) {
        _uiState.update { it.copy(crop = transform(it.crop), cropErrors = emptyMap()) }
    }

    fun loadPersonalFromGrower(grower: GrowerEntity) {
        _uiState.update {
            it.copy(
                personal = PersonalDetailsForm(
                    firstName = grower.first_name,
                    middleName = grower.middle_name.orEmpty(),
                    lastName = grower.last_name,
                    nrcNumber = grower.nrc_number,
                    sex = grower.sex ?: "MALE",
                    dateOfBirth = grower.date_of_birth.orEmpty(),
                    category = grower.category ?: "SMALL_SCALE",
                    localPhone = grower.phone_number.orEmpty(),
                    email = grower.email.orEmpty(),
                    address = grower.address.orEmpty(),
                    townVillage = grower.town_village.orEmpty(),
                    provinceId = grower.province.orEmpty(),
                    districtId = grower.district.orEmpty(),
                    gpsLatitude = grower.gps_latitude?.toString().orEmpty(),
                    gpsLongitude = grower.gps_longitude?.toString().orEmpty(),
                    profilePhotoPath = grower.profile_photo_path,
                    idFrontPath = grower.id_front_path,
                    idBackPath = grower.id_back_path,
                ),
            )
        }
    }

    fun goToCropStep(): Boolean {
        val errors = validatePersonal(_uiState.value.personal)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(personalErrors = errors) }
            return false
        }
        _uiState.update { it.copy(registrationStep = 2) }
        return true
    }

    fun goToPersonalStep() = _uiState.update { it.copy(registrationStep = 1) }

    val lastRegisteredId = _uiState.map { it.lastRegisteredLocalId }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    fun resetRegistrationForm() {
        val lastId = _uiState.value.lastRegisteredLocalId
        val lastName = _uiState.value.lastRegisteredName
        _uiState.update {
            it.copy(
                personal = PersonalDetailsForm(),
                crop = CropDetailsForm(),
                registrationStep = 1,
                personalErrors = emptyMap(),
                cropErrors = emptyMap(),
                saveError = null,
                lastRegisteredLocalId = lastId,
                lastRegisteredName = lastName,
            )
        }
    }

    fun submitRegistration(onCreated: (String) -> Unit) {
        syncCropFromWizardFields()
        val personalErrors = validatePersonal(_uiState.value.personal)
        val cropErrors = validateCrop(_uiState.value.crop)
        if (personalErrors.isNotEmpty() || cropErrors.isNotEmpty()) {
            _uiState.update { it.copy(personalErrors = personalErrors, cropErrors = cropErrors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val personal = _uiState.value.personal
                val registeredName = listOf(personal.firstName, personal.lastName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                val id = growerRepository.createFullRegistration(
                    personal = personal,
                    crop = _uiState.value.crop,
                )
                _uiState.update {
                    it.copy(
                        lastRegisteredLocalId = id,
                        lastRegisteredName = registeredName.ifBlank { "Grower" },
                    )
                }
                resetRegistrationForm()
                onCreated(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Save failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveGrowerEdit(growerLocalId: String, onSaved: () -> Unit) {
        val errors = validatePersonal(_uiState.value.personal, forEdit = true)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(personalErrors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                growerRepository.saveGrowerEdit(growerLocalId, _uiState.value.personal)
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Save failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun resubmitCorrection(growerLocalId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                growerRepository.saveGrowerEdit(growerLocalId, _uiState.value.personal)
                growerRepository.resubmitCorrection(growerLocalId)
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Resubmit failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteRegistration(localId: String) {
        viewModelScope.launch { growerRepository.deleteLocalRegistration(localId) }
    }

    fun syncAllPending() {
        viewModelScope.launch { growerRepository.triggerSync() }
    }

    fun submitCropAllocation(growerLocalId: String, onDone: () -> Unit) {
        val cropErrors = validateCrop(_uiState.value.crop)
        if (cropErrors.isNotEmpty()) {
            _uiState.update { it.copy(cropErrors = cropErrors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                growerRepository.queueCropAllocation(growerLocalId, _uiState.value.crop)
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(saveError = e.message ?: "Crop save failed") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun validateWizardIdentity(form: PersonalDetailsForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.firstName.isBlank() && form.lastName.isBlank()) {
            errors["firstName"] = "First or last name is required"
        }
        if (form.nrcNumber.isBlank()) errors["nrcNumber"] = "NRC / ID is required"
        if (form.localPhone.isBlank()) errors["localPhone"] = "Phone is required"
        return errors
    }

    private fun validateWizardFarmLocation(form: PersonalDetailsForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.provinceId.isBlank()) errors["provinceId"] = "Province is required"
        if (form.districtId.isBlank()) errors["districtId"] = "District is required"
        if (form.address.isBlank()) errors["address"] = "Address is required"
        if (form.townVillage.isBlank()) errors["townVillage"] = "Town / village is required"
        return errors
    }

    private fun validatePersonal(form: PersonalDetailsForm, forEdit: Boolean = false): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.firstName.isBlank() && form.lastName.isBlank()) {
            errors["firstName"] = "First or last name is required"
        }
        if (!forEdit) {
            if (form.nrcNumber.isBlank()) errors["nrcNumber"] = "NRC / ID is required"
            if (form.address.isBlank()) errors["address"] = "Address is required"
            if (form.townVillage.isBlank()) errors["townVillage"] = "Town / village is required"
            if (form.provinceId.isBlank()) errors["provinceId"] = "Province is required"
            if (form.districtId.isBlank()) errors["districtId"] = "District is required"
            if (form.localPhone.isBlank()) errors["localPhone"] = "Phone is required"
            if (form.profilePhotoPath == null) errors["profilePhoto"] = "Profile photo is required"
            if (form.idFrontPath == null) errors["idFront"] = "ID front is required"
            if (form.idBackPath == null) errors["idBack"] = "ID back is required"
        } else {
            if (form.provinceId.isBlank()) errors["provinceId"] = "Province is required"
            if (form.districtId.isBlank()) errors["districtId"] = "District is required"
        }
        return errors
    }

    private fun validateCrop(form: CropDetailsForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.tobaccoTypeId.isBlank()) errors["tobaccoTypeId"] = "Crop type is required"
        if (!form.isSelfSponsored && form.sponsorId.isNullOrBlank()) {
            errors["sponsorId"] = "Select a sponsor or mark self-sponsored"
        }
        val ha = form.hectarage.toDoubleOrNull()
        if (ha == null || ha < 0.5 || ha > 300) errors["hectarage"] = "Hectarage must be 0.5–300"
        val barns = form.numberOfBarns.toIntOrNull()
        if (barns == null || barns !in 1..20) errors["numberOfBarns"] = "Barns must be 1–20"
        if (form.barnTypeId.isBlank()) errors["barnTypeId"] = "Barn type is required"
        val strings = form.stringsPerBarn.toIntOrNull()
        if (strings == null || strings <= 0) errors["stringsPerBarn"] = "Strings per barn is required"
        return errors
    }
}

private fun GrowerEntity.toListItem(): GrowerListItem {
    val name = listOfNotNull(first_name, middle_name, last_name).joinToString(" ")
    val statusLabel = status.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    val statusTone = when (status.uppercase()) {
        "ACTIVE", "APPROVED" -> GlTone.Success
        "PENDING", "REVIEW" -> GlTone.Warning
        "DRAFT" -> GlTone.Default
        "RETURNED_FOR_CORRECTION" -> GlTone.Gold
        else -> GlTone.Default
    }
    return GrowerListItem(
        localId = local_id,
        name = name,
        tbzId = tbz_id ?: nrc_number,
        subtitle = listOfNotNull(phone_number, district, province).joinToString(" · ").ifBlank { "—" },
        statusLabel = statusLabel,
        statusTone = statusTone,
        syncStatus = sync_status,
        riskLabel = "Low",
        riskTone = GlTone.Success,
        flagged = status == "RETURNED_FOR_CORRECTION",
    )
}

private fun matchesPortalStatusFilter(grower: GrowerEntity, filter: String): Boolean {
    val apiStatus = apiStatusForFilter(filter) ?: return true
    return grower.status.equals(apiStatus, ignoreCase = true)
}
