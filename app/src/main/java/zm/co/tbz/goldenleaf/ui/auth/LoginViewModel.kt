package zm.co.tbz.goldenleaf.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.remote.ApiResult
import zm.co.tbz.goldenleaf.data.repository.AuthRepository
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val otp: String = "",
    val portalUrl: String = UserPreferences.DEFAULT_PORTAL_BASE_URL,
    val showApiSettings: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val requiresOtp: Boolean = false,
    val connectionMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onOtpChange(value: String) = _state.update { it.copy(otp = value, error = null) }
    fun onPortalUrlChange(value: String) = _state.update { it.copy(portalUrl = value) }
    fun toggleApiSettings() = _state.update { it.copy(showApiSettings = !it.showApiSettings) }

    fun savePortalUrl() {
        viewModelScope.launch {
            when (val result = authRepository.updatePortalBaseUrl(_state.value.portalUrl)) {
                is ApiResult.Success -> _state.update { it.copy(connectionMessage = "Portal URL saved") }
                is ApiResult.Error -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, connectionMessage = null, error = null) }
            when (val result = authRepository.testConnection()) {
                is ApiResult.Success -> _state.update { it.copy(isLoading = false, connectionMessage = "Connection OK") }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val current = _state.value
            authRepository.updatePortalBaseUrl(current.portalUrl)
            when (
                val result = authRepository.login(
                    email = current.email,
                    password = current.password,
                    otp = current.otp.takeIf { it.isNotBlank() },
                )
            ) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is ApiResult.Error -> _state.update {
                    it.copy(
                        isLoading = false,
                        error = result.message,
                        requiresOtp = result.requiresOtp || it.requiresOtp,
                    )
                }
            }
        }
    }
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: MutableStateFlow<ChangePasswordUiState> = _state

    fun onOldPasswordChange(v: String) = _state.update { it.copy(oldPassword = v) }
    fun onNewPasswordChange(v: String) = _state.update { it.copy(newPassword = v) }

    fun submit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            if (s.newPassword.length < 10) {
                _state.update { it.copy(error = "New password must be at least 10 characters") }
                return@launch
            }
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.changePassword(s.oldPassword, s.newPassword)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}

data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)
