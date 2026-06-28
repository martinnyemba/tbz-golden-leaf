package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.dao.UtilityDao
import zm.co.tbz.goldenleaf.data.local.entity.NotificationEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncAuditEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UtilityRepository @Inject constructor(
    private val utilityDao: UtilityDao
) {
    fun observeNotifications(): Flow<List<NotificationEntity>> = utilityDao.observeNotifications()
    fun observeUnreadCount(): Flow<Int> = utilityDao.observeUnreadCount()
    suspend fun upsertNotifications(notifications: List<NotificationEntity>) = utilityDao.upsertNotifications(notifications)
    suspend fun markAsRead(id: String) = utilityDao.markAsRead(id)

    fun observeSyncAudit(): Flow<List<SyncAuditEntity>> = utilityDao.observeSyncAudit()
    suspend fun insertSyncAudit(audit: SyncAuditEntity): Long = utilityDao.insertSyncAudit(audit)
    suspend fun updateSyncAudit(audit: SyncAuditEntity) = utilityDao.updateSyncAudit(audit)
}
