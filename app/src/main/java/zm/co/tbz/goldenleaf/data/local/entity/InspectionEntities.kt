package zm.co.tbz.goldenleaf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey val local_id: String,
    val remote_id: String? = null,
    val sync_status: String,
    val idempotency_key: String,
    val updated_at_local: Long,
    val updated_at_server: String? = null,
    val last_sync_error: String? = null,
    val conflict_category: String? = null,

    val grower_id: String,
    val grower_name: String? = null,
    val inspector_id: String? = null,
    val inspection_type: String,
    val scheduled_date: String,
    val status: String,
    val province: String? = null,
    val district: String? = null,
    val notes: String? = null
)

@Entity(tableName = "inspection_reports")
data class InspectionReportEntity(
    @PrimaryKey val local_id: String,
    val sync_status: String = "pending",
    val inspection_local_id: String? = null,
    val type: String, // VALIDATION, NURSERY, FIELD, CURING
    val data_json: String,
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "validations")
data class ValidationEntity(
    @PrimaryKey val local_id: String,
    val remote_id: String? = null,
    val sync_status: String,
    val idempotency_key: String,
    val updated_at_local: Long,
    val updated_at_server: String? = null,
    val last_sync_error: String? = null,
    val conflict_category: String? = null,

    val inspection_id: String? = null,
    val grower_id: String,
    val validated_hectarage: Double,
    val yield_per_hectare: Double,
    val risk_score: Int? = null,
    val status: String
)
