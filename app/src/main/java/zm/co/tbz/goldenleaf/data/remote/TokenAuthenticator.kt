package zm.co.tbz.goldenleaf.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import zm.co.tbz.goldenleaf.core.network.PortalSettings
import zm.co.tbz.goldenleaf.core.security.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refreshes the access token and retries the request when the server returns
 * 401. Without this, the access token (55-minute TTL) is only ever refreshed by
 * background workers, so any direct API call made after expiry fails with 401
 * until the user logs in again — which also starves reference/data sync.
 *
 * Uses a bare OkHttp client (no [AuthInterceptor]/no authenticator) for the
 * refresh call to avoid a dependency cycle and a refresh-on-refresh loop.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val portalSettings: PortalSettings,
    private val json: Json,
) : Authenticator {

    private val refreshClient = OkHttpClient()

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        // Never try to refresh the refresh call itself, and give up after a retry.
        if (response.request.url.encodedPath.endsWith("auth/token/refresh/")) return null
        if (responseCount(response) >= 2) return null

        val refreshToken = tokenStore.getRefreshToken() ?: return null
        val usedToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()

        // If another concurrent 401 already refreshed, just retry with the latest token.
        val current = tokenStore.getAccessToken()
        val newAccess = if (current != null && current != usedToken) {
            current
        } else {
            performRefresh(refreshToken) ?: return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }

    private fun performRefresh(refreshToken: String): String? {
        return try {
            val base = portalSettings.getApiBaseUrl().trimEnd('/')
            val bodyJson = buildJsonObject { put("refresh", refreshToken) }.toString()
            val request = Request.Builder()
                .url("$base/auth/token/refresh/")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            refreshClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    // Refresh token is dead — clear the session so the UI prompts re-login.
                    if (resp.code == 401) tokenStore.clear()
                    return null
                }
                val payload = resp.body?.string() ?: return null
                val obj = json.parseToJsonElement(payload).jsonObject
                val access = obj["access"]?.jsonPrimitive?.content ?: return null
                // Server rotates + blacklists the old refresh token; persist the new
                // one it returns, else the next refresh uses a dead token and 401s.
                val newRefresh = obj["refresh"]?.jsonPrimitive?.content ?: refreshToken
                tokenStore.saveTokens(access, newRefresh, System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS)
                access
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var prior = response.priorResponse
        var count = 1
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        // Matches the server's SIMPLE_JWT ACCESS_TOKEN_LIFETIME (15 min).
        private const val ACCESS_TOKEN_TTL_MS = 15 * 60 * 1000L
    }
}
