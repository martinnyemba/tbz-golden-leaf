package zm.co.tbz.goldenleaf.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun scheduleAfterLogin() {
        scheduleReferenceRefresh()
        scheduleDeltaDownload()
        scheduleUpload(force = true)
    }

    suspend fun scheduleUpload(force: Boolean = false) {
        val prefs = userPreferences.preferences.first()
        if (!prefs.autoSyncEnabled && !force) return
        val constraints = buildConstraints(wifiOnly = prefs.wifiOnlySync)
        val request = OneTimeWorkRequestBuilder<SessionRefreshWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            WORK_SESSION,
            if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
        val upload = OneTimeWorkRequestBuilder<UploadSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            WORK_UPLOAD,
            if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            upload,
        )
    }

    fun scheduleReferenceRefresh() {
        val request = OneTimeWorkRequestBuilder<ReferenceRefreshWorker>()
            .setConstraints(connectedConstraints())
            .build()
        workManager.enqueueUniqueWork(WORK_REFERENCE, ExistingWorkPolicy.KEEP, request)
    }

    fun scheduleDeltaDownload(force: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<DeltaDownloadWorker>()
            .setConstraints(connectedConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(
            WORK_DELTA,
            if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    suspend fun manualSync(): ManualSyncOutcome {
        val prefs = userPreferences.preferences.first()
        if (!prefs.autoSyncEnabled) return ManualSyncOutcome.AUTO_DISABLED
        scheduleUpload(force = true)
        scheduleDeltaDownload()
        return ManualSyncOutcome.OK
    }

    private fun buildConstraints(wifiOnly: Boolean): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

    private fun connectedConstraints(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    enum class ManualSyncOutcome {
        OK, UP_TO_DATE, NO_SESSION, OFFLINE, WIFI_ONLY, AUTO_DISABLED,
    }

    companion object {
        private const val WORK_UPLOAD = "tbz_upload_sync"
        private const val WORK_SESSION = "tbz_session_refresh"
        private const val WORK_REFERENCE = "tbz_reference_refresh"
        private const val WORK_DELTA = "tbz_delta_download"
    }
}
