package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Mirrors the payload returned by `GET /api/v1/mobile/reference/`
 * (apps/mobile_api/services/reference_service.py :: build_full_reference_payload).
 *
 * Every list defaults to empty and every non-key field is nullable so the
 * bundle still parses when the portal scopes sections out by permission
 * (e.g. sponsors/salesfloors/buyers/grades are dropped for users without the
 * relevant capability) or omits a field. A single missing key must never blow
 * up the whole bundle and leave the app with no dropdown data.
 */
@Serializable
data class ReferenceBundleResponse(
    val version: String? = null,
    val generated_at: String? = null,
    val provinces: List<ProvinceDto> = emptyList(),
    val districts: List<DistrictDto> = emptyList(),
    val sponsors: List<SponsorDto> = emptyList(),
    val salesfloors: List<SalesFloorDto> = emptyList(),
    val buyers: List<BuyerDto> = emptyList(),
    val tobacco_types: List<CodeLabelDto> = emptyList(),
    val barn_types: List<CodeLabelDto> = emptyList(),
    val stakeholders: List<StakeholderDto> = emptyList(),
)

/** Portal sends provinces as `{code, label}` (province code is the canonical key). */
@Serializable
data class ProvinceDto(
    val code: String,
    val label: String = "",
)

/** Portal sends districts as `{code, name, province}` where `province` is the province code. */
@Serializable
data class DistrictDto(
    val name: String,
    val province: String,
    val code: String? = null,
)

@Serializable
data class SponsorDto(
    val id: String,
    val name: String = "",
    val code: String? = null,
)

@Serializable
data class SalesFloorDto(
    val id: String,
    val name: String = "",
    val code: String? = null,
    val province: String? = null,
    val district: String? = null,
    val address: String? = null,
)

@Serializable
data class BuyerDto(
    val id: String,
    val name: String = "",
    val company_name: String? = null,
    val code: String? = null,
    val contact_phone: String? = null,
)

/** Choice-style references the portal emits as `{code, label}`. */
@Serializable
data class CodeLabelDto(
    val code: String,
    val label: String = "",
)

@Serializable
data class StakeholderDto(
    val id: String,
    val name: String = "",
    val code: String? = null,
)

/**
 * Response of `GET /api/v1/mobile/reference/price-matrix/?buyer=&season=[&tobacco_type=]`.
 * Approved grades are owned per buyer, so the bale-capture grade picker scopes
 * to the batch's buyer + season and groups the rows by tobacco type, mirroring
 * the web wizard's `marketing_price_matrix_lookup`.
 */
@Serializable
data class PriceMatrixResponse(
    val grades: List<PriceMatrixGradeDto> = emptyList(),
)

@Serializable
data class PriceMatrixGradeDto(
    val grade: String,
    val tobacco_type: String = "",
    val price_per_kg: String = "",
)
