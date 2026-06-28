package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PagedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

@Serializable
data class GrowerDto(
    val id: String,
    val tbz_id: String? = null,
    val first_name: String,
    val last_name: String,
    val nrc_number: String? = null,
    val status: String,
    val province: String? = null,
    val district: String? = null
)

@Serializable
data class TransportPermitDto(
    val id: String,
    val permit_number: String,
    val status: String,
    val total_bales: Int,
    val total_weight_kg: Double,
    val grower_name: String? = null
)

@Serializable
data class InspectionDto(
    val id: String,
    val inspection_type: String,
    val status: String,
    val scheduled_date: String,
    val grower_name: String? = null
)

@Serializable
data class VerifyQrRequest(
    val permit_token: String,
    val salesfloor_id: String? = null
)

@Serializable
data class VerifyQrResponse(
    val valid: Boolean,
    val permit_number: String? = null,
    val grower_name: String? = null,
    val tbz_id: String? = null,
    val total_bales: Int? = null,
    val remaining_bales: Int? = null,
    val status: String? = null
)

@Serializable
data class BulkBaleCreateRequest(
    val grower: String,
    val season: String,
    val permit_token: String,
    val bales: List<BaleDto>
)

@Serializable
data class BaleDto(
    val bale_ticket_number: String,
    val grade_mark: String,
    val weight_kg: Double,
    val status: String, // BOUGHT, REJECTED
    val rejection_reason: String? = null,
    val buyer: String? = null,
    val sale_date: String? = null
)
