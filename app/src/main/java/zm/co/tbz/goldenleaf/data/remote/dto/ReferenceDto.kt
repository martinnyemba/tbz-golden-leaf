package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReferenceBundleResponse(
    val provinces: List<ProvinceDto> = emptyList(),
    val districts: List<DistrictDto> = emptyList(),
    val sponsors: List<SponsorDto> = emptyList(),
    val salesfloors: List<SalesFloorDto> = emptyList(),
    val buyers: List<BuyerDto> = emptyList(),
    val tobacco_types: List<TobaccoTypeDto> = emptyList(),
    val barn_types: List<BarnTypeDto> = emptyList(),
    val version: String? = null
)

@Serializable
data class ProvinceDto(
    val id: String,
    val name: String,
    val code: String? = null
)

@Serializable
data class DistrictDto(
    val id: String,
    val name: String,
    val province_id: String
)

@Serializable
data class SponsorDto(
    val id: String,
    val name: String
)

@Serializable
data class SalesFloorDto(
    val id: String,
    val name: String,
    val location: String? = null
)

@Serializable
data class BuyerDto(
    val id: String,
    val name: String
)

@Serializable
data class TobaccoTypeDto(
    val id: String,
    val name: String,
    val code: String
)

@Serializable
data class BarnTypeDto(
    val id: String,
    val name: String
)
