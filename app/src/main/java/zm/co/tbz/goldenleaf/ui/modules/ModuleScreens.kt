package zm.co.tbz.goldenleaf.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zm.co.tbz.goldenleaf.ui.components.ModuleHubCard
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar

@Composable
fun SearchScreen() {
    Scaffold(topBar = { TbzTopBar("Search") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Global search across cached growers, permits, and inspections.")
            Text("Coming soon: offline Room-backed search.")
        }
    }
}

@Composable
fun MenuScreen(
    onRegistration: () -> Unit,
    onInspection: () -> Unit,
    onMarketing: () -> Unit,
    onPermits: () -> Unit,
    onArbitration: () -> Unit,
    onSyncSettings: () -> Unit,
) {
    Scaffold(topBar = { TbzTopBar("More") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModuleHubCard("Registration", "Grower registration & corrections", onRegistration)
            ModuleHubCard("Inspection", "Schedules, reports, validation", onInspection)
            ModuleHubCard("Marketing & Sales", "Bale capture & pending sales", onMarketing)
            ModuleHubCard("Permits", "Transport & group permits", onPermits)
            ModuleHubCard("Arbitration", "Bale arbitration submissions", onArbitration)
            ModuleHubCard("Sync settings", "Offline queue & manual sync", onSyncSettings)
        }
    }
}

@Composable
fun InspectionHubScreen() {
    Scaffold(topBar = { TbzTopBar("Inspection") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Inspection schedules, field reports, nursery/curing forms, and validation.")
            Text("Routes: /inspection/* — scaffold ready for full form implementation.")
        }
    }
}

@Composable
fun MarketingHubScreen() {
    Scaffold(topBar = { TbzTopBar("Marketing") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Sales capture, pending sales sync, and bale barcode scanning.")
        }
    }
}

@Composable
fun PermitsHubScreen() {
    Scaffold(topBar = { TbzTopBar("Permits") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Permit requests, validation, group permits, and approval flows.")
        }
    }
}

@Composable
fun ArbitrationScreen() {
    Scaffold(topBar = { TbzTopBar("Arbitration") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Arbitration submission for bale disputes.")
        }
    }
}

@Composable
fun StaticContentScreen(title: String, body: String) {
    Scaffold(topBar = { TbzTopBar(title) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(body)
        }
    }
}
