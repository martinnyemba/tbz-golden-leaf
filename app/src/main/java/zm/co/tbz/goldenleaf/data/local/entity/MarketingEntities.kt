package zm.co.tbz.goldenleaf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bales")
data class BaleEntity(
    @PrimaryKey val local_id: String,
    val remote_id: String? = null,
    val sync_status: String,
    val idempotency_key: String,
    val updated_at_local: Long,
    val updated_at_server: String? = null,
    val last_sync_error: String? = null,
    val conflict_category: String? = null,

    val grower_id: String,
    val permit_number: String? = null,
    val ticket_number: String,
    val grade_mark: String,
    val weight_kg: Double,
    val status: String, // BOUGHT, REJECTED
    val sale_date: String
)

@Entity(tableName = "pending_sales")
data class PendingSaleEntity(
    @PrimaryKey val local_id: String,
    val sync_status: String = "pending",
    val permit_token: String,
    val data_json: String, // Batch info + bale rows
    val created_at: Long = System.currentTimeMillis()
)
