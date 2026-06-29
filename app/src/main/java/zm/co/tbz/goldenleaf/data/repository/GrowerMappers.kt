package zm.co.tbz.goldenleaf.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.remote.dto.GrowerDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun GrowerDto.toEntity(existing: GrowerEntity? = null): GrowerEntity {
    val preserveLocal = existing?.sync_status == SyncStatuses.PENDING &&
        existing.remote_id == null
    return GrowerEntity(
        local_id = existing?.local_id ?: id,
        remote_id = id,
        sync_status = if (preserveLocal) existing!!.sync_status else SyncStatuses.SYNCED,
        idempotency_key = existing?.idempotency_key ?: id,
        updated_at_local = System.currentTimeMillis(),
        updated_at_server = updated_at,
        last_sync_error = existing?.last_sync_error,
        conflict_category = existing?.conflict_category,
        tbz_id = tbz_id,
        grower_type = grower_type,
        first_name = first_name,
        middle_name = middle_name,
        last_name = last_name,
        nrc_number = nrc_number.orEmpty(),
        sex = sex,
        date_of_birth = date_of_birth,
        category = category,
        phone_number = phone_number,
        email = email,
        address = address,
        town_village = town_or_village,
        province = province,
        district = district,
        gps_latitude = gps_latitude,
        gps_longitude = gps_longitude,
        profile_photo_path = existing?.profile_photo_path,
        id_front_path = existing?.id_front_path,
        id_back_path = existing?.id_back_path,
        status = status,
        correction_reason = correction_reason,
        correction_requested_at = correction_requested_at,
        correction_reviewer_name = correction_reviewer_name,
    )
}

internal fun changedFieldLabels(patchJson: String, json: Json): String {
    return runCatching {
        json.parseToJsonElement(patchJson).jsonObject.keys
            .map { key ->
                when (key) {
                    "first_name" -> "First name"
                    "last_name" -> "Last name"
                    "nrc_number" -> "NRC"
                    "phone_number" -> "Phone"
                    "province", "district" -> "Location"
                    "address", "town_or_village" -> "Address"
                    else -> key.replace('_', ' ').replaceFirstChar { it.titlecase() }
                }
            }
            .distinct()
            .joinToString(", ")
    }.getOrDefault("Profile fields")
}

internal fun formatEditTimestamp(epochMs: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMs))
}

internal fun apiStatusForFilter(filter: String): String? = when (filter) {
    "All" -> null
    "Draft" -> "DRAFT"
    "Pending" -> "PENDING"
    "Returned" -> "RETURNED_FOR_CORRECTION"
    "Approved" -> "APPROVED"
    "Active" -> "ACTIVE"
    "Rejected" -> "REJECTED"
    "Suspended" -> "SUSPENDED"
    else -> null
}

internal fun growerTypeFromWizardKey(key: String): Pair<String, String> = when (key) {
    "commercial" -> "COMMERCIAL" to "INDIVIDUAL"
    "company" -> "COMPANY" to "COMPANY"
    else -> "SMALL_SCALE" to "INDIVIDUAL"
}
