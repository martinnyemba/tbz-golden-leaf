package zm.co.tbz.goldenleaf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transport_permits")
data class TransportPermitEntity(
    @PrimaryKey val local_id: String,
    val remote_id: String? = null,
    val sync_status: String,
    val idempotency_key: String,
    val updated_at_local: Long,
    val updated_at_server: String? = null,
    val last_sync_error: String? = null,
    val conflict_category: String? = null,

    val permit_number: String? = null,
    val grower_id: String, // local_id of grower
    val grower_name: String? = null,
    val total_bales: Int,
    val total_weight_kg: Double,
    val purpose: String,
    val license_plate: String,
    val origin_province: String,
    val origin_district: String,
    val destination_sales_floor: String,
    val status: String,
    val valid_from: String? = null,
    val valid_to: String? = null,
    val qr_token: String? = null
)

@Entity(tableName = "group_permits")
data class GroupPermitEntity(
    @PrimaryKey val local_id: String,
    val remote_id: String? = null,
    val sync_status: String,
    val idempotency_key: String,
    val updated_at_local: Long,
    val updated_at_server: String? = null,
    val last_sync_error: String? = null,
    val conflict_category: String? = null,

    val permit_number: String? = null,
    val license_plate: String,
    val destination_sales_floor: String,
    val status: String,
    val entry_count: Int,
    val total_bales: Int,
    val total_weight_kg: Double,
    val qr_token: String? = null
)

@Entity(tableName = "permit_requests")
data class PermitRequestEntity(
    @PrimaryKey val local_id: String,
    val sync_status: String = "pending",
    val data_json: String,
    val created_at: Long = System.currentTimeMillis()
)
