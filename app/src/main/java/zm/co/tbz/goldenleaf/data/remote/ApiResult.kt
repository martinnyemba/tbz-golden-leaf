package zm.co.tbz.goldenleaf.data.remote

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import zm.co.tbz.goldenleaf.data.remote.dto.ApiErrorEnvelope

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val requiresOtp: Boolean = false) : ApiResult<Nothing>()
}

object ApiErrorParser {
    fun parse(throwable: Throwable, json: Json): String {
        if (throwable !is HttpException) return throwable.message ?: "Network error"
        val body = throwable.response()?.errorBody()?.string() ?: return "Request failed (${throwable.code()})"
        return try {
            val envelope = json.decodeFromString<ApiErrorEnvelope>(body)
            envelope.error?.message
                ?: envelope.detail
                ?: envelope.otp
                ?: envelope.error?.details
                ?: "Request failed (${throwable.code()})"
        } catch (_: Exception) {
            "Request failed (${throwable.code()})"
        }
    }

    fun requiresOtp(throwable: Throwable, json: Json): Boolean {
        if (throwable !is HttpException) return false
        val body = throwable.response()?.errorBody()?.string() ?: return false
        return try {
            val envelope = json.decodeFromString<ApiErrorEnvelope>(body)
            envelope.otp != null
        } catch (_: Exception) {
            false
        }
    }
}
