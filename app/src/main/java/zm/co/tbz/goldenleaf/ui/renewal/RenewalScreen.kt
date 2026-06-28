package zm.co.tbz.goldenleaf.ui.renewal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.ui.components.InfoBanner
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar
import zm.co.tbz.goldenleaf.ui.registration.FormSectionTitle
import zm.co.tbz.goldenleaf.ui.registration.FormTextField
import zm.co.tbz.goldenleaf.ui.registration.RegistrationViewModel
import zm.co.tbz.goldenleaf.ui.registration.ScrollableFormColumn

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

    Scaffold(topBar = { TbzTopBar("Season renewal") }) { padding ->
        ScrollableFormColumn(Modifier.padding(padding)) {
            InfoBanner("Renewal updates the grower's crop allocation for the current season.")
            FormSectionTitle("Find grower")
            FormTextField(
                query,
                viewModel::onSearchChange,
                "Search by name, TBZ ID, or NRC",
            )
            results.forEach { grower ->
                RenewalGrowerRow(
                    grower = grower,
                    onSelect = { onOpenCropAllocation(grower.local_id) },
                )
            }
        }
    }
}

@Composable
private fun RenewalGrowerRow(grower: GrowerEntity, onSelect: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${grower.first_name} ${grower.last_name}", fontWeight = FontWeight.SemiBold)
                grower.tbz_id?.let { Text("TBZ: $it") }
                Text("NRC: ${grower.nrc_number}")
            }
            Button(onClick = onSelect) { Text("Renew crop") }
        }
    }
}
