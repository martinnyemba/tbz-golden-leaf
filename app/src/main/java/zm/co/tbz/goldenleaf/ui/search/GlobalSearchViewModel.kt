package zm.co.tbz.goldenleaf.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import zm.co.tbz.goldenleaf.data.repository.GrowerRepository
import zm.co.tbz.goldenleaf.data.repository.PermitRepository
import javax.inject.Inject

enum class SearchResultType {
    GROWER,
    PERMIT,
}

data class SearchResultItem(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val subtitle: String,
)

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    growerRepository: GrowerRepository,
    permitRepository: PermitRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<SearchResultItem>> = combine(
        growerRepository.observeGrowers(),
        permitRepository.observeTransportPermits(),
        _query,
    ) { growers, permits, query ->
        val term = query.trim().lowercase()
        if (term.isBlank()) {
            emptyList()
        } else {
            val growerResults = growers
                .filter { grower ->
                    grower.first_name.lowercase().contains(term) ||
                        grower.last_name.lowercase().contains(term) ||
                        grower.nrc_number.lowercase().contains(term) ||
                        grower.tbz_id?.lowercase()?.contains(term) == true
                }
                .map { grower ->
                    SearchResultItem(
                        id = grower.local_id,
                        type = SearchResultType.GROWER,
                        title = listOfNotNull(grower.first_name, grower.middle_name, grower.last_name)
                            .joinToString(" ")
                            .trim(),
                        subtitle = buildString {
                            append(grower.nrc_number)
                            grower.tbz_id?.let { append(" · TBZ $it") }
                            append(" · ${grower.status}")
                        },
                    )
                }
            val permitResults = permits
                .filter { permit ->
                    permit.permit_number?.lowercase()?.contains(term) == true ||
                        permit.grower_name?.lowercase()?.contains(term) == true ||
                        permit.license_plate.lowercase().contains(term) ||
                        permit.status.lowercase().contains(term)
                }
                .map { permit ->
                    SearchResultItem(
                        id = permit.local_id,
                        type = SearchResultType.PERMIT,
                        title = permit.permit_number ?: "Permit",
                        subtitle = buildString {
                            permit.grower_name?.let { append(it).append(" · ") }
                            append("${permit.total_bales} bales · ${permit.status}")
                        },
                    )
                }
            growerResults + permitResults
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) = _query.update { value }
}
