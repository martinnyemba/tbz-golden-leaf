package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import zm.co.tbz.goldenleaf.data.local.dao.GrowerDao
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEditEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerRegistrationEntity
import zm.co.tbz.goldenleaf.data.local.entity.OfflineQueueEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.sync.SyncCoordinator
import zm.co.tbz.goldenleaf.ui.registration.CropDetailsForm
import zm.co.tbz.goldenleaf.ui.registration.PersonalDetailsForm
import zm.co.tbz.goldenleaf.ui.registration.formatPhoneNumber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrowerRepository @Inject constructor(
    private val growerDao: GrowerDao,
    private val syncRepository: SyncRepository,
    private val syncCoordinator: SyncCoordinator,
    private val json: Json,
) {
    fun observeGrowers(): Flow<List<GrowerEntity>> = growerDao.observeGrowers()
    fun observeGrowerById(localId: String): Flow<GrowerEntity?> = growerDao.observeGrowerById(localId)
    fun observeRegistrationById(localId: String): Flow<GrowerRegistrationEntity?> =
        growerDao.observeRegistrationById(localId)
    suspend fun upsertGrower(grower: GrowerEntity) = growerDao.upsertGrower(grower)

    suspend fun upsertGrowersFromServer(growers: List<GrowerEntity>) {
        if (growers.isEmpty()) return
        growerDao.upsertGrowers(growers)
    }

    fun observeRegistrations(): Flow<List<GrowerRegistrationEntity>> = growerDao.observeRegistrations()
    fun observeEdits(): Flow<List<GrowerEditEntity>> = growerDao.observeEdits()

    suspend fun createFullRegistration(
        personal: PersonalDetailsForm,
        crop: CropDetailsForm,
    ): String {
        val localId = UUID.randomUUID().toString()
        val phone = formatPhoneNumber(personal.country, personal.localPhone)
        val growerPayload = buildGrowerCreatePayload(personal, phone)
        val cropPayload = buildCropPayload(crop)
        val fullPayload = buildJsonObject {
            put("grower", growerPayload)
            put("crop", cropPayload)
        }
        val payloadJson = json.encodeToString(fullPayload)
        val growerPayloadJson = json.encodeToString(growerPayload)

        growerDao.upsertRegistration(
            GrowerRegistrationEntity(
                local_id = localId,
                sync_status = SyncStatuses.PENDING,
                data_json = payloadJson,
            ),
        )
        growerDao.upsertGrower(
            GrowerEntity(
                local_id = localId,
                sync_status = SyncStatuses.PENDING,
                idempotency_key = localId,
                updated_at_local = System.currentTimeMillis(),
                first_name = personal.firstName.trim(),
                middle_name = personal.middleName.trim().ifBlank { null },
                last_name = personal.lastName.trim(),
                nrc_number = personal.nrcNumber.trim().uppercase(),
                sex = personal.sex,
                date_of_birth = personal.dateOfBirth.ifBlank { null },
                category = personal.category,
                phone_number = phone,
                email = personal.email.trim().ifBlank { null },
                address = personal.address.trim(),
                town_village = personal.townVillage.trim(),
                province = personal.provinceId,
                district = personal.districtId,
                gps_latitude = personal.gpsLatitude.toDoubleOrNull(),
                gps_longitude = personal.gpsLongitude.toDoubleOrNull(),
                profile_photo_path = personal.profilePhotoPath,
                id_front_path = personal.idFrontPath,
                id_back_path = personal.idBackPath,
                status = "PENDING",
            ),
        )
        enqueue(
            localId = localId,
            operation = "POST",
            endpoint = "growers/growers/",
            payloadJson = growerPayloadJson,
        )
        return localId
    }

    suspend fun saveGrowerEdit(
        growerLocalId: String,
        personal: PersonalDetailsForm,
    ): String {
        val grower = growerDao.getGrowerById(growerLocalId)
            ?: error("Grower not found")
        val phone = formatPhoneNumber(personal.country, personal.localPhone)
        val patch = buildGrowerPatchPayload(personal, phone)
        val patchJson = json.encodeToString(patch)
        val editId = UUID.randomUUID().toString()

        growerDao.upsertEdit(
            GrowerEditEntity(
                local_id = editId,
                grower_local_id = growerLocalId,
                sync_status = SyncStatuses.PENDING,
                patch_json = patchJson,
            ),
        )
        growerDao.upsertGrower(
            grower.copy(
                first_name = personal.firstName.trim(),
                middle_name = personal.middleName.trim().ifBlank { null },
                last_name = personal.lastName.trim(),
                nrc_number = personal.nrcNumber.trim().uppercase(),
                sex = personal.sex,
                date_of_birth = personal.dateOfBirth.ifBlank { null },
                phone_number = phone,
                email = personal.email.trim().ifBlank { null },
                address = personal.address.trim(),
                town_village = personal.townVillage.trim(),
                province = personal.provinceId,
                district = personal.districtId,
                profile_photo_path = personal.profilePhotoPath ?: grower.profile_photo_path,
                id_front_path = personal.idFrontPath ?: grower.id_front_path,
                id_back_path = personal.idBackPath ?: grower.id_back_path,
                sync_status = SyncStatuses.PENDING,
                updated_at_local = System.currentTimeMillis(),
            ),
        )

        val remoteId = grower.remote_id ?: growerLocalId
        enqueue(
            localId = editId,
            operation = "PATCH",
            endpoint = "growers/growers/$remoteId/",
            payloadJson = patchJson,
        )
        return editId
    }

    suspend fun resubmitCorrection(growerLocalId: String) {
        val grower = growerDao.getGrowerById(growerLocalId)
            ?: error("Grower not found")
        val remoteId = grower.remote_id ?: growerLocalId
        val resubmitId = UUID.randomUUID().toString()
        enqueue(
            localId = resubmitId,
            operation = "POST",
            endpoint = "growers/growers/$remoteId/resubmit-corrections/",
            payloadJson = "{}",
        )
        growerDao.upsertGrower(
            grower.copy(
                status = "PENDING",
                sync_status = SyncStatuses.PENDING,
                updated_at_local = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun queueCropAllocation(growerLocalId: String, crop: CropDetailsForm) {
        val grower = growerDao.getGrowerById(growerLocalId) ?: error("Grower not found")
        val remoteId = grower.remote_id ?: growerLocalId
        val cropId = UUID.randomUUID().toString()
        val payloadJson = json.encodeToString(buildCropPayload(crop, remoteId))
        enqueue(
            localId = cropId,
            operation = "POST",
            endpoint = "growers/crop-allocations/",
            payloadJson = payloadJson,
        )
    }

    suspend fun deleteLocalRegistration(localId: String) {
        growerDao.getRegistrationById(localId)?.let { growerDao.deleteRegistration(it) }
        growerDao.getGrowerById(localId)?.let { growerDao.upsertGrower(it.copy(sync_status = SyncStatuses.FAILED)) }
    }

    suspend fun deleteLocalEdit(editLocalId: String) {
        growerDao.getEditById(editLocalId)?.let { growerDao.deleteEdit(it) }
    }

    suspend fun triggerSync() = syncCoordinator.scheduleUpload()

    private suspend fun enqueue(
        localId: String,
        operation: String,
        endpoint: String,
        payloadJson: String,
    ) {
        syncRepository.upsertQueueItem(
            OfflineQueueEntity(
                local_id = localId,
                sync_status = SyncStatuses.PENDING,
                idempotency_key = localId,
                operation = operation,
                endpoint = endpoint,
                payload_json = payloadJson,
            ),
        )
        syncCoordinator.scheduleUpload()
    }

    private fun buildGrowerCreatePayload(personal: PersonalDetailsForm, phone: String): JsonObject =
        buildJsonObject {
            put("grower_type", personal.growerType)
            put("first_name", personal.firstName.trim().uppercase())
            if (personal.middleName.isNotBlank()) put("middle_name", personal.middleName.trim().uppercase())
            put("last_name", personal.lastName.trim().uppercase())
            put("nrc_number", personal.nrcNumber.trim().uppercase())
            put("sex", personal.sex)
            if (personal.dateOfBirth.isNotBlank()) put("date_of_birth", personal.dateOfBirth)
            put("category", personal.category)
            put("phone_number", phone)
            if (personal.email.isNotBlank()) put("email", personal.email.trim())
            put("address", personal.address.trim())
            put("town_or_village", personal.townVillage.trim())
            put("province", personal.provinceId)
            put("district", personal.districtId)
            put("country", personal.country)
            personal.gpsLatitude.toDoubleOrNull()?.let { put("gps_latitude", it) }
            personal.gpsLongitude.toDoubleOrNull()?.let { put("gps_longitude", it) }
        }

    private fun buildGrowerPatchPayload(personal: PersonalDetailsForm, phone: String): JsonObject =
        buildJsonObject {
            if (personal.firstName.isNotBlank()) put("first_name", personal.firstName.trim().uppercase())
            if (personal.middleName.isNotBlank()) put("middle_name", personal.middleName.trim().uppercase())
            if (personal.lastName.isNotBlank()) put("last_name", personal.lastName.trim().uppercase())
            if (personal.nrcNumber.isNotBlank()) put("nrc_number", personal.nrcNumber.trim().uppercase())
            put("sex", personal.sex)
            if (personal.dateOfBirth.isNotBlank()) put("date_of_birth", personal.dateOfBirth)
            put("phone_number", phone)
            if (personal.email.isNotBlank()) put("email", personal.email.trim())
            if (personal.address.isNotBlank()) put("address", personal.address.trim())
            if (personal.townVillage.isNotBlank()) put("town_or_village", personal.townVillage.trim())
            put("province", personal.provinceId)
            put("district", personal.districtId)
        }

    private fun buildCropPayload(crop: CropDetailsForm, growerRemoteId: String? = null): JsonObject =
        buildJsonObject {
            growerRemoteId?.let { put("grower", it) }
            put("tobacco_type", crop.tobaccoTypeId)
            if (crop.isSelfSponsored) {
                put("is_self_sponsored", true)
            } else {
                crop.sponsorId?.let { put("sponsor", it) }
            }
            put("hectarage", crop.hectarage.toDoubleOrNull() ?: 0.0)
            put("number_of_barns", crop.numberOfBarns.toIntOrNull() ?: 0)
            put("barn_type", crop.barnTypeId)
            put("number_of_strings_per_barn", crop.stringsPerBarn.toIntOrNull() ?: 0)
            crop.gpsLatitude.toDoubleOrNull()?.let { put("gps_latitude", it) }
            crop.gpsLongitude.toDoubleOrNull()?.let { put("gps_longitude", it) }
        }
}
