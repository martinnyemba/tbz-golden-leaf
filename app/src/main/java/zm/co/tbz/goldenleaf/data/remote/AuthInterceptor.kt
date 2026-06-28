package zm.co.tbz.goldenleaf.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import zm.co.tbz.goldenleaf.core.network.PortalSettings
import zm.co.tbz.goldenleaf.core.security.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val portalSettings: PortalSettings,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val apiUrl = portalSettings.getApiBaseUrl().toHttpUrlOrNull()
            ?: return chain.proceed(original)

        val rawPath = original.url.encodedPath
        val relativePath = when {
            rawPath.contains("/api/v1/") -> rawPath.substringAfter("/api/v1/")
            else -> rawPath.trimStart('/')
        }
        val rebuilt = original.url.newBuilder()
            .scheme(apiUrl.scheme)
            .host(apiUrl.host)
            .port(apiUrl.port)
            .encodedPath("/api/v1/$relativePath")
            .build()

        val builder = original.newBuilder().url(rebuilt)
        tokenStore.getAccessToken()?.let { token ->
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
