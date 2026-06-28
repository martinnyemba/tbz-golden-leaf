package zm.co.tbz.goldenleaf.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.entity.BaleEntity
import zm.co.tbz.goldenleaf.data.local.entity.PendingSaleEntity

@Dao
interface MarketingDao {
    @Query("SELECT * FROM bales ORDER BY sale_date DESC")
    fun observeBales(): Flow<List<BaleEntity>>

    @Upsert
    suspend fun upsertBales(bales: List<BaleEntity>)

    @Query("SELECT * FROM pending_sales ORDER BY created_at DESC")
    fun observePendingSales(): Flow<List<PendingSaleEntity>>

    @Upsert
    suspend fun upsertPendingSale(sale: PendingSaleEntity)

    @Delete
    suspend fun deletePendingSale(sale: PendingSaleEntity): Int
}
