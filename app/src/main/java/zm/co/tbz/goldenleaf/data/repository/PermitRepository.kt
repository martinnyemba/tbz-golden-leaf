package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import zm.co.tbz.goldenleaf.data.local.dao.PermitDao
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitDraftEntity
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitEntity
import zm.co.tbz.goldenleaf.data.local.entity.OfflineQueueEntity
import zm.co.tbz.goldenleaf.data.local.entity.PermitRequestEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.local.entity.TransportPermitEntity
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.remote.dto.GroupPermitValidateRequest
import zm.co.tbz.goldenleaf.data.remote.dto.GroupPermitValidateResponse
import zm.co.tbz.goldenleaf.data.remote.dto.VerifyQrRequest
import zm.co.tbz.goldenleaf.data.remote.dto.VerifyQrResponse
import zm.co.tbz.goldenleaf.data.sync.SyncCoordinator
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermitRepository @Inject constructor(
    private val permitDao: PermitDao,
    private val syncRepository: SyncRepository,
    private val syncCoordinator: SyncCoordinator,
    private val api: TrmcsApi,
    private val json: Json,
) {
    fun observeTransportPermits(): Flow<List<TransportPermitEntity>> = permitDao.observeTransportPermits()
    suspend fun upsertTransportPermits(permits: List<TransportPermitEntity>) = permitDao.upsertTransportPermits(permits)

    fun observeGroupPermits(): Flow<List<GroupPermitEntity>> = permitDao.observeGroupPermits()
    fun observeGroupPermit(localId: String): Flow<GroupPermitEntity?> = permitDao.observeGroupPermit(localId)
    suspend fun upsertGroupPermits(permits: List<GroupPermitEntity>) = permitDao.upsertGroupPermits(permits)

    fun observeGroupPermitDrafts(): Flow<List<GroupPermitDraftEntity>> = permitDao.observeGroupPermitDrafts()
    fun observeGroupPermitDraft(localId: String): Flow<GroupPermitDraftEntity?> =
        permitDao.observeGroupPermitDraft(localId)

    fun observePermitRequests(): Flow<List<PermitRequestEntity>> = permitDao.observePermitRequests()
    suspend fun deletePermitRequest(request: PermitRequestEntity) = permitDao.deletePermitRequest(request)

    suspend fun verifyPermit(token: String, salesfloorId: String): VerifyQrResponse =
        api.verifyQr(VerifyQrRequest(permit_token = token, salesfloor_id = salesfloorId.ifBlank { null }))

    suspend fun validateGroupPermitQr(token: String): GroupPermitValidateResponse =
        api.validateGroupPermitQr(GroupPermitValidateRequest(permit_token = token.trim()))

    suspend fun submitPermitRequest(payloadJson: String): String {
        val localId = UUID.randomUUID().toString()
        permitDao.upsertPermitRequest(
            PermitRequestEntity(
                local_id = localId,
                sync_status = SyncStatuses.PENDING,
                data_json = payloadJson,
            ),
        )
        enqueue(localId, "POST", "permits/transport-permits/", payloadJson)
        return localId
    }

    suspend fun queueTransportPermitReview(
        permitLocalId: String,
        remoteId: String,
        payloadJson: String,
    ) {
        val queueId = "$permitLocalId-review-${UUID.randomUUID()}"
        enqueue(queueId, "POST", "permits/transport-permits/$remoteId/approve-reject/", payloadJson)
    }

    suspend fun queueGroupPermitReview(
        permitLocalId: String,
        remoteId: String,
        payloadJson: String,
    ) {
        val queueId = "$permitLocalId-review-${UUID.randomUUID()}"
        enqueue(queueId, "POST", "permits/group-permits/$remoteId/approve-reject/", payloadJson)
    }

    suspend fun saveGroupPermitDraft(
        localId: String?,
        headerJson: String,
        entriesJson: String,
    ): String {
        val id = localId ?: UUID.randomUUID().toString()
        permitDao.upsertGroupPermitDraft(
            GroupPermitDraftEntity(
                local_id = id,
                sync_status = SyncStatuses.PENDING,
                header_json = headerJson,
                entries_json = entriesJson,
                phase = GroupPermitDraftEntity.PHASE_DRAFT,
            ),
        )
        return id
    }

    suspend fun submitGroupPermitDraft(draftId: String): Result<String> {
        val draft = permitDao.getGroupPermitDraft(draftId)
            ?: return Result.failure(IllegalStateException("Draft not found"))
        val entries = parseDraftEntries(draft.entries_json)
        if (entries.size < 2) {
            return Result.failure(IllegalStateException("Group permit requires at least 2 growers"))
        }
        return try {
            val header = json.decodeFromString<JsonObject>(draft.header_json)
            val created = api.createGroupPermit(header)
            entries.forEach { entry ->
                api.addGroupPermitEntry(created.id, entry)
            }
            api.submitGroupPermit(created.id)
            permitDao.deleteGroupPermitDraft(draft)
            syncCoordinator.scheduleDeltaDownload()
            Result.success(created.id)
        } catch (_: Exception) {
            permitDao.upsertGroupPermitDraft(
                draft.copy(
                    sync_status = SyncStatuses.PENDING,
                    phase = GroupPermitDraftEntity.PHASE_HEADER_QUEUED,
                ),
            )
            enqueue(draftId, "POST", "permits/group-permits/", draft.header_json)
            Result.success(draftId)
        }
    }

    suspend fun handleSyncServerData(clientId: String, serverDataJson: String) {
        val draft = permitDao.getGroupPermitDraft(clientId) ?: return
        if (draft.phase != GroupPermitDraftEntity.PHASE_HEADER_QUEUED) return
        val remoteId = runCatching {
            json.parseToJsonElement(serverDataJson).jsonObject["id"]?.jsonPrimitive?.content
        }.getOrNull() ?: return
        continueGroupPermitAfterHeaderSync(clientId, remoteId)
    }

    suspend fun continueGroupPermitAfterHeaderSync(draftId: String, remoteId: String) {
        val draft = permitDao.getGroupPermitDraft(draftId) ?: return
        val entries = parseDraftEntries(draft.entries_json)
        permitDao.upsertGroupPermitDraft(
            draft.copy(
                remote_id = remoteId,
                phase = GroupPermitDraftEntity.PHASE_ENTRIES_QUEUED,
            ),
        )
        entries.forEachIndexed { index, entry ->
            val entryId = "$draftId-entry-$index"
            enqueue(
                entryId,
                "POST",
                "permits/group-permits/$remoteId/entries/",
                json.encodeToString(JsonObject.serializer(), entry),
            )
        }
        enqueue(
            "$draftId-submit",
            "POST",
            "permits/group-permits/$remoteId/submit/",
            "{}",
        )
        permitDao.upsertGroupPermitDraft(
            draft.copy(
                remote_id = remoteId,
                phase = GroupPermitDraftEntity.PHASE_DONE,
                sync_status = SyncStatuses.SYNCED,
            ),
        )
    }

    suspend fun resubmitGroupPermitCorrections(localId: String, remoteId: String) {
        enqueue(
            "$localId-resubmit",
            "POST",
            "permits/group-permits/$remoteId/resubmit-corrections/",
            "{}",
        )
    }

    suspend fun patchTransportPermit(localId: String, remoteId: String, payloadJson: String) {
        val patchId = "$localId-patch-${UUID.randomUUID()}"
        enqueue(patchId, "PATCH", "permits/transport-permits/$remoteId/", payloadJson)
    }

    suspend fun resubmitTransportPermitCorrections(localId: String, remoteId: String) {
        enqueue(
            "$localId-resubmit",
            "POST",
            "permits/transport-permits/$remoteId/resubmit-corrections/",
            "{}",
        )
    }

    suspend fun queueGroupPermitEntry(remoteId: String, entryJson: String): String {
        val entryId = UUID.randomUUID().toString()
        enqueue(entryId, "POST", "permits/group-permits/$remoteId/entries/", entryJson)
        return entryId
    }

    suspend fun patchGroupPermitHeader(localId: String, remoteId: String, headerJson: String) {
        enqueue(localId, "PATCH", "permits/group-permits/$remoteId/", headerJson)
    }

    suspend fun deleteGroupPermitDraft(draft: GroupPermitDraftEntity) {
        permitDao.deleteGroupPermitDraft(draft)
    }

    suspend fun triggerSync() {
        syncCoordinator.scheduleUpload()
        syncCoordinator.scheduleDeltaDownload()
    }

    private fun parseDraftEntries(entriesJson: String): List<JsonObject> =
        runCatching {
            json.decodeFromString(ListSerializer(JsonObject.serializer()), entriesJson)
        }.getOrDefault(emptyList())

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
