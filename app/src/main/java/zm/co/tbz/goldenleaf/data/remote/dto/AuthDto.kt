package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val otp: String? = null
)

@Serializable
data class LoginResponse(
    val access: String,
    val refresh: String,
    val full_name: String? = null,
    val roles: List<String> = emptyList(),
    val otp: String? = null,
    val must_change_password: Boolean = false,
    val permissions: Map<String, Boolean> = emptyMap()
)

@Serializable
data class TokenRefreshRequest(
    val refresh: String
)

@Serializable
data class TokenRefreshResponse(
    val access: String
)

@Serializable
data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

@Serializable
data class DeviceRegistrationRequest(
    val registration_id: String,
    val type: String = "ANDROID"
)
