package zm.co.tbz.goldenleaf.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import zm.co.tbz.goldenleaf.data.local.entity.GrowerDocumentUploadEntity

@Dao
interface GrowerDocumentDao {
    @Query(
        "SELECT * FROM grower_document_uploads WHERE grower_local_id = :growerLocalId " +
            "AND sync_status != 'synced' ORDER BY created_at ASC",
    )
    suspend fun getPendingForGrower(growerLocalId: String): List<GrowerDocumentUploadEntity>

    @Query("SELECT * FROM grower_document_uploads WHERE sync_status != 'synced' ORDER BY created_at ASC")
    suspend fun getAllPending(): List<GrowerDocumentUploadEntity>

    @Upsert
    suspend fun upsert(upload: GrowerDocumentUploadEntity)

    @Upsert
    suspend fun upsertAll(uploads: List<GrowerDocumentUploadEntity>)
}
