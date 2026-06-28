package zm.co.tbz.goldenleaf.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import zm.co.tbz.goldenleaf.ui.components.ErrorText
import zm.co.tbz.goldenleaf.ui.components.LoadingBox
import zm.co.tbz.goldenleaf.ui.components.TbzStatCard
import zm.co.tbz.goldenleaf.ui.components.TbzTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenRegistration: () -> Unit,
    onOpenCorrections: () -> Unit,
    onOpenSync: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val profile by viewModel.profile.collectAsState()
    Scaffold(topBar = { TbzTopBar("Home Dashboard") }) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Welcome, ${profile?.full_name ?: "Officer"}")
                state.error?.let { ErrorText(it) }
                if (state.isLoading && state.dashboard == null) {
                    LoadingBox()
                } else {
                    val dash = state.dashboard
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(
                            listOf(
                                "Pending registrations" to (dash?.pending_registrations?.toString() ?: "0"),
                                "Returned registrations" to (dash?.returned_registrations?.toString() ?: "0"),
                                "Pending permits" to (dash?.pending_permits?.toString() ?: "0"),
                                "Returned permits" to (dash?.returned_permits?.toString() ?: "0"),
                                "Scheduled inspections" to (dash?.scheduled_inspections?.toString() ?: "0"),
                                "Bales today" to (dash?.bales_today?.toString() ?: "0"),
                            ),
                        ) { (title, value) ->
                            TbzStatCard(title = title, value = value)
                        }
                    }
                }
                Button(onClick = onOpenRegistration, modifier = Modifier.fillMaxWidth()) {
                    Text("Registration")
                }
                Button(onClick = onOpenCorrections, modifier = Modifier.fillMaxWidth()) {
                    Text("Corrections Inbox")
                }
                Button(onClick = onOpenSync, modifier = Modifier.fillMaxWidth()) {
                    Text("Sync Settings")
                }
            }
        }
    }
}
