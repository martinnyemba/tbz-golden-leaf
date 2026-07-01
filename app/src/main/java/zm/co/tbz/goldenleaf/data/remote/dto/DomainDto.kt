package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
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
    val grower_type: String? = null,
    val first_name: String,
    val middle_name: String? = null,
    val last_name: String,
    val nrc_number: String? = null,
    val sex: String? = null,
    val date_of_birth: String? = null,
    val category: String? = null,
    val phone_number: String? = null,
    val email: String? = null,
    val address: String? = null,
    val town_or_village: String? = null,
    val province: String? = null,
    val district: String? = null,
    val gps_latitude: Double? = null,
    val gps_longitude: Double? = null,
    val status: String,
    val correction_reason: String? = null,
    val correction_requested_at: String? = null,
    val correction_reviewer_name: String? = null,
    val updated_at: String? = null,
)

@Serializable
data class TransportPermitDto(
    val id: String,
    val permit_number: String? = null,
    val status: String = "",
    val total_bales: Int = 0,
    val total_weight_kg: Double = 0.0,
    val grower_name: String? = null,
    val license_plate: String? = null,
    val origin_province: String? = null,
    val origin_district: String? = null,
    val destination_sales_floor: String? = null,
    val purpose: String? = null,
    val valid_from: String? = null,
    val valid_to: String? = null,
    val correction_reason: String? = null,
    val rejection_reason: String? = null,
    val comments: String? = null,
    val grower_category: String? = null,
    val buyer: String? = null,
    val is_bought: Boolean? = null,
    val buyer_accepted: Boolean? = null,
    // Signed QR token, populated by the server once the permit is approved/issued.
    val qr_code_data: String? = null,
)

@Serializable
data class GroupPermitDto(
    val id: String,
    val group_permit_number: String? = null,
    val license_plate: String = "",
    val origin_province: String? = null,
    val origin_district: String? = null,
    val destination_sales_floor: String = "",
    val purpose: String? = null,
    val status: String = "",
    val total_bales: Int = 0,
    val total_weight_kg: Double = 0.0,
    val valid_from: String? = null,
    val valid_to: String? = null,
    val correction_reason: String? = null,
    val rejection_reason: String? = null,
    val comments: String? = null,
    val entries: List<GroupPermitEntryDto>? = null,
    // Signed QR token, populated by the server once the group permit is approved/issued.
    val qr_code_data: String? = null,
)

@Serializable
data class GroupPermitEntryDto(
    val id: String,
    val grower_name: String? = null,
    val grower_tbz_id: String? = null,
    val total_bales: Int = 0,
    val total_weight_kg: Double = 0.0,
    val status: String = "",
    val grower_category: String? = null,
)

@Serializable
data class GroupPermitCreatedResponse(
    val id: String,
    val status: String? = null,
)

@Serializable
data class GroupPermitValidateRequest(
    val permit_token: String,
)

@Serializable
data class GroupPermitValidateResponse(
    val valid: Boolean,
    val detail: String? = null,
    val group_permit_number: String? = null,
    val license_plate: String? = null,
    val destination_sales_floor: String? = null,
    val purpose: String? = null,
    val total_bales: Int? = null,
    val total_weight_kg: Double? = null,
    val valid_from: String? = null,
    val valid_to: String? = null,
    val status: String? = null,
    val entries: List<GroupPermitEntryDto>? = null,
)

@Serializable
data class PermitApproveRequest(
    val action: String,
    val valid_from: String? = null,
    val valid_to: String? = null,
    val reason: String? = null,
)

// ── Grower profile related records (Inspections / Permits / Sales tabs) ──────
@Serializable
data class GrowerRelatedResponse(
    val inspections: List<GrowerInspectionDto> = emptyList(),
    val permits: List<GrowerPermitDto> = emptyList(),
    val sales: List<GrowerSaleDto> = emptyList(),
)

@Serializable
data class GrowerInspectionDto(
    val id: String,
    val inspection_type: String = "",
    val inspection_type_display: String = "",
    val status: String = "",
    val scheduled_date: String? = null,
)

@Serializable
data class GrowerPermitDto(
    val id: String,
    val permit_number: String? = null,
    val status: String = "",
    val purpose: String = "",
    val total_bales: Int = 0,
    val total_weight_kg: Double = 0.0,
    val destination_sales_floor: String = "",
    val valid_from: String? = null,
    val valid_to: String? = null,
)

@Serializable
data class GrowerSaleDto(
    val id: String,
    val bale_ticket_number: String = "",
    val grade_mark: String = "",
    val weight_kg: Double = 0.0,
    val status: String = "",
    val season: String = "",
    val sale_date: String? = null,
    val salesfloor: String = "",
)

@Serializable
data class InspectionDto(
    val id: String,
    val inspection_type: String = "",
    val status: String = "",
    val scheduled_date: String = "",
    val grower_name: String? = null
)

@Serializable
data class VerifyQrRequest(
    val permit_token: String,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val salesfloor_id: String? = null,
)

@Serializable
data class VerifyQrResponse(
    val valid: Boolean,
    val permit_number: String? = null,
    val grower_name: String? = null,
    // The verify-qr endpoint returns this as `grower_tbz_id`.
    @SerialName("grower_tbz_id")
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
