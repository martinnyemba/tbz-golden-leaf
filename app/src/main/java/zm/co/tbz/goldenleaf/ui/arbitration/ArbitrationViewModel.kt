package zm.co.tbz.goldenleaf.ui.arbitration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.repository.InspectionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ArbitrationViewModel @Inject constructor(
    private val inspectionRepository: InspectionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ArbitrationUiState(
            form = ArbitrationForm(
                arbitrationDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            ),
        ),
    )
    val uiState: StateFlow<ArbitrationUiState> = _uiState.asStateFlow()

    fun updateForm(transform: (ArbitrationForm) -> ArbitrationForm) {
        _uiState.update { it.copy(form = transform(it.form), fieldErrors = emptyMap()) }
    }

    fun submit(onSubmitted: () -> Unit) {
        val form = _uiState.value.form
        val errors = validate(form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                inspectionRepository.submitArbitration(
                    baleId = form.baleId,
                    arbitrationDate = form.arbitrationDate,
                    isRejected = form.isRejected,
                    rejectionReason = form.rejectionReason.ifBlank { null },
                    inspectorRemarks = form.inspectorRemarks,
                    isFinal = form.isFinal,
                )
                _uiState.value = ArbitrationUiState(
                    form = ArbitrationForm(
                        arbitrationDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                    ),
                    saveSuccess = true,
                )
                onSubmitted()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, saveError = e.message ?: "Submit failed")
                }
            }
        }
    }

    private fun validate(form: ArbitrationForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.baleId.isBlank()) errors["baleId"] = "Bale ID is required"
        if (form.arbitrationDate.isBlank()) errors["arbitrationDate"] = "Date is required"
        if (form.isRejected && form.rejectionReason.isBlank()) {
            errors["rejectionReason"] = "Rejection reason is required when rejected"
        }
        return errors
    }
}
