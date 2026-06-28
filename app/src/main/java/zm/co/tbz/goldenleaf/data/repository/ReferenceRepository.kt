package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.dao.ReferenceDao
import zm.co.tbz.goldenleaf.data.local.entity.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceRepository @Inject constructor(
    private val referenceDao: ReferenceDao
) {
    fun observeProvinces(): Flow<List<ProvinceEntity>> = referenceDao.observeProvinces()
    suspend fun upsertProvinces(provinces: List<ProvinceEntity>) = referenceDao.upsertProvinces(provinces)

    fun observeDistricts(provinceId: String): Flow<List<DistrictEntity>> = referenceDao.observeDistricts(provinceId)
    suspend fun upsertDistricts(districts: List<DistrictEntity>) = referenceDao.upsertDistricts(districts)

    fun observeSponsors(): Flow<List<SponsorEntity>> = referenceDao.observeSponsors()
    suspend fun upsertSponsors(sponsors: List<SponsorEntity>) = referenceDao.upsertSponsors(sponsors)

    fun observeSalesFloors(): Flow<List<SalesFloorEntity>> = referenceDao.observeSalesFloors()
    suspend fun upsertSalesFloors(salesFloors: List<SalesFloorEntity>) = referenceDao.upsertSalesFloors(salesFloors)

    fun observeBuyers(): Flow<List<BuyerEntity>> = referenceDao.observeBuyers()
    suspend fun upsertBuyers(buyers: List<BuyerEntity>) = referenceDao.upsertBuyers(buyers)

    fun observeTobaccoTypes(): Flow<List<TobaccoTypeEntity>> = referenceDao.observeTobaccoTypes()
    suspend fun upsertTobaccoTypes(types: List<TobaccoTypeEntity>) = referenceDao.upsertTobaccoTypes(types)

    fun observeBarnTypes(): Flow<List<BarnTypeEntity>> = referenceDao.observeBarnTypes()
    suspend fun upsertBarnTypes(types: List<BarnTypeEntity>) = referenceDao.upsertBarnTypes(types)
}
