package zm.co.tbz.goldenleaf.ui.renewal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.ui.components.GlAvatar
import zm.co.tbz.goldenleaf.ui.components.GlBanner
import zm.co.tbz.goldenleaf.ui.components.GlButton
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSearchBar
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors
import zm.co.tbz.goldenleaf.ui.registration.RegistrationViewModel

@Composable
fun RenewalScreen(
    onOpenCropAllocation: (String) -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val growers by viewModel.filteredGrowers.collectAsState()
    val query = uiState.searchQuery
    val results = if (query.length >= 2) {
        growers.filter {
            it.first_name.contains(query, true) ||
                it.last_name.contains(query, true) ||
                it.nrc_number.contains(query, true) ||
                it.tbz_id?.contains(query, true) == true
        }.take(20)
    } else {
        emptyList()
    }
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Season renewal")
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlBanner(
                    title = "Renewal updates the grower's crop allocation for the current season.",
                    tone = GlTone.Info,
                    icon = "info",
                )
                GlSectionHeader(title = "Find grower")
                GlSearchBar(
                    value = query,
                    onValueChange = viewModel::onSearchChange,
                    placeholder = "Search by name, TBZ ID, or NRC",
                )
                when {
                    query.length < 2 -> GlEmptyState(
                        title = "Search for a grower",
                        icon = "search",
                        subtitle = "Type at least 2 characters to find a grower to renew",
                    )
                    results.isEmpty() -> GlEmptyState(
                        title = "No matches",
                        icon = "users",
                        subtitle = "No growers found for \"$query\"",
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        results.forEach { grower ->
                            RenewalGrowerRow(
                                grower = grower,
                                onSelect = { onOpenCropAllocation(grower.local_id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenewalGrowerRow(grower: GrowerEntity, onSelect: () -> Unit) {
    val c = glColors()
    GlCard(contentPadding = 12.dp) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlAvatar(name = "${grower.first_name} ${grower.last_name}", size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    "${grower.first_name} ${grower.last_name}",
                    color = c.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                grower.tbz_id?.let {
                    Text("TBZ: $it", color = c.textMuted, fontSize = 12.sp)
                }
                Text("NRC: ${grower.nrc_number}", color = c.textMuted, fontSize = 12.sp)
            }
            GlButton(
                text = "Renew crop",
                onClick = onSelect,
                fillMaxWidth = false,
            )
        }
    }
}
