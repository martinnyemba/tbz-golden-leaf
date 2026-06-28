package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.dao.SyncDao
import zm.co.tbz.goldenleaf.data.local.entity.OfflineQueueEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val syncDao: SyncDao
) {
    fun observeQueue(): Flow<List<OfflineQueueEntity>> = syncDao.observeQueue()

    suspend fun getPendingItems(): List<OfflineQueueEntity> = syncDao.getPendingItems()

    suspend fun upsertQueueItem(item: OfflineQueueEntity) = syncDao.upsertQueueItem(item)

    suspend fun updateStatus(localId: String, status: String, error: String? = null) =
        syncDao.updateStatus(localId, status, error)

    suspend fun deleteQueueItem(item: OfflineQueueEntity) = syncDao.deleteQueueItem(item)
}
