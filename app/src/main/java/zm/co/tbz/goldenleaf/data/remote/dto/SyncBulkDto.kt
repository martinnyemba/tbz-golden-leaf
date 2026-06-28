package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SyncBulkRequest(
    val items: List<SyncItemDto>
)

@Serializable
data class SyncItemDto(
    val client_id: String,
    val idempotency_key: String? = null,
    val operation: String, // POST, PATCH
    val endpoint: String,
    val payload: JsonElement,
    val queued_at: String? = null
)

@Serializable
data class SyncBulkResponse(
    val results: List<SyncItemResultDto>
)

@Serializable
data class SyncItemResultDto(
    val client_id: String,
    val status: Int,
    val server_id: String? = null,
    val server_data: JsonElement? = null,
    val error: String? = null,
    val replay: Boolean = false
)
