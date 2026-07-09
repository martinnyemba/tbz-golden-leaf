package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val refreshMutex = Mutex()
    @Volatile private var lastRefreshSuccessAt = 0L
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

    fun observeStakeholders(): Flow<List<StakeholderEntity>> = referenceDao.observeStakeholders()

    /**
     * Fetches the full reference bundle (provinces, districts, sponsors, sales
     * floors, buyers, tobacco/crop types, barn types) and caches it locally.
     *
     * Serialized with a mutex and throttled so the several screens that ask for
     * a refresh on open don't fan out into a burst of identical requests (which
     * a single-threaded dev server drops as "connection closed"). Pass
     * [force] = true for explicit/background sync to bypass the throttle.
     *
     * Throws on failure so callers can surface the real error; the 401 path is
     * handled by the OkHttp token authenticator.
     */
    suspend fun refreshReference(force: Boolean = false) {
        refreshMutex.withLock {
            if (!force && System.currentTimeMillis() - lastRefreshSuccessAt < THROTTLE_MS) return
            val bundle = api.reference()
            referenceDao.replaceAll(
                // Provinces are keyed by their canonical code ("EASTERN"); the
                // GrowerSerializer reads `province` back as that code, so storing
                // id = code makes edit screens pre-select correctly and the sync
                // payload (province = id) round-trips through coerce_geography_attrs.
                provinces = bundle.provinces.map {
                    ProvinceEntity(id = it.code, name = it.label.ifBlank { it.code }, code = it.code)
                },
                // Districts are keyed by name (unique across Zambia's provinces) to
                // match what the server returns for `district`; province_id is the
                // parent province code so observeDistricts(provinceCode) works.
                districts = bundle.districts.map {
                    DistrictEntity(id = it.name, name = titleCase(it.name), province_id = it.province)
                },
                sponsors = bundle.sponsors.map { SponsorEntity(it.id, it.name) },
                salesFloors = bundle.salesfloors.map {
                    SalesFloorEntity(it.id, it.name, salesFloorLocation(it.district, it.province, it.address))
                },
                buyers = bundle.buyers.map { BuyerEntity(it.id, it.company_name?.ifBlank { it.name } ?: it.name) },
                // Choice references arrive as {code, label}; store code as the id so
                // dropdown selections submit the TextChoices code the server expects.
                tobaccoTypes = bundle.tobacco_types.map {
                    TobaccoTypeEntity(id = it.code, name = it.label.ifBlank { it.code }, code = it.code)
                },
                barnTypes = bundle.barn_types.map {
                    BarnTypeEntity(id = it.code, name = it.label.ifBlank { it.code })
                },
                stakeholders = bundle.stakeholders.map { StakeholderEntity(it.id, it.name, it.code) },
            )
            userPreferences.setReferenceVersion(bundle.version)
            lastRefreshSuccessAt = System.currentTimeMillis()
        }
    }

    private companion object {
        const val THROTTLE_MS = 30_000L

        fun salesFloorLocation(district: String?, province: String?, address: String?): String? {
            val parts = listOfNotNull(district?.takeIf { it.isNotBlank() }, province?.takeIf { it.isNotBlank() })
            return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
                ?: address?.takeIf { it.isNotBlank() }
        }

        fun titleCase(value: String): String =
            value.split(" ").joinToString(" ") { word ->
                if (word.isEmpty()) word
                else word[0].uppercase() + word.substring(1).lowercase()
            }
    }
}
