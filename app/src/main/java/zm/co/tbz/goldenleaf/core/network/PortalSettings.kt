package zm.co.tbz.goldenleaf.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortalSettings @Inject constructor() {
    private val _apiBaseUrl = MutableStateFlow(UserPreferences.toApiBaseUrl(UserPreferences.DEFAULT_PORTAL_BASE_URL))
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl.asStateFlow()

    fun updatePortalBaseUrl(portalBaseUrl: String) {
        _apiBaseUrl.value = UserPreferences.toApiBaseUrl(portalBaseUrl)
    }

    fun getApiBaseUrl(): String = _apiBaseUrl.value
}
