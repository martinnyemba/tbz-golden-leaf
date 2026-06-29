package zm.co.tbz.goldenleaf.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlScreenHeader
import zm.co.tbz.goldenleaf.ui.components.GlSearchBar
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors

@Composable
fun GlobalSearchScreen(
    onOpenGrower: (String) -> Unit = {},
    onOpenPermit: (String) -> Unit = {},
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val c = glColors()

    Scaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            GlScreenHeader(title = "Search")
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlSearchBar(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Search growers and permits",
                )
                when {
                    query.isBlank() -> GlEmptyState(
                        title = "Search offline data",
                        icon = "search",
                        subtitle = "Search cached growers and transport permits offline",
                    )
                    results.isEmpty() -> GlEmptyState(
                        title = "No matches",
                        icon = "search",
                        subtitle = "No results for \"$query\"",
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        results.forEach { item ->
                            GlRow(
                                title = item.title,
                                subtitle = item.subtitle,
                                leadingIcon = when (item.type) {
                                    SearchResultType.GROWER -> "profile"
                                    SearchResultType.PERMIT -> "permit"
                                },
                                tone = when (item.type) {
                                    SearchResultType.GROWER -> GlTone.Gold
                                    SearchResultType.PERMIT -> GlTone.Primary
                                },
                                onClick = {
                                    when (item.type) {
                                        SearchResultType.GROWER -> onOpenGrower(item.id)
                                        SearchResultType.PERMIT -> onOpenPermit(item.id)
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
