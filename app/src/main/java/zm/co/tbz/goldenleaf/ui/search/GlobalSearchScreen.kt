package zm.co.tbz.goldenleaf.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import zm.co.tbz.goldenleaf.ui.components.GlScaffold
import zm.co.tbz.goldenleaf.ui.components.glVerticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.GlCard
import zm.co.tbz.goldenleaf.ui.components.GlEmptyState
import zm.co.tbz.goldenleaf.ui.components.GlFilterPills
import zm.co.tbz.goldenleaf.ui.components.GlIcon
import zm.co.tbz.goldenleaf.ui.components.GlPill
import zm.co.tbz.goldenleaf.ui.components.GlRow
import zm.co.tbz.goldenleaf.ui.components.GlSearchBar
import zm.co.tbz.goldenleaf.ui.components.GlSectionHeader
import zm.co.tbz.goldenleaf.ui.components.GlTone
import zm.co.tbz.goldenleaf.ui.components.glColors

/** 07 · Universal search — matches handoff ScreenSearch. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchScreen(
    onOpenGrower: (String) -> Unit = {},
    onOpenPermit: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val c = glColors()
    var typeFilter by remember { mutableStateOf("All") }

    val filteredResults = remember(results, typeFilter) {
        when (typeFilter) {
            "Growers" -> results.filter { it.type == SearchResultType.GROWER }
            "Permits" -> results.filter { it.type == SearchResultType.PERMIT }
            "Inspections" -> emptyList()
            else -> results
        }
    }

    GlScaffold(containerColor = c.bg) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(c.surfaceAlt)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        GlIcon("arrow-left", size = 20.dp, tint = c.text)
                    }
                }
                GlSearchBar(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Search growers, permits, NRC…",
                    modifier = Modifier.weight(1f),
                )
            }

            GlFilterPills(
                options = listOf("All", "Growers", "Permits", "Inspections"),
                selected = typeFilter,
                onSelect = { typeFilter = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .glVerticalScroll()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                if (query.isBlank()) {
                    GlSectionHeader(
                        title = "Recent",
                        action = if (recents.isNotEmpty()) "Clear" else null,
                        onAction = viewModel::clearRecents,
                    )
                    recents.forEach { recent ->
                        GlRow(
                            title = recent,
                            leadingIcon = "clock",
                            onClick = { viewModel.selectRecent(recent) },
                        )
                    }
                    GlSectionHeader(title = "Suggestions", modifier = Modifier.padding(top = 8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            "High-risk growers",
                            "Pending permits",
                            "Today's inspections",
                            "Sales last 7 days",
                        ).forEach { hint ->
                            GlPill(text = hint, tone = GlTone.Default)
                        }
                    }
                } else if (filteredResults.isEmpty()) {
                    GlEmptyState(
                        title = "No matches",
                        icon = "search",
                        subtitle = "No results for \"$query\"",
                    )
                } else {
                    GlSectionHeader(title = "${filteredResults.size} results")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredResults.forEach { item ->
                            GlCard(
                                onClick = {
                                    viewModel.rememberRecent(item.title)
                                    when (item.type) {
                                        SearchResultType.GROWER -> onOpenGrower(item.id)
                                        SearchResultType.PERMIT -> onOpenPermit(item.id)
                                    }
                                },
                                contentPadding = 12.dp,
                            ) {
                                GlRow(
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    leadingIcon = when (item.type) {
                                        SearchResultType.GROWER -> "profile"
                                        SearchResultType.PERMIT -> "permit"
                                    },
                                    tone = when (item.type) {
                                        SearchResultType.GROWER -> GlTone.Primary
                                        SearchResultType.PERMIT -> GlTone.Gold
                                    },
                                    trailing = { GlIcon("chevron-right", size = 18.dp, tint = c.textSubtle) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
