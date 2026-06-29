package zm.co.tbz.goldenleaf.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
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
    private val referenceRepository: zm.co.tbz.goldenleaf.data.repository.ReferenceRepository,
    private val userPreferences: UserPreferences,
    private val json: Json,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Best-effort proactive refresh; the OkHttp authenticator also refreshes on 401.
        authRepository.refreshSessionIfNeeded()

        // Each section is independent: one failing endpoint must not block the rest,
        // and a partial pull should still update the "last sync" timestamp.
        val errors = mutableListOf<String>()
        var anySuccess = false
        suspend fun section(name: String, block: suspend () -> Unit) {
            runCatching { block() }
                .onSuccess { anySuccess = true }
                .onFailure { errors += "$name: ${it.message ?: it.javaClass.simpleName}" }
        }

        section("reference") { referenceRepository.refreshReference() }
        section("growers") { pullGrowers() }
        section("transport permits") { pullTransportPermits() }
        section("group permits") { pullGroupPermits() }
        section("inspections") { pullInspections() }

        if (anySuccess) {
            userPreferences.setLastSyncAt(System.currentTimeMillis())
        }
        userPreferences.setLastSyncError(errors.takeIf { it.isNotEmpty() }?.joinToString("\n"))

        return when {
            errors.isEmpty() -> Result.success()
            anySuccess -> Result.success() // partial; surfaced via lastSyncError
            else -> Result.retry()
        }
    }

    private suspend fun pullGrowers() {
        val cursor = syncRepository.getCursor(ENTITY_GROWERS)
        val updatedAfter = cursor?.updated_after
        growerRepository.pullGrowersDelta(updatedAfter)
        syncRepository.upsertCursor(
            SyncCursorEntity(
                entity_type = ENTITY_GROWERS,
                updated_after = updatedAfter ?: nowIso(),
                last_success_at = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun pullTransportPermits() {
        val cursor = syncRepository.getCursor(ENTITY_PERMITS)
        val updatedAfter = cursor?.updated_after
        val maxUpdated = cursor?.updated_after
        var pageNumber = 1
        while (true) {
            val page = api.getTransportPermits(updatedAfter = updatedAfter, page = pageNumber)
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
                    grower_category = dto.grower_category.orEmpty().ifBlank { "SMALL_SCALE" },
                    buyer_id = dto.buyer,
                    is_bought = dto.is_bought ?: false,
                    buyer_accepted = dto.buyer_accepted ?: false,
                )
            }
            permitRepository.upsertTransportPermits(entities)
            if (page.next == null) break
            pageNumber++
        }
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
        val updatedAfter = cursor?.updated_after
        val maxUpdated = cursor?.updated_after
        var pageNumber = 1
        while (true) {
            val page = api.getGroupPermits(updatedAfter = updatedAfter, page = pageNumber)
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
            if (page.next == null) break
            pageNumber++
        }
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
        val updatedAfter = cursor?.updated_after
        val maxUpdated = cursor?.updated_after
        var pageNumber = 1
        while (true) {
            val page = api.getInspections(updatedAfter = updatedAfter, page = pageNumber)
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
            if (page.next == null) break
            pageNumber++
        }
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
