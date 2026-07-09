package zm.co.tbz.goldenleaf.ui.corrections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import zm.co.tbz.goldenleaf.ui.components.glVerticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.GlAccent
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlKpiTile
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlPillSize
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSyncChip
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors

/** 08 · Corrections inbox — cross-module returned submissions. */
@Composable
fun CorrectionsScreen(
    onOpenGrowerCorrection: (String) -> Unit,
    onOpenTransportPermitCorrection: (String) -> Unit,
    onOpenGroupPermitCorrection: (String) -> Unit,
    onBack: (() -> Unit)? = null,
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

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(
                title = "Corrections inbox",
                subtitle = "${items.size} items returned",
                onBack = onBack,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .glVerticalScroll(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                GlFilterPills(
                    options = listOf("All", "Growers", "Permits", "Group permits"),
                    selected = filter,
                    onSelect = { filter = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                if (visible.isEmpty()) {
                    GlEmptyState(
                        title = "No items returned for correction",
                        icon = "check-circle",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        visible.forEach { item ->
                            CorrectionInboxCard(
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
                Spacer(Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun CorrectionInboxCard(item: CorrectionInboxItem, onClick: () -> Unit) {
    val c = glColors()
    GlCard(onClick = onClick, accent = GlAccent.Gold, contentPadding = 14.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.goldSoft),
                contentAlignment = Alignment.Center,
            ) {
                GlIcon(item.icon, size = 22.dp, tint = c.goldDeep)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        item.title,
                        color = c.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    GlPill(text = "Returned", tone = GlTone.Returned, size = GlPillSize.Sm)
                }
                Text(
                    item.ref,
                    color = c.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                )
                item.reason?.let { reason ->
                    Text(
                        "\"$reason\"",
                        color = c.text,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.goldSoft)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Returned ${item.returnedLabel}",
                        color = c.textMuted,
                        fontSize = 11.sp,
                    )
                    GlSyncChip(status = item.syncStatus)
                }
            }
        }
    }
}
