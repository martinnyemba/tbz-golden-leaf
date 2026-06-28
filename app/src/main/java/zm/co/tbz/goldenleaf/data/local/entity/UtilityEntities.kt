package zm.co.tbz.goldenleaf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val channel: String,
    val subject: String,
    val message: String,
    val status: String,
    val reference: String? = null,
    val created_at: String,
    val is_read: Boolean = false,
    val is_favorite: Boolean = false,
    val is_archived: Boolean = false,
    val is_deleted: Boolean = false
)

@Entity(tableName = "sync_audit")
data class SyncAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val started_at: Long,
    val finished_at: Long? = null,
    val status: String, // success, failure, partial
    val items_synced: Int = 0,
    val items_failed: Int = 0,
    val error_log: String? = null
)
