package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import zm.co.tbz.goldenleaf.data.local.dao.MarketingDao
import zm.co.tbz.goldenleaf.data.local.entity.OfflineQueueEntity
import zm.co.tbz.goldenleaf.data.local.entity.PendingSaleEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.remote.dto.VerifyQrRequest
import zm.co.tbz.goldenleaf.data.remote.dto.VerifyQrResponse
import zm.co.tbz.goldenleaf.data.sync.SyncCoordinator
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketingRepository @Inject constructor(
    private val marketingDao: MarketingDao,
    private val syncRepository: SyncRepository,
    private val syncCoordinator: SyncCoordinator,
    private val api: TrmcsApi,
    private val json: Json,
) {
    fun observePendingSales(): Flow<List<PendingSaleEntity>> = marketingDao.observePendingSales()
    suspend fun deletePendingSale(sale: PendingSaleEntity) = marketingDao.deletePendingSale(sale)

    suspend fun verifyPermit(token: String, salesfloorId: String): VerifyQrResponse =
        api.verifyQr(VerifyQrRequest(permit_token = token, salesfloor_id = salesfloorId))

    suspend fun queuePendingSale(permitToken: String, payloadJson: String): String {
        val localId = UUID.randomUUID().toString()
        marketingDao.upsertPendingSale(
            PendingSaleEntity(
                local_id = localId,
                sync_status = SyncStatuses.PENDING,
                permit_token = permitToken,
                data_json = payloadJson,
            ),
        )
        syncRepository.upsertQueueItem(
            OfflineQueueEntity(
                local_id = localId,
                sync_status = SyncStatuses.PENDING,
                idempotency_key = localId,
                operation = "POST",
                endpoint = "marketing/bales/bulk-create/",
                payload_json = payloadJson,
            ),
        )
        syncCoordinator.scheduleUpload()
        return localId
    }

    suspend fun updatePendingSale(localId: String, permitToken: String, payloadJson: String) {
        marketingDao.upsertPendingSale(
            PendingSaleEntity(
                local_id = localId,
                sync_status = SyncStatuses.PENDING,
                permit_token = permitToken,
                data_json = payloadJson,
            ),
        )
        syncRepository.upsertQueueItem(
            OfflineQueueEntity(
                local_id = localId,
                sync_status = SyncStatuses.PENDING,
                idempotency_key = localId,
                operation = "POST",
                endpoint = "marketing/bales/bulk-create/",
                payload_json = payloadJson,
            ),
        )
        syncCoordinator.scheduleUpload()
    }

    suspend fun triggerSync() = syncCoordinator.scheduleUpload()
}
