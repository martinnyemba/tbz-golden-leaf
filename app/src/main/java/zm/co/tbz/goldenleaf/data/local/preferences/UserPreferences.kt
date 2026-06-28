package zm.co.tbz.goldenleaf.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tbz_prefs")

private const val DEFAULT_PORTAL_BASE_URL = "http://10.0.2.2:8001"

data class AppPreferences(
    val portalBaseUrl: String = DEFAULT_PORTAL_BASE_URL,
    val onboardingCompleted: Boolean = false,
    val darkTheme: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val wifiOnlySync: Boolean = false,
    val lastSyncAt: Long = 0L,
    val cachedProfileJson: String? = null,
    val referenceVersion: String? = null,
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            portalBaseUrl = prefs[KEY_PORTAL_URL] ?: DEFAULT_PORTAL_BASE_URL,
            onboardingCompleted = prefs[KEY_ONBOARDING] ?: false,
            darkTheme = prefs[KEY_DARK_THEME] ?: false,
            autoSyncEnabled = prefs[KEY_AUTO_SYNC] ?: true,
            wifiOnlySync = prefs[KEY_WIFI_ONLY] ?: false,
            lastSyncAt = prefs[KEY_LAST_SYNC] ?: 0L,
            cachedProfileJson = prefs[KEY_PROFILE_JSON],
            referenceVersion = prefs[KEY_REF_VERSION],
        )
    }

    suspend fun setPortalBaseUrl(url: String) {
        context.dataStore.edit { it[KEY_PORTAL_URL] = normalizePortalUrl(url) }
    }

    suspend fun setOnboardingCompleted(value: Boolean = true) {
        context.dataStore.edit { it[KEY_ONBOARDING] = value }
    }

    suspend fun setDarkTheme(value: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = value }
    }

    suspend fun setAutoSyncEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SYNC] = value }
    }

    suspend fun setWifiOnlySync(value: Boolean) {
        context.dataStore.edit { it[KEY_WIFI_ONLY] = value }
    }

    suspend fun setLastSyncAt(epochMs: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC] = epochMs }
    }

    suspend fun setCachedProfileJson(json: String?) {
        context.dataStore.edit {
            if (json == null) it.remove(KEY_PROFILE_JSON) else it[KEY_PROFILE_JSON] = json
        }
    }

    suspend fun setReferenceVersion(version: String?) {
        context.dataStore.edit {
            if (version == null) it.remove(KEY_REF_VERSION) else it[KEY_REF_VERSION] = version
        }
    }

    companion object {
        const val DEFAULT_PORTAL_BASE_URL = "http://10.0.2.2:8001"

        fun normalizePortalUrl(raw: String): String {
            var url = raw.trim().trimEnd('/')
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            return url
        }

        fun toApiBaseUrl(portalBaseUrl: String): String =
            "${normalizePortalUrl(portalBaseUrl)}/api/v1/"

        private val KEY_PORTAL_URL = stringPreferencesKey("portal_base_url")
        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync_enabled")
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only_sync")
        private val KEY_LAST_SYNC = longPreferencesKey("last_sync_at")
        private val KEY_PROFILE_JSON = stringPreferencesKey("cached_profile_json")
        private val KEY_REF_VERSION = stringPreferencesKey("reference_version")
    }
}
