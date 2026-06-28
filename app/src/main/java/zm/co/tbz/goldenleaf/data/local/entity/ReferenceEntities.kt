package zm.co.tbz.goldenleaf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reference_provinces")
data class ProvinceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String? = null
)

@Entity(tableName = "reference_districts")
data class DistrictEntity(
    @PrimaryKey val id: String,
    val name: String,
    val province_id: String
)

@Entity(tableName = "reference_sponsors")
data class SponsorEntity(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(tableName = "reference_salesfloors")
data class SalesFloorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String? = null
)

@Entity(tableName = "reference_buyers")
data class BuyerEntity(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(tableName = "reference_tobacco_types")
data class TobaccoTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String
)

@Entity(tableName = "reference_barn_types")
data class BarnTypeEntity(
    @PrimaryKey val id: String,
    val name: String
)
