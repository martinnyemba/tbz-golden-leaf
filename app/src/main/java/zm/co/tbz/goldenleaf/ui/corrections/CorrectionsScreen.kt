package zm.co.tbz.goldenleaf.ui.corrections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlButtonVariant
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlKpiTile
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors

@Composable
fun CorrectionsScreen(
    onOpenGrowerCorrection: (String) -> Unit,
    onOpenTransportPermitCorrection: (String) -> Unit,
    onOpenGroupPermitCorrection: (String) -> Unit,
    viewModel: CorrectionsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    var filter by remember { mutableStateOf("All") }
    val c = glColors()

    val visible = when (filter) {
        "Growers" -> items.filter { it.type == CorrectionType.GROWER }
        "Permits" -> items.filter { it.type == CorrectionType.TRANSPORT_PERMIT }
        "Group permits" -> items.filter { it.type == CorrectionType.GROUP_PERMIT }
        else -> items
    }

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Corrections inbox", subtitle = "${items.size} items returned")
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlKpiTile(
                        label = "Growers",
                        value = items.count { it.type == CorrectionType.GROWER }.toString(),
                        tone = GlTone.Gold,
                        modifier = Modifier.weight(1f),
                    )
                    GlKpiTile(
                        label = "Permits",
                        value = items.count { it.type == CorrectionType.TRANSPORT_PERMIT }.toString(),
                        tone = GlTone.Gold,
                        modifier = Modifier.weight(1f),
                    )
                    GlKpiTile(
                        label = "Group",
                        value = items.count { it.type == CorrectionType.GROUP_PERMIT }.toString(),
                        tone = GlTone.Gold,
                        modifier = Modifier.weight(1f),
                    )
                }
                GlBanner(
                    title = "Action required",
                    subtitle = "Review the correction reason, fix the issues highlighted, and resubmit for approval.",
                    tone = GlTone.Warning,
                    icon = "warning",
                )
                GlFilterPills(
                    options = listOf("All", "Growers", "Permits", "Group permits"),
                    selected = filter,
                    onSelect = { filter = it },
                )
                GlButton(
                    text = "Sync all pending",
                    onClick = viewModel::syncAll,
                    variant = GlButtonVariant.Outline,
                    leadingIcon = "sync",
                )
                if (visible.isEmpty()) {
                    GlEmptyState(title = "No items returned for correction", icon = "check-circle")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        visible.forEach { item ->
                            CorrectionRow(
                                item = item,
                                onClick = {
                                    when (item.type) {
                                        CorrectionType.GROWER -> onOpenGrowerCorrection(item.localId)
                                        CorrectionType.TRANSPORT_PERMIT -> onOpenTransportPermitCorrection(item.localId)
                                        CorrectionType.GROUP_PERMIT -> onOpenGroupPermitCorrection(item.localId)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CorrectionRow(item: CorrectionInboxItem, onClick: () -> Unit) {
    val c = glColors()
    GlCard(onClick = onClick, accent = GlAccent.Gold, contentPadding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.text,
                    modifier = Modifier.weight(1f),
                )
                GlPill(text = "Returned", tone = GlTone.Returned, size = GlPillSize.Sm)
            }
            Text(item.type.name.replace('_', ' ').lowercase(), fontSize = 11.sp, color = c.textMuted)
            item.reason?.let {
                Text(
                    "\"$it\"",
                    fontSize = 12.sp,
                    color = c.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.goldSoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            GlSyncChip(status = item.syncStatus)
        }
    }
}
