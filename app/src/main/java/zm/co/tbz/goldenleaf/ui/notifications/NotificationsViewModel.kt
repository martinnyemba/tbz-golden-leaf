package zm.co.tbz.goldenleaf.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.entity.NotificationEntity
import zm.co.tbz.goldenleaf.data.remote.ApiErrorParser
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.remote.dto.NotificationDto
import zm.co.tbz.goldenleaf.data.repository.UtilityRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class NotificationsUiState(
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val utilityRepository: UtilityRepository,
    private val api: TrmcsApi,
    private val json: Json,
) : ViewModel() {
    val notifications = utilityRepository.observeNotifications().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                val existingReadIds = utilityRepository.observeNotifications()
                    .first()
                    .filter { it.is_read }
                    .map { it.id }
                    .toSet()
                val dtos = api.notifications()
                utilityRepository.upsertNotifications(
                    dtos.map { dto -> dto.toEntity(isRead = dto.id in existingReadIds) },
                )
                _state.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        error = ApiErrorParser.parse(e, json),
                    )
                }
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch { utilityRepository.markAsRead(id) }
    }
}

private fun NotificationDto.toEntity(isRead: Boolean = false): NotificationEntity =
    NotificationEntity(
        id = id,
        channel = channel,
        subject = subject,
        message = message,
        status = status,
        reference = reference,
        created_at = created_at,
        is_read = isRead,
    )
