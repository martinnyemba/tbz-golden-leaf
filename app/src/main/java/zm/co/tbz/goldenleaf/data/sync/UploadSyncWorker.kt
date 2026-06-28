package zm.co.tbz.goldenleaf.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.remote.dto.SyncBulkRequest
import zm.co.tbz.goldenleaf.data.remote.dto.SyncItemDto
import zm.co.tbz.goldenleaf.data.repository.AuthRepository
import zm.co.tbz.goldenleaf.data.repository.PermitRepository
import zm.co.tbz.goldenleaf.data.repository.SyncRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@HiltWorker
class UploadSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val api: TrmcsApi,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val json: Json,
    private val permitRepository: PermitRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!authRepository.refreshSessionIfNeeded()) return Result.retry()
        val pending = syncRepository.getPendingItems()
        if (pending.isEmpty()) return Result.success()

        pending.chunked(BATCH_SIZE).forEach { batch ->
            val items = batch.map { queueItem ->
                SyncItemDto(
                    client_id = queueItem.local_id,
                    idempotency_key = queueItem.idempotency_key,
                    operation = queueItem.operation,
                    endpoint = queueItem.endpoint,
                    payload = json.parseToJsonElement(queueItem.payload_json),
                    queued_at = formatQueuedAt(queueItem.queued_at),
                )
            }
            batch.forEach { syncRepository.updateStatus(it.local_id, "syncing") }
            try {
                val response = api.syncBulk(SyncBulkRequest(items))
                response.results.forEach { result ->
                    val status = when (result.status.lowercase()) {
                        "uploaded" -> SyncStatuses.SYNCED
                        "failed" -> when (result.http_status) {
                            409 -> SyncStatuses.NEEDS_REVIEW
                            401, 403 -> SyncStatuses.BLOCKED
                            else -> SyncStatuses.FAILED
                        }
                        else -> SyncStatuses.FAILED
                    }
                    syncRepository.updateStatus(
                        localId = result.client_id,
                        status = status,
                        error = result.error,
                    )
                    if (status == SyncStatuses.SYNCED) {
                        result.server_data?.let { serverData ->
                            handleGroupPermitContinuation(result.client_id, serverData)
                        }
                    }
                }
            } catch (_: Exception) {
                batch.forEach { syncRepository.updateStatus(it.local_id, "pending") }
                return Result.retry()
            }
        }
        userPreferences.setLastSyncAt(System.currentTimeMillis())
        return Result.success()
    }

    private suspend fun handleGroupPermitContinuation(clientId: String, serverData: JsonElement) {
        runCatching {
            permitRepository.handleSyncServerData(clientId, json.encodeToString(JsonElement.serializer(), serverData))
        }
    }

    private fun formatQueuedAt(epochMs: Long): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            Instant.ofEpochMilli(epochMs).atOffset(ZoneOffset.UTC),
        )

    companion object {
        private const val BATCH_SIZE = 200
    }
}
