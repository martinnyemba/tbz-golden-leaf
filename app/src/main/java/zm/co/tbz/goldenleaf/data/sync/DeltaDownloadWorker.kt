package zm.co.tbz.goldenleaf.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncCursorEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.repository.AuthRepository
import zm.co.tbz.goldenleaf.data.repository.GrowerRepository
import zm.co.tbz.goldenleaf.data.repository.SyncRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@HiltWorker
class DeltaDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: TrmcsApi,
    private val growerRepository: GrowerRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!authRepository.refreshSessionIfNeeded()) return Result.retry()
        return try {
            pullGrowers()
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
        var maxUpdated = cursor?.updated_after
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

    private fun nowIso(): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC))

    companion object {
        private const val ENTITY_GROWERS = "growers"
    }
}
