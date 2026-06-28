package zm.co.tbz.goldenleaf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_cursors")
data class SyncCursorEntity(
    @PrimaryKey val entity_type: String,
    val updated_after: String,
    val last_success_at: Long = System.currentTimeMillis()
)
