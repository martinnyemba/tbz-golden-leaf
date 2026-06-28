package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncStatusResponse(
    val server_time: String,
    val reference_version: String,
    val api_version: String
)
