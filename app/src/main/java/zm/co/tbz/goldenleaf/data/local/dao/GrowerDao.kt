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

    @Query("SELECT * FROM growers WHERE local_id = :localId")
    suspend fun getGrowerById(localId: String): GrowerEntity?

    @Query("SELECT * FROM grower_registrations WHERE local_id = :localId")
    suspend fun getRegistrationById(localId: String): GrowerRegistrationEntity?

    @Query("SELECT * FROM grower_registrations WHERE local_id = :localId")
    fun observeRegistrationById(localId: String): Flow<GrowerRegistrationEntity?>

    @Upsert
    suspend fun upsertGrower(grower: GrowerEntity)

    @Upsert
    suspend fun upsertGrowers(growers: List<GrowerEntity>)

    @Query("SELECT * FROM grower_registrations ORDER BY created_at DESC")
    fun observeRegistrations(): Flow<List<GrowerRegistrationEntity>>

    @Upsert
    suspend fun upsertRegistration(registration: GrowerRegistrationEntity)

    @Delete
    suspend fun deleteRegistration(registration: GrowerRegistrationEntity): Int

    @Query("SELECT * FROM grower_edits WHERE local_id = :localId")
    suspend fun getEditById(localId: String): GrowerEditEntity?

    @Query("SELECT * FROM grower_edits ORDER BY created_at DESC")
    fun observeEdits(): Flow<List<GrowerEditEntity>>

    @Upsert
    suspend fun upsertEdit(edit: GrowerEditEntity)

    @Delete
    suspend fun deleteEdit(edit: GrowerEditEntity): Int
}
