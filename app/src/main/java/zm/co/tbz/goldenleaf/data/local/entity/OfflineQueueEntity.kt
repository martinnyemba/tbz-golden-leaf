package zm.co.tbz.goldenleaf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_queue")
data class OfflineQueueEntity(
    @PrimaryKey val local_id: String, // UUID
    val remote_id: String? = null,
    val sync_status: String, // pending, syncing, synced, failed, needs_review, blocked
    val idempotency_key: String,
    val operation: String, // POST, PATCH
    val endpoint: String,
    val payload_json: String,
    val queued_at: Long = System.currentTimeMillis(),
    val updated_at_local: Long = System.currentTimeMillis(),
    val updated_at_server: String? = null,
    val last_sync_error: String? = null,
    val conflict_category: String? = null,
    val retry_count: Int = 0,
    val next_retry_at: Long? = null
)
