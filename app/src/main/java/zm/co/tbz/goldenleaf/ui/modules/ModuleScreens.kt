package zm.co.tbz.goldenleaf.ui.modules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlDivider
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.glColors
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
    onRenewal: () -> Unit,
    onNotifications: () -> Unit,
    onSyncSettings: () -> Unit,
    canRegistration: Boolean = true,
    canInspection: Boolean = true,
    canMarketing: Boolean = true,
    canPermits: Boolean = true,
    canArbitration: Boolean = true,
) {
    val c = glColors()
    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "More")
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                GlCard(contentPadding = 0.dp) {
                    Column {
                        val rows = buildList {
                            if (canRegistration) add(Triple("Registration", "Grower registration & corrections", "users" to onRegistration))
                            if (canInspection) add(Triple("Inspection", "Schedules, reports, validation", "check-circle" to onInspection))
                            if (canMarketing) add(Triple("Marketing & Sales", "Bale capture & pending sales", "bale" to onMarketing))
                            if (canPermits) add(Triple("Permits", "Transport & group permits", "permit" to onPermits))
                            if (canArbitration) add(Triple("Arbitration", "Bale arbitration submissions", "warning" to onArbitration))
                            if (canRegistration) add(Triple("Season renewal", "Update grower crop allocation", "sync" to onRenewal))
                            add(Triple("Notifications", "Alerts and system messages", "bell" to onNotifications))
                            add(Triple("Sync settings", "Offline queue & manual sync", "cloud-up" to onSyncSettings))
                        }
                        rows.forEachIndexed { index, (title, subtitle, iconAndAction) ->
                            val (icon, action) = iconAndAction
                            GlRow(title = title, subtitle = subtitle, leadingIcon = icon, onClick = action)
                            if (index < rows.lastIndex) GlDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaticContentScreen(title: String, body: String) {
    val c = glColors()
    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = title)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(body, color = c.text, fontSize = 14.sp)
            }
        }
    }
}
