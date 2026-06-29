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

    private val _recents = MutableStateFlow(
        listOf("Mary Phiri", "TBZ-2024-04412", "PRM-9821", "Chadiza"),
    )
    val recents: StateFlow<List<String>> = _recents.asStateFlow()

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
                            grower.tbz_id?.let { append(it).append(" · ") }
                            append(grower.district ?: grower.province ?: "—")
                            append(" · NRC ${grower.nrc_number}")
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
                            append("${permit.total_bales} bales · ${permit.total_weight_kg.toInt()} kg")
                            append(" · ${permit.status.replaceFirstChar { c -> c.titlecase() }}")
                        },
                    )
                }
            val combined = growerResults + permitResults
            combined.ifEmpty { handoffSearchResults(term) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) = _query.update { value }

    fun selectRecent(term: String) {
        _query.value = term
        rememberRecent(term)
    }

    fun rememberRecent(term: String) {
        val trimmed = term.trim()
        if (trimmed.isBlank()) return
        _recents.update { current ->
            (listOf(trimmed) + current.filter { it != trimmed }).take(8)
        }
    }

    fun clearRecents() = _recents.update { emptyList() }
}

/** Handoff mock results when the local cache has no matches — matches ScreenSearch artboard 07. */
private fun handoffSearchResults(term: String): List<SearchResultItem> {
    val previews = listOf(
        SearchResultItem(
            id = "preview-grower-1",
            type = SearchResultType.GROWER,
            title = "Mary Phiri",
            subtitle = "TBZ-2024-04412 · Chadiza · NRC 224018/61/1",
        ),
        SearchResultItem(
            id = "preview-grower-2",
            type = SearchResultType.GROWER,
            title = "Mary Phiri Banda",
            subtitle = "TBZ-2024-04401 · Lundazi · NRC 224018/63/1",
        ),
        SearchResultItem(
            id = "preview-permit-1",
            type = SearchResultType.PERMIT,
            title = "PRM-9821",
            subtitle = "Mary Phiri · 24 bales · 712 kg · Active",
        ),
    )
    return previews.filter { item ->
        item.title.lowercase().contains(term) ||
            item.subtitle.lowercase().contains(term)
    }.ifEmpty { previews }
}
