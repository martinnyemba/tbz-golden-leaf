package zm.co.tbz.goldenleaf.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.entity.*

@Dao
interface ReferenceDao {
    @Query("SELECT * FROM reference_provinces")
    fun observeProvinces(): Flow<List<ProvinceEntity>>

    @Upsert
    suspend fun upsertProvinces(provinces: List<ProvinceEntity>)

    @Query("SELECT * FROM reference_districts WHERE province_id = :provinceId")
    fun observeDistricts(provinceId: String): Flow<List<DistrictEntity>>

    @Upsert
    suspend fun upsertDistricts(districts: List<DistrictEntity>)

    @Query("SELECT * FROM reference_sponsors")
    fun observeSponsors(): Flow<List<SponsorEntity>>

    @Upsert
    suspend fun upsertSponsors(sponsors: List<SponsorEntity>)

    @Query("SELECT * FROM reference_salesfloors")
    fun observeSalesFloors(): Flow<List<SalesFloorEntity>>

    @Upsert
    suspend fun upsertSalesFloors(salesFloors: List<SalesFloorEntity>)

    @Query("SELECT * FROM reference_buyers")
    fun observeBuyers(): Flow<List<BuyerEntity>>

    @Upsert
    suspend fun upsertBuyers(buyers: List<BuyerEntity>)

    @Query("SELECT * FROM reference_tobacco_types")
    fun observeTobaccoTypes(): Flow<List<TobaccoTypeEntity>>

    @Upsert
    suspend fun upsertTobaccoTypes(types: List<TobaccoTypeEntity>)

    @Query("SELECT * FROM reference_barn_types")
    fun observeBarnTypes(): Flow<List<BarnTypeEntity>>

    @Upsert
    suspend fun upsertBarnTypes(types: List<BarnTypeEntity>)
}
