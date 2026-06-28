package zm.co.tbz.goldenleaf.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.repository.SyncRepository
import zm.co.tbz.goldenleaf.data.sync.SyncCoordinator
import zm.co.tbz.goldenleaf.ui.components.SyncStatusChip
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar
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
        viewModelScope.launch { syncCoordinator.scheduleUpload(force = true) }
    }
}

@Composable
fun SyncSettingsScreen(viewModel: SyncSettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsState()
    val queue by viewModel.queue.collectAsState()
    Scaffold(topBar = { TbzTopBar("Sync Settings") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Portal: ${prefs?.portalBaseUrl.orEmpty()}")
            Text(
                "Last sync: " + if ((prefs?.lastSyncAt ?: 0L) > 0) {
                    DateFormat.getDateTimeInstance().format(Date(prefs!!.lastSyncAt))
                } else {
                    "Never"
                },
            )
            RowSwitch("Automatic sync", prefs?.autoSyncEnabled == true, viewModel::setAutoSync)
            RowSwitch("Sync only on Wi-Fi", prefs?.wifiOnlySync == true, viewModel::setWifiOnly)
            Button(onClick = viewModel::syncNow, modifier = Modifier.fillMaxWidth()) { Text("Sync now") }
            Text("Pending queue (${queue.count { it.sync_status == "pending" }})")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(queue, key = { it.local_id }) { item ->
                    Column(Modifier.fillMaxWidth()) {
                        Text("${item.operation} ${item.endpoint}")
                        SyncStatusChip(item.sync_status)
                        item.last_sync_error?.let { Text(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
