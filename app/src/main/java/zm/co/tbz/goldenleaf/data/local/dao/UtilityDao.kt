package zm.co.tbz.goldenleaf.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.entity.NotificationEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncAuditEntity

@Dao
interface UtilityDao {
    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE is_read = 0 AND is_deleted = 0")
    fun observeUnreadCount(): Flow<Int>

    @Upsert
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("SELECT * FROM sync_audit ORDER BY started_at DESC")
    fun observeSyncAudit(): Flow<List<SyncAuditEntity>>

    @Insert
    suspend fun insertSyncAudit(audit: SyncAuditEntity): Long

    @Update
    suspend fun updateSyncAudit(audit: SyncAuditEntity)
}
