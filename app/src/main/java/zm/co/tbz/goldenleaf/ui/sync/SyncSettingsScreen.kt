package zm.co.tbz.goldenleaf.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.repository.SyncRepository
import zm.co.tbz.goldenleaf.data.sync.SyncCoordinator
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFieldRow
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.GlToggle
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val syncCoordinator: SyncCoordinator,
    syncRepository: SyncRepository,
) : ViewModel() {
    val prefs = userPreferences.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val queue = syncRepository.observeQueue().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setAutoSync(value: Boolean) = viewModelScope.launch { userPreferences.setAutoSyncEnabled(value) }
    fun setWifiOnly(value: Boolean) = viewModelScope.launch { userPreferences.setWifiOnlySync(value) }

    fun syncNow() {
        viewModelScope.launch {
            syncCoordinator.scheduleUpload(force = true)
            syncCoordinator.scheduleDeltaDownload(force = true)
        }
    }
}

@Composable
fun SyncSettingsScreen(viewModel: SyncSettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val c = glColors()

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Offline & sync")
            ScrollableFormColumn {
                GlCard(contentPadding = 12.dp) {
                    Column {
                        GlFieldRow(label = "Portal", value = prefs?.portalBaseUrl.orEmpty())
                        GlFieldRow(
                            label = "Last sync",
                            value = if ((prefs?.lastSyncAt ?: 0L) > 0) {
                                DateFormat.getDateTimeInstance().format(Date(prefs!!.lastSyncAt))
                            } else {
                                "Never"
                            },
                        )
                    }
                }
                prefs?.lastSyncError?.takeIf { it.isNotBlank() }?.let { err ->
                    GlBanner(
                        title = "Last sync had errors",
                        subtitle = err,
                        tone = GlTone.Warning,
                        icon = "warning",
                    )
                }
                GlSectionHeader(title = "Sync settings")
                GlCard(contentPadding = 4.dp) {
                    Column {
                        GlRow(
                            title = "Automatic sync",
                            leadingIcon = "cloud-up",
                            trailing = {
                                GlToggle(checked = prefs?.autoSyncEnabled == true, onCheckedChange = viewModel::setAutoSync)
                            },
                        )
                        GlDivider()
                        GlRow(
                            title = "Sync only on Wi-Fi",
                            leadingIcon = "sync",
                            trailing = {
                                GlToggle(checked = prefs?.wifiOnlySync == true, onCheckedChange = viewModel::setWifiOnly)
                            },
                        )
                    }
                }
                GlButton(text = "Sync now", onClick = viewModel::syncNow, leadingIcon = "cloud-up")
                GlSectionHeader(title = "Pending queue · ${queue.count { it.sync_status == "pending" }}")
                if (queue.isEmpty()) {
                    GlEmptyState(title = "Nothing to sync", icon = "cloud-up", subtitle = "All records are up to date")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        queue.forEach { item ->
                            GlCard(contentPadding = 12.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    GlFieldRow(label = "Operation", value = "${item.operation} ${item.endpoint}")
                                    GlSyncChip(status = item.sync_status)
                                    item.last_sync_error?.let { ErrorText(it) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
