package zm.co.tbz.goldenleaf.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.entity.*

@Dao
interface ReferenceDao {
    @Query("SELECT * FROM reference_provinces ORDER BY name")
    fun observeProvinces(): Flow<List<ProvinceEntity>>

    @Upsert
    suspend fun upsertProvinces(provinces: List<ProvinceEntity>)

    @Query("SELECT * FROM reference_districts WHERE province_id = :provinceId ORDER BY name")
    fun observeDistricts(provinceId: String): Flow<List<DistrictEntity>>

    @Upsert
    suspend fun upsertDistricts(districts: List<DistrictEntity>)

    @Query("SELECT * FROM reference_sponsors ORDER BY name")
    fun observeSponsors(): Flow<List<SponsorEntity>>

    @Upsert
    suspend fun upsertSponsors(sponsors: List<SponsorEntity>)

    @Query("SELECT * FROM reference_salesfloors ORDER BY name")
    fun observeSalesFloors(): Flow<List<SalesFloorEntity>>

    @Upsert
    suspend fun upsertSalesFloors(salesFloors: List<SalesFloorEntity>)

    @Query("SELECT * FROM reference_buyers ORDER BY name")
    fun observeBuyers(): Flow<List<BuyerEntity>>

    @Upsert
    suspend fun upsertBuyers(buyers: List<BuyerEntity>)

    @Query("SELECT * FROM reference_tobacco_types ORDER BY name")
    fun observeTobaccoTypes(): Flow<List<TobaccoTypeEntity>>

    @Upsert
    suspend fun upsertTobaccoTypes(types: List<TobaccoTypeEntity>)

    @Query("SELECT * FROM reference_barn_types ORDER BY name")
    fun observeBarnTypes(): Flow<List<BarnTypeEntity>>

    @Upsert
    suspend fun upsertBarnTypes(types: List<BarnTypeEntity>)

    @Query("SELECT * FROM reference_stakeholders ORDER BY name")
    fun observeStakeholders(): Flow<List<StakeholderEntity>>

    @Upsert
    suspend fun upsertStakeholders(stakeholders: List<StakeholderEntity>)

    // ── Clears (used by the atomic bundle replace below) ──────────────────────
    @Query("DELETE FROM reference_provinces") suspend fun clearProvinces()
    @Query("DELETE FROM reference_districts") suspend fun clearDistricts()
    @Query("DELETE FROM reference_sponsors") suspend fun clearSponsors()
    @Query("DELETE FROM reference_salesfloors") suspend fun clearSalesFloors()
    @Query("DELETE FROM reference_buyers") suspend fun clearBuyers()
    @Query("DELETE FROM reference_tobacco_types") suspend fun clearTobaccoTypes()
    @Query("DELETE FROM reference_barn_types") suspend fun clearBarnTypes()
    @Query("DELETE FROM reference_stakeholders") suspend fun clearStakeholders()

    /**
     * Replaces every reference table from one bundle in a single transaction so
     * rows the portal no longer returns (a deactivated sales floor, a buyer that
     * lost its licence) disappear instead of lingering, and observers never see a
     * half-applied bundle. A section omitted by permission scoping arrives as an
     * empty list and clears that table — which is correct: the user can't pick
     * from data they're not allowed to see.
     */
    @Transaction
    suspend fun replaceAll(
        provinces: List<ProvinceEntity>,
        districts: List<DistrictEntity>,
        sponsors: List<SponsorEntity>,
        salesFloors: List<SalesFloorEntity>,
        buyers: List<BuyerEntity>,
        tobaccoTypes: List<TobaccoTypeEntity>,
        barnTypes: List<BarnTypeEntity>,
        stakeholders: List<StakeholderEntity>,
    ) {
        clearProvinces(); upsertProvinces(provinces)
        clearDistricts(); upsertDistricts(districts)
        clearSponsors(); upsertSponsors(sponsors)
        clearSalesFloors(); upsertSalesFloors(salesFloors)
        clearBuyers(); upsertBuyers(buyers)
        clearTobaccoTypes(); upsertTobaccoTypes(tobaccoTypes)
        clearBarnTypes(); upsertBarnTypes(barnTypes)
        clearStakeholders(); upsertStakeholders(stakeholders)
    }
}
