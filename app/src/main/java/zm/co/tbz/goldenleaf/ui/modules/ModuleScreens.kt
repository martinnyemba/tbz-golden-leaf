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
import zm.co.tbz.goldenleaf.ui.search.GlobalSearchScreen

@Composable
fun SearchScreen(
    onOpenGrower: (String) -> Unit = {},
    onOpenPermit: (String) -> Unit = {},
) {
    GlobalSearchScreen(
        onOpenGrower = onOpenGrower,
        onOpenPermit = onOpenPermit,
    )
}

@Composable
fun MenuScreen(
    onRegistration: () -> Unit,
    onInspection: () -> Unit,
    onMarketing: () -> Unit,
    onPermits: () -> Unit,
    onArbitration: () -> Unit,
    onNotifications: () -> Unit,
    onSyncSettings: () -> Unit,
    canRegistration: Boolean = true,
    canInspection: Boolean = true,
    canMarketing: Boolean = true,
    canPermits: Boolean = true,
    canArbitration: Boolean = true,
) {
    Scaffold(topBar = { TbzTopBar("More") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (canRegistration) {
                ModuleHubCard("Registration", "Grower registration & corrections", onRegistration)
            }
            if (canInspection) {
                ModuleHubCard("Inspection", "Schedules, reports, validation", onInspection)
            }
            if (canMarketing) {
                ModuleHubCard("Marketing & Sales", "Bale capture & pending sales", onMarketing)
            }
            if (canPermits) {
                ModuleHubCard("Permits", "Transport & group permits", onPermits)
            }
            if (canArbitration) {
                ModuleHubCard("Arbitration", "Bale arbitration submissions", onArbitration)
            }
            ModuleHubCard("Notifications", "Alerts and system messages", onNotifications)
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
