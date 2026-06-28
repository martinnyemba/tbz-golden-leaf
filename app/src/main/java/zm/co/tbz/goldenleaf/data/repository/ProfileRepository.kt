package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.remote.ApiErrorParser
import zm.co.tbz.goldenleaf.data.remote.ApiResult
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.remote.dto.MobileProfileResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: TrmcsApi,
    private val userPreferences: UserPreferences,
    private val json: Json,
) {
    val profile: Flow<MobileProfileResponse?> = userPreferences.preferences.map { prefs ->
        prefs.cachedProfileJson?.let {
            runCatching { json.decodeFromString<MobileProfileResponse>(it) }.getOrNull()
        }
    }

    suspend fun refreshProfile(): ApiResult<MobileProfileResponse> {
        return try {
            val profile = api.mobileMe()
            userPreferences.setCachedProfileJson(json.encodeToString(profile))
            ApiResult.Success(profile)
        } catch (e: Exception) {
            ApiResult.Error(ApiErrorParser.parse(e, json))
        }
    }
}
