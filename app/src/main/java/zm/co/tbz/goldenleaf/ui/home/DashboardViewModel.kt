package zm.co.tbz.goldenleaf.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.entity.InspectionEntity
import zm.co.tbz.goldenleaf.data.remote.ApiResult
import zm.co.tbz.goldenleaf.data.remote.dto.DashboardResponse
import zm.co.tbz.goldenleaf.data.repository.DashboardRepository
import zm.co.tbz.goldenleaf.data.repository.InspectionRepository
import zm.co.tbz.goldenleaf.data.repository.ProfileRepository
import zm.co.tbz.goldenleaf.data.repository.SyncRepository
import zm.co.tbz.goldenleaf.data.repository.UtilityRepository
import zm.co.tbz.goldenleaf.ui.components.GlTone
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val dashboard: DashboardResponse? = null,
)

data class DashboardScheduleCard(
    val localId: String,
    val growerName: String,
    val growerId: String,
    val kind: String,
    val timeLabel: String,
    val location: String,
    val isHighRisk: Boolean,
    val pillTone: GlTone,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    profileRepository: ProfileRepository,
    inspectionRepository: InspectionRepository,
    syncRepository: SyncRepository,
    utilityRepository: UtilityRepository,
) : ViewModel() {
    val profile = profileRepository.profile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val inspections = inspectionRepository.observeInspections().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val highRiskGrowerIds = inspectionRepository.observeValidations().map { validations ->
        validations.filter { (it.risk_score ?: 0) >= 70 }.map { it.grower_id }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val pendingSyncCount = syncRepository.observeQueue().map { queue ->
        queue.count { it.sync_status == "pending" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val unreadNotificationCount = utilityRepository.observeNotifications().map { items ->
        items.count { !it.is_read }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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

    fun scheduleCards(
        inspections: List<InspectionEntity>,
        highRiskGrowerIds: Set<String>,
        period: String,
        typeFilter: String,
    ): List<DashboardScheduleCard> {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val live = inspections
            .filter { inspection ->
                val date = parseInspectionDate(inspection.scheduled_date) ?: return@filter false
                when (period) {
                    "Today" -> date == today
                    else -> !date.isBefore(weekStart) && !date.isAfter(weekEnd)
                }
            }
            .mapNotNull { inspection ->
                val kind = inspectionKind(inspection.inspection_type)
                if (typeFilter != "All" && kind != typeFilter) return@mapNotNull null
                val date = parseInspectionDate(inspection.scheduled_date)
                DashboardScheduleCard(
                    localId = inspection.local_id,
                    growerName = inspection.grower_name ?: "Grower",
                    growerId = inspection.grower_id,
                    kind = kind,
                    timeLabel = date?.format(DateTimeFormatter.ofPattern("HH:mm")).takeUnless { it == "00:00" }
                        ?: date?.format(DateTimeFormatter.ofPattern("EEE")) ?: inspection.scheduled_date,
                    location = inspection.district ?: inspection.province ?: "—",
                    isHighRisk = inspection.grower_id in highRiskGrowerIds,
                    pillTone = kindTone(kind, inspection.grower_id in highRiskGrowerIds),
                )
            }
            .sortedBy { it.timeLabel }
        return live.ifEmpty { handoffScheduleCards(period, typeFilter) }
    }
}

/** Handoff mock schedule when no cached inspections — matches ScreenHome artboard 05. */
internal fun handoffScheduleCards(period: String, typeFilter: String): List<DashboardScheduleCard> {
    val todayItems = listOf(
        DashboardScheduleCard("preview-1", "Mary Phiri", "TBZ-04412", "Field", "09:30", "Chadiza", true, GlTone.Warning),
        DashboardScheduleCard("preview-2", "Charles Tembo", "TBZ-03128", "Curing", "13:00", "Katete", false, GlTone.Primary),
        DashboardScheduleCard("preview-3", "Felix Sakala", "TBZ-04501", "Nursery", "15:30", "Petauke", false, GlTone.Primary),
    )
    val weekItems = listOf(
        DashboardScheduleCard("preview-4", "Gladys Mwale", "TBZ-02981", "Validation", "Thu", "Lundazi", false, GlTone.Gold),
        DashboardScheduleCard("preview-5", "Loveness Banda", "TBZ-04610", "Field", "Fri", "Chipata", false, GlTone.Gold),
    )
    val items = if (period == "Today") todayItems else weekItems
    return if (typeFilter == "All") items else items.filter { it.kind == typeFilter }
}

private fun parseInspectionDate(raw: String): LocalDate? = runCatching {
    LocalDate.parse(raw.take(10))
}.getOrNull()

private fun inspectionKind(type: String): String = when (type.uppercase()) {
    "FIELD" -> "Field"
    "NURSERY" -> "Nursery"
    "CURING" -> "Curing"
    "VALIDATION" -> "Validation"
    else -> type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun kindTone(kind: String, isHighRisk: Boolean): GlTone = when {
    isHighRisk && kind == "Field" -> GlTone.Warning
    kind == "Validation" -> GlTone.Gold
    kind in listOf("Field", "Nursery", "Curing") -> GlTone.Primary
    else -> GlTone.Default
}
