package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MobileProfileResponse(
    val id: String,
    val email: String,
    val full_name: String,
    val roles: List<String>,
    val permissions: Map<String, Boolean>,
    val modules: Map<String, Boolean>,
    val province: String? = null,
    val district: String? = null
)
