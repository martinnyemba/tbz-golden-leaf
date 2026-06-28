package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val otp: String? = null,
)

@Serializable
data class LoginResponse(
    val access: String,
    val refresh: String,
)

@Serializable
data class TokenRefreshRequest(
    val refresh: String,
)

@Serializable
data class TokenRefreshResponse(
    val access: String,
)

@Serializable
data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String,
)

@Serializable
data class DeviceRegistrationRequest(
    val registration_id: String,
    val type: String = "ANDROID",
)

@Serializable
data class ApiErrorEnvelope(
    val success: Boolean = false,
    val error: ApiErrorBody? = null,
    val detail: String? = null,
    val otp: String? = null,
)

@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val details: String? = null,
)
