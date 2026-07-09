package zm.co.tbz.goldenleaf.ui.permits

object PermitFormChoices {
    val purposes = listOf(
        "SALES" to "Sales",
        "PROCESSING" to "Processing",
        "STORAGE" to "Storage",
        "EXPORT" to "Export",
    )
    val growerCategories = listOf(
        "SMALL_SCALE" to "Small Scale",
        "COMMERCIAL" to "Commercial",
        "COMPANY" to "Company",
    )
    val statusFilters = listOf("All", "PENDING", "APPROVED", "REJECTED", "RETURNED_FOR_CORRECTION")
}

data class PermitRequestForm(
    val growerId: String = "",
    val growerSearch: String = "",
    val growerCategory: String = "SMALL_SCALE",
    val totalBales: String = "",
    val totalWeightKg: String = "",
    val licensePlate: String = "",
    val originProvince: String = "",
    val originDistrict: String = "",
    val destinationSalesFloor: String = "",
    val purpose: String = "SALES",
    val buyerId: String = "",
    val isBought: Boolean = false,
    val buyerAccepted: Boolean = false,
    val comments: String = "",
)

data class PermitRequestUiState(
    val form: PermitRequestForm = PermitRequestForm(),
    val step: Int = 1,
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val searchQuery: String = "",
    val statusFilter: String = "All",
)

data class PermitValidateState(
    val permitToken: String = "",
    val salesfloorId: String = "",
    val isVerifying: Boolean = false,
    val verifyError: String? = null,
    val salesfloorError: String? = null,
    val result: zm.co.tbz.goldenleaf.ui.marketing.VerifiedPermitInfo? = null,
)

data class PermitsHubStats(
    val transportPermits: Int = 0,
    val pendingRequests: Int = 0,
    val groupPermits: Int = 0,
)

data class PermitReviewForm(
    val action: String = "approve",
    val validFrom: String = "",
    val validTo: String = "",
    val reason: String = "",
)

data class PermitReviewUiState(
    val form: PermitReviewForm = PermitReviewForm(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitSuccess: Boolean = false,
)

data class PermitCorrectionForm(
    val growerId: String = "",
    val growerCategory: String = "SMALL_SCALE",
    val totalBales: String = "",
    val totalWeightKg: String = "",
    val licensePlate: String = "",
    val originProvince: String = "",
    val originDistrict: String = "",
    val destinationSalesFloor: String = "",
    val purpose: String = "SALES",
    val buyerId: String = "",
    val isBought: Boolean = false,
    val buyerAccepted: Boolean = false,
    val comments: String = "",
)

data class PermitCorrectionUiState(
    val form: PermitCorrectionForm = PermitCorrectionForm(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
)

object PermitReviewActions {
    const val APPROVE = "approve"
    const val REJECT = "reject"
    const val RETURN = "return_for_correction"
}
