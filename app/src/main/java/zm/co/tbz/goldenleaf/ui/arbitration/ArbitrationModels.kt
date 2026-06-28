package zm.co.tbz.goldenleaf.ui.arbitration

object ArbitrationFormChoices {
    val rejectionReasons = listOf(
        "NESTED" to "Nested",
        "HIGH_MOISTURE" to "High moisture",
        "LOW_MOISTURE" to "Low moisture",
        "NTRM" to "NTRM",
        "OVERWEIGHT" to "Overweight",
        "UNDERWEIGHT" to "Underweight",
        "NO_SALE" to "No sale",
    )
    val rejectedOptions = listOf(true to "Yes", false to "No")
}

data class ArbitrationForm(
    val baleId: String = "",
    val arbitrationDate: String = "",
    val isRejected: Boolean = false,
    val rejectionReason: String = "",
    val inspectorRemarks: String = "",
    val isFinal: Boolean = false,
)

data class ArbitrationUiState(
    val form: ArbitrationForm = ArbitrationForm(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
)
