package zm.co.tbz.goldenleaf.ui.registration

object GrowerFormChoices {
    val growerTypes = listOf("INDIVIDUAL" to "Individual", "COMPANY" to "Company")
    val sexOptions = listOf("MALE" to "Male", "FEMALE" to "Female")
    val categories = listOf(
        "SMALL_SCALE" to "Small Scale",
        "COMMERCIAL" to "Commercial",
        "COMPANY" to "Company",
    )
    val phoneCountries = listOf("Zambia" to "+260")
    val statusFilters = listOf(
        "All",
        "DRAFT",
        "PENDING",
        "RETURNED_FOR_CORRECTION",
        "APPROVED",
        "ACTIVE",
        "REJECTED",
        "SUSPENDED",
    )
    val syncFilters = listOf("All", "pending", "synced", "failed")
    val listStatusFilters = listOf("All", "Active", "Pending", "High risk", "Drafts")
    val updateQueueFilters = listOf("All", "Pending", "Failed", "Needs review", "Synced")
    val wizardStepLabels = listOf("Type", "Identity", "Farm", "Photos")
    val growerTypeOptions = listOf(
        "small" to Triple("Small-scale", "Individual grower · ≤ 5 ha", "94"),
        "commercial" to Triple("Commercial", "5–50 ha · Independent farm", "24"),
        "company" to Triple("Company", "Registered company / cooperative", "10"),
    )
    val cooperativeOptions = listOf("None", "Eastern Tobacco Farmers Coop", "Chadiza Burley Group")
    val curingStructureOptions = listOf("Open shed", "Closed barn", "Flue barn", "Sun-cured frame")
    val tobaccoTypeOptions = listOf("Burley", "Virginia (Flue-cured)", "Oriental", "Dark fire-cured")
}

data class PersonalDetailsForm(
    val growerType: String = "INDIVIDUAL",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val nrcNumber: String = "",
    val sex: String = "MALE",
    val dateOfBirth: String = "",
    val category: String = "SMALL_SCALE",
    val country: String = "Zambia",
    val localPhone: String = "",
    val email: String = "",
    val address: String = "",
    val townVillage: String = "",
    val provinceId: String = "",
    val districtId: String = "",
    val gpsLatitude: String = "",
    val gpsLongitude: String = "",
    val profilePhotoPath: String? = null,
    val idFrontPath: String? = null,
    val idBackPath: String? = null,
    val growerTypeKey: String = "small",
    val nextOfKin: String = "",
    val cooperative: String = "None",
    val villageChief: String = "",
    val totalAreaHa: String = "",
    val tobaccoAreaHa: String = "",
    val tobaccoTypeName: String = "",
    val curingStructure: String = "",
    val estimatedYieldKg: String = "",
    val farmNotes: String = "",
    val farmOverviewPhotoPath: String? = null,
    val curingBarnPhotoPath: String? = null,
    val signatureCaptured: Boolean = false,
    val consentGiven: Boolean = true,
)

data class CropDetailsForm(
    val tobaccoTypeId: String = "",
    val sponsorId: String? = null,
    val isSelfSponsored: Boolean = false,
    val hectarage: String = "",
    val numberOfBarns: String = "",
    val barnTypeId: String = "",
    val stringsPerBarn: String = "",
    val gpsLatitude: String = "",
    val gpsLongitude: String = "",
)

fun calculateYieldPerHa(hectarage: Double): Int =
    if (hectarage <= 9.0) 1500 else 3000

fun formatPhoneNumber(country: String, localNumber: String): String {
    val digits = localNumber.filter { it.isDigit() }
    val normalized = digits.trimStart('0')
    return when (country) {
        "Zambia" -> "+260$normalized"
        else -> normalized
    }
}

/** Display row for grower list — from entity or handoff preview. */
data class GrowerListItem(
    val localId: String,
    val name: String,
    val tbzId: String,
    val subtitle: String,
    val statusLabel: String,
    val statusTone: zm.co.tbz.goldenleaf.ui.components.GlTone,
    val syncStatus: String,
    val riskLabel: String,
    val riskTone: zm.co.tbz.goldenleaf.ui.components.GlTone,
    val flagged: Boolean = false,
    val isPreview: Boolean = false,
)

data class GrowerUpdateQueueItem(
    val localId: String,
    val growerName: String,
    val tbzId: String,
    val changedFields: String,
    val status: String,
    val timestamp: String,
    val error: String? = null,
)

val handoffGrowerListItems = listOf(
    GrowerListItem("preview-1", "Mary Phiri", "TBZ-2024-04412", "Chadiza · 4.2 ha · Burley", "Active", zm.co.tbz.goldenleaf.ui.components.GlTone.Success, "pending", "High", zm.co.tbz.goldenleaf.ui.components.GlTone.Danger, flagged = true, isPreview = true),
    GrowerListItem("preview-2", "Charles Tembo", "TBZ-2024-03128", "Katete · 2.7 ha · Virginia", "Active", zm.co.tbz.goldenleaf.ui.components.GlTone.Success, "synced", "Low", zm.co.tbz.goldenleaf.ui.components.GlTone.Success, isPreview = true),
    GrowerListItem("preview-3", "Gladys Mwale", "TBZ-2024-02981", "Lundazi · 1.8 ha · Burley", "Review", zm.co.tbz.goldenleaf.ui.components.GlTone.Warning, "synced", "Med", zm.co.tbz.goldenleaf.ui.components.GlTone.Warning, isPreview = true),
    GrowerListItem("preview-4", "Felix Sakala", "TBZ-2024-04501", "Petauke · 6.4 ha · Virginia", "Active", zm.co.tbz.goldenleaf.ui.components.GlTone.Success, "synced", "Low", zm.co.tbz.goldenleaf.ui.components.GlTone.Success, isPreview = true),
    GrowerListItem("preview-5", "Loveness Banda", "TBZ-2024-04610", "Chipata · 0.9 ha · Burley", "Pending", zm.co.tbz.goldenleaf.ui.components.GlTone.Warning, "pending", "Med", zm.co.tbz.goldenleaf.ui.components.GlTone.Warning, isPreview = true),
    GrowerListItem("preview-6", "Joseph Zulu", "TBZ-2023-09812", "Mambwe · 3.1 ha · Burley", "Draft", zm.co.tbz.goldenleaf.ui.components.GlTone.Default, "draft", "Low", zm.co.tbz.goldenleaf.ui.components.GlTone.Success, isPreview = true),
)

val handoffGrowerUpdateItems = listOf(
    GrowerUpdateQueueItem("preview-u1", "Mary Phiri", "TBZ-2024-04412", "Phone, address", "pending", "12 May 09:42"),
    GrowerUpdateQueueItem("preview-u2", "Charles Tembo", "TBZ-2024-03128", "District, province", "synced", "11 May 14:20"),
    GrowerUpdateQueueItem("preview-u3", "Loveness Banda", "TBZ-2024-04610", "NRC, date of birth", "failed", "10 May 08:15", "NRC already exists on another registered grower"),
    GrowerUpdateQueueItem("preview-u4", "Felix Sakala", "TBZ-2024-04501", "Email, phone", "pending", "10 May 16:30"),
    GrowerUpdateQueueItem("preview-u5", "Gladys Mwale", "TBZ-2024-02981", "Address, town", "needs_review", "08 May 11:10", "Grower status changed by approver — please confirm edit before retrying"),
)
