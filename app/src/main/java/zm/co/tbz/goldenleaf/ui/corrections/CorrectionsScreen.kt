package zm.co.tbz.goldenleaf.ui.corrections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.SyncStatusChip
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar

@Composable
fun CorrectionsScreen(
    onOpenGrowerCorrection: (String) -> Unit,
    onOpenTransportPermitCorrection: (String) -> Unit,
    onOpenGroupPermitCorrection: (String) -> Unit,
    viewModel: CorrectionsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()

    Scaffold(topBar = { TbzTopBar("Corrections inbox") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(
                onClick = viewModel::syncAll,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) { Text("Sync all pending") }
            if (items.isEmpty()) {
                Text("No items returned for correction")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { "${it.type}-${it.localId}" }) { item ->
                        CorrectionRow(
                            item = item,
                            onClick = {
                                when (item.type) {
                                    CorrectionType.GROWER -> onOpenGrowerCorrection(item.localId)
                                    CorrectionType.TRANSPORT_PERMIT ->
                                        onOpenTransportPermitCorrection(item.localId)
                                    CorrectionType.GROUP_PERMIT ->
                                        onOpenGroupPermitCorrection(item.localId)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CorrectionRow(item: CorrectionInboxItem, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.type.name.replace('_', ' '), fontWeight = FontWeight.SemiBold)
            Text(item.title)
            Text(item.subtitle)
            item.reason?.let { Text("Reason: $it") }
            SyncStatusChip(item.syncStatus)
        }
    }
}
