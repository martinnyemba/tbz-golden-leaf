package zm.co.tbz.goldenleaf.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEditEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerRegistrationEntity

@Dao
interface GrowerDao {
    @Query("SELECT * FROM growers ORDER BY updated_at_local DESC")
    fun observeGrowers(): Flow<List<GrowerEntity>>

    @Query("SELECT * FROM growers WHERE local_id = :localId")
    fun observeGrowerById(localId: String): Flow<GrowerEntity?>

    @Upsert
    suspend fun upsertGrower(grower: GrowerEntity)

    @Upsert
    suspend fun upsertGrowers(growers: List<GrowerEntity>)

    @Query("SELECT * FROM grower_registrations ORDER BY created_at DESC")
    fun observeRegistrations(): Flow<List<GrowerRegistrationEntity>>

    @Upsert
    suspend fun upsertRegistration(registration: GrowerRegistrationEntity)

    @Delete
    suspend fun deleteRegistration(registration: GrowerRegistrationEntity)

    @Query("SELECT * FROM grower_edits ORDER BY created_at DESC")
    fun observeEdits(): Flow<List<GrowerEditEntity>>

    @Upsert
    suspend fun upsertEdit(edit: GrowerEditEntity)

    @Delete
    suspend fun deleteEdit(edit: GrowerEditEntity)
}
