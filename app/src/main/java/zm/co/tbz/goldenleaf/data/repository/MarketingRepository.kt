package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.dao.MarketingDao
import zm.co.tbz.goldenleaf.data.local.entity.BaleEntity
import zm.co.tbz.goldenleaf.data.local.entity.PendingSaleEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketingRepository @Inject constructor(
    private val marketingDao: MarketingDao
) {
    fun observeBales(): Flow<List<BaleEntity>> = marketingDao.observeBales()
    suspend fun upsertBales(bales: List<BaleEntity>) = marketingDao.upsertBales(bales)

    fun observePendingSales(): Flow<List<PendingSaleEntity>> = marketingDao.observePendingSales()
    suspend fun upsertPendingSale(sale: PendingSaleEntity) = marketingDao.upsertPendingSale(sale)
    suspend fun deletePendingSale(sale: PendingSaleEntity) = marketingDao.deletePendingSale(sale)
}
