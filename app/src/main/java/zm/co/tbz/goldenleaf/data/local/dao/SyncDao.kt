package zm.co.tbz.goldenleaf.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.entity.OfflineQueueEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncCursorEntity

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_cursors WHERE entity_type = :type")
    suspend fun getCursor(type: String): SyncCursorEntity?

    @Upsert
    suspend fun upsertCursor(cursor: SyncCursorEntity)

    @Query("SELECT * FROM offline_queue ORDER BY queued_at ASC")
    fun observeQueue(): Flow<List<OfflineQueueEntity>>

    @Query("SELECT * FROM offline_queue WHERE sync_status = 'pending' ORDER BY queued_at ASC")
    suspend fun getPendingItems(): List<OfflineQueueEntity>

    @Upsert
    suspend fun upsertQueueItem(item: OfflineQueueEntity)

    @Query("UPDATE offline_queue SET sync_status = :status, last_sync_error = :error WHERE local_id = :localId")
    suspend fun updateStatus(localId: String, status: String, error: String?): Int

    @Delete
    suspend fun deleteQueueItem(item: OfflineQueueEntity): Int

    @Query("SELECT COUNT(*) FROM offline_queue WHERE sync_status = 'pending'")
    suspend fun pendingCount(): Int
}
