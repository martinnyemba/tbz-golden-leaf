package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.dao.PermitDao
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitEntity
import zm.co.tbz.goldenleaf.data.local.entity.PermitRequestEntity
import zm.co.tbz.goldenleaf.data.local.entity.TransportPermitEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermitRepository @Inject constructor(
    private val permitDao: PermitDao
) {
    fun observeTransportPermits(): Flow<List<TransportPermitEntity>> = permitDao.observeTransportPermits()
    suspend fun upsertTransportPermits(permits: List<TransportPermitEntity>) = permitDao.upsertTransportPermits(permits)

    fun observeGroupPermits(): Flow<List<GroupPermitEntity>> = permitDao.observeGroupPermits()
    suspend fun upsertGroupPermits(permits: List<GroupPermitEntity>) = permitDao.upsertGroupPermits(permits)

    fun observePermitRequests(): Flow<List<PermitRequestEntity>> = permitDao.observePermitRequests()
    suspend fun upsertPermitRequest(request: PermitRequestEntity) = permitDao.upsertPermitRequest(request)
    suspend fun deletePermitRequest(request: PermitRequestEntity) = permitDao.deletePermitRequest(request)
}
