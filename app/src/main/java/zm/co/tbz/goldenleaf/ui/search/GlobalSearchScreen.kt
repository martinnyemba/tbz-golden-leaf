package zm.co.tbz.goldenleaf.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar

@Composable
fun GlobalSearchScreen(
    onOpenGrower: (String) -> Unit = {},
    onOpenPermit: (String) -> Unit = {},
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(topBar = { TbzTopBar("Search") }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search growers and permits") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (query.isBlank()) {
                Text("Search cached growers and transport permits offline.")
            } else if (results.isEmpty()) {
                Text("No matches for \"$query\".")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { "${it.type}:${it.id}" }) { item ->
                        Card(
                            onClick = {
                                when (item.type) {
                                    SearchResultType.GROWER -> onOpenGrower(item.id)
                                    SearchResultType.PERMIT -> onOpenPermit(item.id)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    item.title,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    when (item.type) {
                                        SearchResultType.GROWER -> "Grower"
                                        SearchResultType.PERMIT -> "Permit"
                                    },
                                )
                                Text(item.subtitle)
                            }
                        }
                    }
                }
            }
        }
    }
}
