package zm.co.tbz.goldenleaf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grower_document_uploads")
data class GrowerDocumentUploadEntity(
    @PrimaryKey val local_id: String,
    val grower_local_id: String,
    val grower_remote_id: String? = null,
    val field_name: String,
    val file_path: String,
    val sync_status: String,
    val last_sync_error: String? = null,
    val created_at: Long = System.currentTimeMillis(),
) {
    companion object {
        const val FIELD_PROFILE = "profile_photo"
        const val FIELD_ID_FRONT = "id_front"
        const val FIELD_ID_BACK = "id_back"
    }
}
