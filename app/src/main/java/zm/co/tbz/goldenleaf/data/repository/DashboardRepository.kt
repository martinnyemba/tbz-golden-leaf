package zm.co.tbz.goldenleaf.data.repository

import zm.co.tbz.goldenleaf.data.remote.ApiErrorParser
import zm.co.tbz.goldenleaf.data.remote.ApiResult
import kotlinx.serialization.json.Json
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.remote.dto.DashboardResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val api: TrmcsApi,
    private val json: Json,
) {
    suspend fun loadDashboard(): ApiResult<DashboardResponse> {
        return try {
            ApiResult.Success(api.dashboard())
        } catch (e: Exception) {
            ApiResult.Error(ApiErrorParser.parse(e, json))
        }
    }
}
