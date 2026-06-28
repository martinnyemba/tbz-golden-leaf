package zm.co.tbz.goldenleaf.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.remote.ApiResult
import zm.co.tbz.goldenleaf.data.remote.dto.DashboardResponse
import zm.co.tbz.goldenleaf.data.repository.DashboardRepository
import zm.co.tbz.goldenleaf.data.repository.ProfileRepository
import zm.co.tbz.goldenleaf.data.remote.dto.MobileProfileResponse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val dashboard: DashboardResponse? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {
    val profile = profileRepository.profile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = dashboardRepository.loadDashboard()) {
                is ApiResult.Success -> _state.update { it.copy(isLoading = false, dashboard = result.data) }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
