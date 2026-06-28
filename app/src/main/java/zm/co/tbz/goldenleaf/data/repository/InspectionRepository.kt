package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import zm.co.tbz.goldenleaf.data.local.dao.InspectionDao
import zm.co.tbz.goldenleaf.data.local.entity.InspectionEntity
import zm.co.tbz.goldenleaf.data.local.entity.InspectionReportEntity
import zm.co.tbz.goldenleaf.data.local.entity.OfflineQueueEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.local.entity.ValidationEntity
import zm.co.tbz.goldenleaf.data.sync.SyncCoordinator
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectionRepository @Inject constructor(
    private val inspectionDao: InspectionDao,
    private val syncRepository: SyncRepository,
    private val syncCoordinator: SyncCoordinator,
    private val json: Json,
) {
    fun observeInspections(): Flow<List<InspectionEntity>> = inspectionDao.observeInspections()
    suspend fun upsertInspections(inspections: List<InspectionEntity>) = inspectionDao.upsertInspections(inspections)

    fun observeReports(): Flow<List<InspectionReportEntity>> = inspectionDao.observeReports()
    suspend fun deleteReport(report: InspectionReportEntity) = inspectionDao.deleteReport(report)

    fun observeValidations(): Flow<List<ValidationEntity>> = inspectionDao.observeValidations()
    suspend fun upsertValidations(validations: List<ValidationEntity>) = inspectionDao.upsertValidations(validations)

    suspend fun scheduleInspection(
        growerId: String,
        growerName: String?,
        inspectorId: String,
        inspectionType: String,
        scheduledDate: String,
        province: String,
        district: String,
        notes: String?,
    ): String {
        val localId = UUID.randomUUID().toString()
        val payload = buildJsonObject {
            put("grower", growerId)
            put("inspector", inspectorId)
            put("inspection_type", inspectionType)
            put("scheduled_date", scheduledDate)
            put("province", province)
            put("district", district)
            if (!notes.isNullOrBlank()) put("notes", notes)
        }
        val payloadJson = json.encodeToString(payload)
        inspectionDao.upsertInspections(
            listOf(
                InspectionEntity(
                    local_id = localId,
                    sync_status = SyncStatuses.PENDING,
                    idempotency_key = localId,
                    updated_at_local = System.currentTimeMillis(),
                    grower_id = growerId,
                    grower_name = growerName,
                    inspector_id = inspectorId,
                    inspection_type = inspectionType,
                    scheduled_date = scheduledDate,
                    status = "SCHEDULED",
                    province = province,
                    district = district,
                    notes = notes,
                ),
            ),
        )
        enqueue(localId, "POST", "inspectorate/inspections/", payloadJson)
        return localId
    }

    suspend fun saveInspectionReport(
        type: String,
        endpoint: String,
        payloadJson: String,
        inspectionLocalId: String? = null,
    ): String {
        val localId = UUID.randomUUID().toString()
        inspectionDao.upsertReport(
            InspectionReportEntity(
                local_id = localId,
                sync_status = SyncStatuses.PENDING,
                inspection_local_id = inspectionLocalId,
                type = type,
                data_json = payloadJson,
            ),
        )
        enqueue(localId, "POST", endpoint, payloadJson)
        return localId
    }

    suspend fun triggerSync() = syncCoordinator.scheduleUpload()

    suspend fun submitArbitration(
        baleId: String,
        arbitrationDate: String,
        isRejected: Boolean,
        rejectionReason: String?,
        inspectorRemarks: String,
        isFinal: Boolean = false,
    ): String {
        val localId = UUID.randomUUID().toString()
        val payload = buildJsonObject {
            put("bale_id", baleId.trim())
            put("arbitration_date", arbitrationDate)
            put("is_rejected", isRejected)
            if (isRejected && !rejectionReason.isNullOrBlank()) {
                put("rejection_reason", rejectionReason)
            }
            if (inspectorRemarks.isNotBlank()) put("inspector_remarks", inspectorRemarks.trim())
            put("is_final", isFinal)
        }
        val payloadJson = json.encodeToString(payload)
        enqueue(localId, "POST", "inspectorate/arbitrations/", payloadJson)
        return localId
    }

    private suspend fun enqueue(localId: String, operation: String, endpoint: String, payloadJson: String) {
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
}
