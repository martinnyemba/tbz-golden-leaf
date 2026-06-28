package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.dao.GrowerDao
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEditEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerRegistrationEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrowerRepository @Inject constructor(
    private val growerDao: GrowerDao
) {
    fun observeGrowers(): Flow<List<GrowerEntity>> = growerDao.observeGrowers()
    fun observeGrowerById(localId: String): Flow<GrowerEntity?> = growerDao.observeGrowerById(localId)
    suspend fun upsertGrower(grower: GrowerEntity) = growerDao.upsertGrower(grower)
    suspend fun upsertGrowers(growers: List<GrowerEntity>) = growerDao.upsertGrowers(growers)

    fun observeRegistrations(): Flow<List<GrowerRegistrationEntity>> = growerDao.observeRegistrations()
    suspend fun upsertRegistration(registration: GrowerRegistrationEntity) = growerDao.upsertRegistration(registration)
    suspend fun deleteRegistration(registration: GrowerRegistrationEntity) = growerDao.deleteRegistration(registration)

    fun observeEdits(): Flow<List<GrowerEditEntity>> = growerDao.observeEdits()
    suspend fun upsertEdit(edit: GrowerEditEntity) = growerDao.upsertEdit(edit)
    suspend fun deleteEdit(edit: GrowerEditEntity) = growerDao.deleteEdit(edit)
}
