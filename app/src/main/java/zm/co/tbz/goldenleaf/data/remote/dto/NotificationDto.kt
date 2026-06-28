package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val channel: String,
    val subject: String,
    val message: String,
    val status: String,
    val reference: String? = null,
    val created_at: String,
    val sent_at: String? = null
)
