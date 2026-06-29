package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.remote.ApiErrorParser
import zm.co.tbz.goldenleaf.data.remote.ApiResult
import zm.co.tbz.goldenleaf.core.network.PortalSettings
import zm.co.tbz.goldenleaf.core.security.TokenStore
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.remote.dto.LoginRequest
import zm.co.tbz.goldenleaf.data.remote.dto.TokenRefreshRequest
import zm.co.tbz.goldenleaf.data.sync.SyncCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: TrmcsApi,
    private val tokenStore: TokenStore,
    private val userPreferences: UserPreferences,
    private val portalSettings: PortalSettings,
    private val profileRepository: ProfileRepository,
    private val syncCoordinator: SyncCoordinator,
    private val json: Json,
) {
    fun isLoggedIn(): Boolean = tokenStore.getAccessToken() != null

    suspend fun initializePortalUrl() {
        val prefs = userPreferences.preferences.first()
        portalSettings.updatePortalBaseUrl(prefs.portalBaseUrl)
    }

    suspend fun updatePortalBaseUrl(url: String): ApiResult<Unit> {
        return try {
            val normalized = UserPreferences.normalizePortalUrl(url)
            userPreferences.setPortalBaseUrl(normalized)
            portalSettings.updatePortalBaseUrl(normalized)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to save portal URL")
        }
    }

    suspend fun testConnection(): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val portalRoot = portalSettings.getApiBaseUrl()
                .trimEnd('/')
                .removeSuffix("/api/v1")
            val connection = (URL("${portalRoot.trimEnd('/')}/health/").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val code = connection.responseCode
            connection.disconnect()
            if (code in 200..299) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Server returned HTTP $code")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Connection failed")
        }
    }

    suspend fun login(email: String, password: String, otp: String? = null): ApiResult<Unit> {
        return try {
            val response = api.login(LoginRequest(email = email.trim(), password = password, otp = otp))
            val expiresAt = System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS
            tokenStore.saveTokens(response.access, response.refresh, expiresAt)
            profileRepository.refreshProfile()
            syncCoordinator.scheduleAfterLogin()
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Error(
                message = ApiErrorParser.parse(e, json),
                requiresOtp = ApiErrorParser.requiresOtp(e, json),
            )
        }
    }

    suspend fun refreshSessionIfNeeded(): Boolean {
        val expiresAt = tokenStore.getExpiresAt()
        if (System.currentTimeMillis() < expiresAt - REFRESH_SKEW_MS) return true
        val refresh = tokenStore.getRefreshToken() ?: return false
        return try {
            val response = api.refreshToken(TokenRefreshRequest(refresh))
            tokenStore.saveTokens(
                response.access,
                refresh,
                System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS,
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun logout() {
        try {
            if (tokenStore.getAccessToken() != null) api.logout()
        } catch (_: Exception) {
            // Preserve offline work; clear session locally regardless.
        } finally {
            tokenStore.clear()
            userPreferences.setCachedProfileJson(null)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<Unit> {
        return try {
            api.changePassword(
                zm.co.tbz.goldenleaf.data.remote.dto.ChangePasswordRequest(
                    old_password = oldPassword,
                    new_password = newPassword,
                ),
            )
            logout()
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Error(ApiErrorParser.parse(e, json))
        }
    }

    companion object {
        private const val ACCESS_TOKEN_TTL_MS = 55 * 60 * 1000L
        private const val REFRESH_SKEW_MS = 5 * 60 * 1000L
    }
}
