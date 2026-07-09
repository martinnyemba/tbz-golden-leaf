package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
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
    // Present when the server rotates refresh tokens (ROTATE_REFRESH_TOKENS).
    // The old refresh token is blacklisted, so this new one MUST be persisted.
    val refresh: String? = null,
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
