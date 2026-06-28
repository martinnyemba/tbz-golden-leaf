package zm.co.tbz.goldenleaf.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.repository.AuthRepository
import zm.co.tbz.goldenleaf.ui.navigation.Routes
import javax.inject.Inject

data class SessionUiState(
    val isReady: Boolean = false,
    val startDestination: String = Routes.SPLASH,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state

    init {
        viewModelScope.launch {
            authRepository.initializePortalUrl()
            val prefs = userPreferences.preferences.first()
            val destination = when {
                !authRepository.isLoggedIn() && !prefs.onboardingCompleted -> Routes.ONBOARDING
                !authRepository.isLoggedIn() -> Routes.PORTAL_LOGIN
                else -> Routes.MAIN
            }
            _state.value = SessionUiState(isReady = true, startDestination = destination)
        }
    }
}
