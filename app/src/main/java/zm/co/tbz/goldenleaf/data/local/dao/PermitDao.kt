package zm.co.tbz.goldenleaf.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitEntity
import zm.co.tbz.goldenleaf.data.local.entity.PermitRequestEntity
import zm.co.tbz.goldenleaf.data.local.entity.TransportPermitEntity

@Dao
interface PermitDao {
    @Query("SELECT * FROM transport_permits ORDER BY updated_at_local DESC")
    fun observeTransportPermits(): Flow<List<TransportPermitEntity>>

    @Upsert
    suspend fun upsertTransportPermits(permits: List<TransportPermitEntity>)

    @Query("SELECT * FROM group_permits ORDER BY updated_at_local DESC")
    fun observeGroupPermits(): Flow<List<GroupPermitEntity>>

    @Upsert
    suspend fun upsertGroupPermits(permits: List<GroupPermitEntity>)

    @Query("SELECT * FROM permit_requests ORDER BY created_at DESC")
    fun observePermitRequests(): Flow<List<PermitRequestEntity>>

    @Upsert
    suspend fun upsertPermitRequest(request: PermitRequestEntity)

    @Delete
    suspend fun deletePermitRequest(request: PermitRequestEntity): Int
}
