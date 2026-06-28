package zm.co.tbz.goldenleaf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "growers")
data class GrowerEntity(
    @PrimaryKey val local_id: String,
    val remote_id: String? = null,
    val sync_status: String,
    val idempotency_key: String,
    val updated_at_local: Long,
    val updated_at_server: String? = null,
    val last_sync_error: String? = null,
    val conflict_category: String? = null,
    
    val tbz_id: String? = null,
    val first_name: String,
    val middle_name: String? = null,
    val last_name: String,
    val nrc_number: String,
    val sex: String? = null,
    val date_of_birth: String? = null,
    val category: String? = null,
    val phone_number: String? = null,
    val email: String? = null,
    val address: String? = null,
    val town_village: String? = null,
    val province: String? = null,
    val district: String? = null,
    val gps_latitude: Double? = null,
    val gps_longitude: Double? = null,
    val profile_photo_path: String? = null,
    val id_front_path: String? = null,
    val id_back_path: String? = null,
    val status: String
)

@Entity(tableName = "grower_registrations")
data class GrowerRegistrationEntity(
    @PrimaryKey val local_id: String,
    val sync_status: String = "pending",
    val data_json: String, // Full registration payload
    val created_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "grower_edits")
data class GrowerEditEntity(
    @PrimaryKey val local_id: String,
    val grower_local_id: String,
    val sync_status: String = "pending",
    val patch_json: String,
    val created_at: Long = System.currentTimeMillis()
)
