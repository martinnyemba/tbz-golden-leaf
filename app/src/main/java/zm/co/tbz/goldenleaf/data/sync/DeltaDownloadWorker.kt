package zm.co.tbz.goldenleaf.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitEntity
import zm.co.tbz.goldenleaf.data.local.entity.InspectionEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncCursorEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.local.entity.TransportPermitEntity
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.repository.AuthRepository
import zm.co.tbz.goldenleaf.data.repository.GrowerRepository
import zm.co.tbz.goldenleaf.data.repository.InspectionRepository
import zm.co.tbz.goldenleaf.data.repository.PermitRepository
import zm.co.tbz.goldenleaf.data.repository.SyncRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@HiltWorker
class DeltaDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: TrmcsApi,
    private val growerRepository: GrowerRepository,
    private val permitRepository: PermitRepository,
    private val inspectionRepository: InspectionRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val json: Json,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!authRepository.refreshSessionIfNeeded()) return Result.retry()
        return try {
            pullGrowers()
            pullTransportPermits()
            pullGroupPermits()
            pullInspections()
            userPreferences.setLastSyncAt(System.currentTimeMillis())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun pullGrowers() {
        val cursor = syncRepository.getCursor(ENTITY_GROWERS)
        var updatedAfter = cursor?.updated_after
        var pageAfter: String? = null
        val maxUpdated = cursor?.updated_after
        do {
            val page = api.getGrowers(updatedAfter = updatedAfter)
            val entities = page.results.map { dto ->
                GrowerEntity(
                    local_id = dto.id,
                    remote_id = dto.id,
                    sync_status = SyncStatuses.SYNCED,
                    idempotency_key = dto.id,
                    updated_at_local = System.currentTimeMillis(),
                    updated_at_server = null,
                    first_name = dto.first_name,
                    last_name = dto.last_name,
                    nrc_number = dto.nrc_number ?: "",
                    tbz_id = dto.tbz_id,
                    province = dto.province,
                    district = dto.district,
                    status = dto.status,
                )
            }
            growerRepository.upsertGrowersFromServer(entities)
            pageAfter = page.next
            updatedAfter = null
        } while (pageAfter != null)
        syncRepository.upsertCursor(
            SyncCursorEntity(
                entity_type = ENTITY_GROWERS,
                updated_after = maxUpdated ?: nowIso(),
                last_success_at = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun pullTransportPermits() {
        val cursor = syncRepository.getCursor(ENTITY_PERMITS)
        var updatedAfter = cursor?.updated_after
        var pageAfter: String? = null
        val maxUpdated = cursor?.updated_after
        do {
            val page = api.getTransportPermits(updatedAfter = updatedAfter)
            val entities = page.results.map { dto ->
                TransportPermitEntity(
                    local_id = dto.id,
                    remote_id = dto.id,
                    sync_status = SyncStatuses.SYNCED,
                    idempotency_key = dto.id,
                    updated_at_local = System.currentTimeMillis(),
                    permit_number = dto.permit_number,
                    grower_id = dto.id,
                    grower_name = dto.grower_name,
                    total_bales = dto.total_bales,
                    total_weight_kg = dto.total_weight_kg,
                    purpose = dto.purpose.orEmpty(),
                    license_plate = dto.license_plate.orEmpty(),
                    origin_province = dto.origin_province.orEmpty(),
                    origin_district = dto.origin_district.orEmpty(),
                    destination_sales_floor = dto.destination_sales_floor.orEmpty(),
                    status = dto.status,
                    valid_from = dto.valid_from,
                    valid_to = dto.valid_to,
                    correction_reason = dto.correction_reason,
                    rejection_reason = dto.rejection_reason,
                    comments = dto.comments,
                )
            }
            permitRepository.upsertTransportPermits(entities)
            pageAfter = page.next
            updatedAfter = null
        } while (pageAfter != null)
        syncRepository.upsertCursor(
            SyncCursorEntity(
                entity_type = ENTITY_PERMITS,
                updated_after = maxUpdated ?: nowIso(),
                last_success_at = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun pullGroupPermits() {
        val cursor = syncRepository.getCursor(ENTITY_GROUP_PERMITS)
        var updatedAfter = cursor?.updated_after
        var pageAfter: String? = null
        val maxUpdated = cursor?.updated_after
        do {
            val page = api.getGroupPermits(updatedAfter = updatedAfter)
            val entities = page.results.map { dto ->
                GroupPermitEntity(
                    local_id = dto.id,
                    remote_id = dto.id,
                    sync_status = SyncStatuses.SYNCED,
                    idempotency_key = dto.id,
                    updated_at_local = System.currentTimeMillis(),
                    permit_number = dto.group_permit_number,
                    license_plate = dto.license_plate,
                    origin_province = dto.origin_province.orEmpty(),
                    origin_district = dto.origin_district.orEmpty(),
                    destination_sales_floor = dto.destination_sales_floor,
                    purpose = dto.purpose.orEmpty(),
                    status = dto.status,
                    entry_count = dto.entries?.size ?: 0,
                    total_bales = dto.total_bales,
                    total_weight_kg = dto.total_weight_kg,
                    valid_from = dto.valid_from,
                    valid_to = dto.valid_to,
                    correction_reason = dto.correction_reason,
                    rejection_reason = dto.rejection_reason,
                    comments = dto.comments,
                    entries_json = dto.entries?.let { json.encodeToString(it) },
                )
            }
            permitRepository.upsertGroupPermits(entities)
            pageAfter = page.next
            updatedAfter = null
        } while (pageAfter != null)
        syncRepository.upsertCursor(
            SyncCursorEntity(
                entity_type = ENTITY_GROUP_PERMITS,
                updated_after = maxUpdated ?: nowIso(),
                last_success_at = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun pullInspections() {
        val cursor = syncRepository.getCursor(ENTITY_INSPECTIONS)
        var updatedAfter = cursor?.updated_after
        var pageAfter: String? = null
        val maxUpdated = cursor?.updated_after
        do {
            val page = api.getInspections(updatedAfter = updatedAfter)
            val entities = page.results.map { dto ->
                InspectionEntity(
                    local_id = dto.id,
                    remote_id = dto.id,
                    sync_status = SyncStatuses.SYNCED,
                    idempotency_key = dto.id,
                    updated_at_local = System.currentTimeMillis(),
                    grower_id = dto.id,
                    grower_name = dto.grower_name,
                    inspection_type = dto.inspection_type,
                    scheduled_date = dto.scheduled_date,
                    status = dto.status,
                )
            }
            inspectionRepository.upsertInspections(entities)
            pageAfter = page.next
            updatedAfter = null
        } while (pageAfter != null)
        syncRepository.upsertCursor(
            SyncCursorEntity(
                entity_type = ENTITY_INSPECTIONS,
                updated_after = maxUpdated ?: nowIso(),
                last_success_at = System.currentTimeMillis(),
            ),
        )
    }

    private fun nowIso(): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC))

    companion object {
        private const val ENTITY_GROWERS = "growers"
        private const val ENTITY_PERMITS = "transport_permits"
        private const val ENTITY_GROUP_PERMITS = "group_permits"
        private const val ENTITY_INSPECTIONS = "inspections"
    }
}
