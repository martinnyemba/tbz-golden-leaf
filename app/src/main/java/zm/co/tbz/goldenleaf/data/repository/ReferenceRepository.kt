package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.dao.ReferenceDao
import zm.co.tbz.goldenleaf.data.local.entity.*
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceRepository @Inject constructor(
    private val referenceDao: ReferenceDao,
    private val api: TrmcsApi,
    private val userPreferences: UserPreferences,
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

    /**
     * Fetches the full reference bundle (provinces, districts, sponsors, sales
     * floors, buyers, tobacco/crop types, barn types) and caches it locally.
     * Throws on failure so callers can surface the real error; the 401 path is
     * handled by the OkHttp token authenticator so an expired session is
     * refreshed and the call retried transparently.
     */
    suspend fun refreshReference() {
        val bundle = api.reference()
        upsertProvinces(bundle.provinces.map { ProvinceEntity(it.id, it.name, it.code) })
        upsertDistricts(bundle.districts.map { DistrictEntity(it.id, it.name, it.province_id) })
        upsertSponsors(bundle.sponsors.map { SponsorEntity(it.id, it.name) })
        upsertSalesFloors(bundle.salesfloors.map { SalesFloorEntity(it.id, it.name, it.location) })
        upsertBuyers(bundle.buyers.map { BuyerEntity(it.id, it.name) })
        upsertTobaccoTypes(bundle.tobacco_types.map { TobaccoTypeEntity(it.id, it.name, it.code) })
        upsertBarnTypes(bundle.barn_types.map { BarnTypeEntity(it.id, it.name) })
        userPreferences.setReferenceVersion(bundle.version)
    }
}
