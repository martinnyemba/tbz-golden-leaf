package zm.co.tbz.goldenleaf.ui.marketing

object MarketingFormChoices {
    val baleStatuses = listOf("BOUGHT" to "Bought", "REJECTED" to "Rejected")
    val rejectionReasons = listOf(
        "NESTED" to "Nested",
        "HIGH_MOISTURE" to "High Moisture",
        "LOW_MOISTURE" to "Low Moisture",
        "NTRM" to "NTRM",
        "OVERWEIGHT" to "Overweight",
        "UNDERWEIGHT" to "Underweight",
        "NO_SALE" to "No Sale",
    )
    val tobaccoTypes = listOf(
        "FLUE_CURED" to "Flue Cured Virginia",
        "BURLEY" to "Burley",
        "DARK_FIRED" to "Dark Fired Tobacco",
    )
}

data class BaleRowForm(
    val localRowId: String = java.util.UUID.randomUUID().toString(),
    val baleTicketNumber: String = "",
    val gradeMark: String = "",
    val weightKg: String = "",
    val status: String = "BOUGHT",
    val rejectionReason: String = "",
    val buyerId: String = "",
    val tobaccoType: String = "FLUE_CURED",
    val pricePerKg: String = "",
)

data class SalesBatchForm(
    val growerId: String = "",
    val season: String = "",
    val salesfloorId: String = "",
    val buyerId: String = "",
    val saleDate: String = "",
)

data class VerifiedPermitInfo(
    val valid: Boolean = false,
    val permitNumber: String? = null,
    val growerName: String? = null,
    val tbzId: String? = null,
    val totalBales: Int? = null,
    val remainingBales: Int? = null,
    val status: String? = null,
)

data class SalesCaptureState(
    val step: Int = 1,
    val permitToken: String = "",
    val salesfloorId: String = "",
    val verifiedPermit: VerifiedPermitInfo? = null,
    val batch: SalesBatchForm = SalesBatchForm(),
    val bales: List<BaleRowForm> = listOf(BaleRowForm()),
    val stepErrors: Map<String, String> = emptyMap(),
    val rowErrors: Map<String, Map<String, String>> = emptyMap(),
    val isVerifying: Boolean = false,
    val isSaving: Boolean = false,
    val verifyError: String? = null,
    val saveError: String? = null,
)

data class MarketingHubStats(
    val pendingSales: Int = 0,
    val syncedBales: Int = 0,
    val failedSales: Int = 0,
)
