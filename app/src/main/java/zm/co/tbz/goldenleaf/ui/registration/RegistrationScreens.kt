package zm.co.tbz.goldenleaf.ui.registration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEditEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerRegistrationEntity
import zm.co.tbz.goldenleaf.ui.components.ModuleHubCard
import zm.co.tbz.goldenleaf.ui.components.SyncStatusChip
import zm.co.tbz.goldenleaf.ui.components.TbzStatCard
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar

@Composable
fun RegistrationHubScreen(
    onNewRegistration: () -> Unit,
    onGrowerList: () -> Unit,
    onLocalRegistrations: () -> Unit,
    onGrowerUpdates: () -> Unit,
    onCorrections: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val stats by viewModel.hubStats.collectAsState()
    val registrations by viewModel.registrations.collectAsState()

    Scaffold(topBar = { TbzTopBar("Registration") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TbzStatCard("Growers", stats.totalGrowers.toString(), Modifier.weight(1f))
                TbzStatCard("Pending", stats.pendingSync.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TbzStatCard("Synced", stats.synced.toString(), Modifier.weight(1f))
                TbzStatCard("Failed", stats.failed.toString(), Modifier.weight(1f))
            }
            ModuleHubCard("New grower registration", "Two-step offline-first capture", onNewRegistration)
            ModuleHubCard("Portal growers list", "Search cached growers", onGrowerList)
            ModuleHubCard("Local registrations", "Drafts and pending sync", onLocalRegistrations)
            ModuleHubCard("Grower updates queue", "Pending PATCH edits", onGrowerUpdates)
            ModuleHubCard("Corrections inbox", "Returned-for-correction", onCorrections)
            if (registrations.isNotEmpty()) {
                Text("Recent local saves", fontWeight = FontWeight.SemiBold)
                registrations.take(3).forEach { RegistrationRow(it) }
            }
        }
    }
}

@Composable
fun GrowerListScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val growers by viewModel.filteredGrowers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TbzTopBar("Growers") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text("Search TBZ ID, NRC, name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GrowerFormChoices.statusFilters.take(4).forEach { status ->
                    FilterChip(
                        selected = uiState.statusFilter == status,
                        onClick = { viewModel.onStatusFilterChange(status) },
                        label = { Text(status.replace('_', ' ')) },
                    )
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(growers, key = { it.local_id }) { grower ->
                    GrowerRow(grower = grower, onClick = { onOpenDetail(grower.local_id) })
                }
            }
        }
    }
}

@Composable
fun LocalRegistrationsScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val registrations by viewModel.filteredRegistrations.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TbzTopBar("Local registrations") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GrowerFormChoices.syncFilters.forEach { filter ->
                    FilterChip(
                        selected = uiState.syncFilter == filter,
                        onClick = { viewModel.onSyncFilterChange(filter) },
                        label = { Text(filter.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Button(
                onClick = viewModel::syncAllPending,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) { Text("Sync all pending") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(registrations, key = { it.local_id }) { reg ->
                    RegistrationRow(reg, onClick = { onOpenDetail(reg.local_id) })
                }
            }
        }
    }
}

@Composable
fun GrowerUpdatesScreen(
    onOpenGrower: (String) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val edits by viewModel.edits.collectAsState()

    Scaffold(topBar = { TbzTopBar("Grower updates") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(onClick = viewModel::syncAllPending, modifier = Modifier.fillMaxWidth()) {
                Text("Sync all pending updates")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(edits, key = { it.local_id }) { edit ->
                    GrowerEditRow(edit, onClick = { onOpenGrower(edit.grower_local_id) })
                }
            }
        }
    }
}

@Composable
fun GrowerDetailScreen(
    localId: String,
    onEdit: (String) -> Unit,
    onCorrection: (String) -> Unit,
    onAddCrop: (String) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val grower by viewModel.observeGrower(localId).collectAsState()

    Scaffold(topBar = { TbzTopBar("Grower details") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val item = grower
            if (item == null) {
                Text("Grower not found")
            } else {
                Text("${item.first_name} ${item.last_name}", fontWeight = FontWeight.Bold)
                item.tbz_id?.let { Text("TBZ ID: $it") }
                Text("NRC: ${item.nrc_number}")
                Text("Phone: ${item.phone_number.orEmpty()}")
                Text("Province: ${item.province.orEmpty()}")
                Text("District: ${item.district.orEmpty()}")
                Text("Workflow: ${item.status}")
                SyncStatusChip(item.sync_status)
                item.last_sync_error?.let { Text("Sync error: $it") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.status == "RETURNED_FOR_CORRECTION") {
                        Button(onClick = { onCorrection(localId) }) { Text("Fix & resubmit") }
                    } else {
                        OutlinedButton(onClick = { onEdit(localId) }) { Text("Edit") }
                        OutlinedButton(onClick = { onAddCrop(localId) }) { Text("Add crop") }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowerRow(grower: GrowerEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${grower.first_name} ${grower.last_name}", fontWeight = FontWeight.SemiBold)
            grower.tbz_id?.let { Text("TBZ: $it") }
            Text("NRC: ${grower.nrc_number}")
            Text("Status: ${grower.status}")
            SyncStatusChip(grower.sync_status)
        }
    }
}

@Composable
private fun RegistrationRow(
    registration: GrowerRegistrationEntity,
    onClick: (() -> Unit)? = null,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Local ID: ${registration.local_id.take(8)}…")
            Text("Created: ${registration.created_at}")
            SyncStatusChip(registration.sync_status)
        }
    }
}

@Composable
private fun GrowerEditRow(edit: GrowerEditEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Edit ${edit.local_id.take(8)}…")
            Text("Grower: ${edit.grower_local_id.take(8)}…")
            SyncStatusChip(edit.sync_status)
        }
    }
}
