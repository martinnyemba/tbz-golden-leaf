package zm.co.tbz.goldenleaf.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.core.rbac.AccessControlService
import javax.inject.Inject

data class MenuAccessState(
    val canRegistration: Boolean = false,
    val canInspection: Boolean = false,
    val canMarketing: Boolean = false,
    val canPermits: Boolean = false,
    val canArbitration: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class MenuAccessViewModel @Inject constructor(
    private val accessControl: AccessControlService,
) : ViewModel() {
    private val _state = MutableStateFlow(MenuAccessState())
    val state: StateFlow<MenuAccessState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = false,
                    canRegistration = accessControl.canAccessRegistration(),
                    canInspection = accessControl.canAccessInspection(),
                    canMarketing = accessControl.canAccessMarketing(),
                    canPermits = accessControl.canAccessPermits(),
                    canArbitration = accessControl.canAccessArbitration(),
                )
            }
        }
    }
}
