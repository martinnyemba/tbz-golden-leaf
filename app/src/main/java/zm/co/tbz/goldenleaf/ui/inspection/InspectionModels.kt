package zm.co.tbz.goldenleaf.ui.inspection

object InspectionTypes {
    const val GROWER_VALIDATION = "GROWER_VALIDATION"
    const val NURSERY_INSPECTION = "NURSERY_INSPECTION"
    const val FIELD_INSPECTION = "FIELD_INSPECTION"
    const val CURING_INSPECTION = "CURING_INSPECTION"
}

object InspectionFormChoices {
    val inspectionTypes = listOf(
        InspectionTypes.GROWER_VALIDATION to "Grower Validation",
        InspectionTypes.NURSERY_INSPECTION to "Nursery Inspection",
        InspectionTypes.FIELD_INSPECTION to "Field Inspection",
        InspectionTypes.CURING_INSPECTION to "Curing Inspection",
    )

    val typeFilters = listOf("All") + inspectionTypes.map { it.first }

    val statusFilters = listOf(
        "All",
        "SCHEDULED",
        "IN_PROGRESS",
        "COMPLETED",
        "CANCELLED",
    )

    val syncFilters = listOf("All", "pending", "synced", "failed", "needs_review")

    // Field inspection (section 4.9)
    val fieldCropStages = listOf(
        "MAIN_FIELD" to "Main Field",
        "TOPPING" to "Topping",
        "REAPING" to "Reaping",
        "CURING" to "Curing",
        "GRADING" to "Grading",
        "STORAGE" to "Storage",
    )
    val plantPopulations = listOf(
        "FULL" to "Full",
        "ACCEPTABLE" to "Acceptable",
        "POOR" to "Poor",
    )
    val cropUniformities = listOf(
        "UNIFORM" to "Uniform",
        "MODERATE" to "Moderate",
        "VARIABLE" to "Variable",
    )
    val fertilizerApplications = listOf(
        "ADEQUATE" to "Adequate",
        "INADEQUATE" to "Inadequate",
        "EXCESSIVE" to "Excessive",
        "NONE" to "None",
    )
    val pestDiseaseStatuses = listOf(
        "NONE" to "None",
        "LOW" to "Low",
        "MODERATE" to "Moderate",
        "HIGH" to "High",
    )
    val weedControls = listOf(
        "GOOD" to "Good",
        "FAIR" to "Fair",
        "POOR" to "Poor",
    )
    val irrigationStatuses = listOf(
        "ADEQUATE" to "Adequate",
        "INADEQUATE" to "Inadequate",
        "NOT_APPLICABLE" to "Not Applicable",
    )

    // Nursery inspection (section 4.10)
    val germinationStatuses = listOf(
        "GOOD" to "Good",
        "FAIR" to "Fair",
        "POOR" to "Poor",
    )
    val seedlingConditions = listOf(
        "HEALTHY" to "Healthy",
        "STRESSED" to "Stressed",
        "DISEASED" to "Diseased",
    )
    val waterSources = listOf(
        "RAINFALL" to "Rainfall",
        "BOREHOLE" to "Borehole",
        "RIVER" to "River",
        "IRRIGATION" to "Irrigation",
        "OTHER" to "Other",
    )
    val yesNo = listOf(true to "Yes", false to "No")

    // Curing inspection (section 4.11)
    val curingBarnTypes = listOf(
        "FLUE_CURED" to "Flue Cured Barn",
        "BULK_CURE" to "Bulk Cure Barn",
        "AIR_CURED" to "Air Cured Barn",
        "FIRE_CURED" to "Fire Cured Barn",
    )
    val fuelSources = listOf(
        "WOOD" to "Wood",
        "COAL" to "Coal",
        "GAS" to "Gas",
        "ELECTRIC" to "Electric",
        "OTHER" to "Other",
    )
    val curingStatuses = listOf(
        "NOT_STARTED" to "Not Started",
        "IN_PROGRESS" to "In Progress",
        "COMPLETED" to "Completed",
    )
    val leafQualities = listOf(
        "GOOD" to "Good",
        "FAIR" to "Fair",
        "POOR" to "Poor",
    )
    val gradingStatuses = listOf(
        "NOT_GRADED" to "Not Graded",
        "IN_PROGRESS" to "In Progress",
        "GRADED" to "Graded",
    )

    // Grower validation (section 4.12)
    val validationCropStages = fieldCropStages
    val validationSexOptions = listOf("MALE" to "Male", "FEMALE" to "Female")
    val validationTobaccoTypes = listOf(
        "FLUE_CURED" to "Flue Cured Virginia",
        "BURLEY" to "Burley",
        "DARK_FIRED" to "Dark Fired Tobacco",
    )
    val validationBarnTypes = curingBarnTypes + listOf(
        "CONVENTIONAL" to "Conventional Barn",
        "TRADITIONAL" to "Traditional Barn",
        "ROCKET" to "Rocket Barn",
        "CHONGOLOLO" to "Chongololo Barn",
        "MATOPE" to "Matope Barn",
        "KAMANGA" to "Kamanga Barn",
        "TUNNEL" to "Tunnel Barn",
        "LIVE_BARN" to "Live Barn",
    )
    val barnCapacityOptions = listOf(true to "YES", false to "NO")
}

data class ScheduleInspectionForm(
    val growerId: String = "",
    val growerName: String = "",
    val growerSearch: String = "",
    val inspectorId: String = "",
    val inspectorName: String = "",
    val inspectionType: String = InspectionTypes.GROWER_VALIDATION,
    val scheduledDate: String = "",
    val provinceId: String = "",
    val districtId: String = "",
    val districtText: String = "",
    val notes: String = "",
)

data class FieldInspectionForm(
    val growerId: String = "",
    val growerName: String = "",
    val inspectionLocalId: String = "",
    val transplantedHectarage: String = "",
    val cropStage: String = "MAIN_FIELD",
    val plantPopulation: String = "FULL",
    val cropUniformity: String = "UNIFORM",
    val fertilizerApplication: String = "ADEQUATE",
    val pestDiseaseStatus: String = "NONE",
    val weedControl: String = "GOOD",
    val irrigationStatus: String = "ADEQUATE",
    val gpsLatitude: String = "",
    val gpsLongitude: String = "",
    val deviceId: String = "",
    val inspectorRemarks: String = "",
)

data class NurseryInspectionForm(
    val growerId: String = "",
    val growerName: String = "",
    val inspectionLocalId: String = "",
    val seedVariety: String = "",
    val nurserySizeBeds: String = "",
    val dateOfSowing: String = "",
    val germinationStatus: String = "GOOD",
    val seedlingCondition: String = "HEALTHY",
    val waterSource: String = "RAINFALL",
    val pestDiseasePresent: Boolean = false,
    val pestDiseaseNotes: String = "",
    val fertilizerUsed: Boolean = false,
    val fertilizerNotes: String = "",
    val chemicalsUsed: Boolean = false,
    val chemicalsNotes: String = "",
    val gpsLatitude: String = "",
    val gpsLongitude: String = "",
    val deviceId: String = "",
    val inspectorRemarks: String = "",
)

data class CuringInspectionForm(
    val growerId: String = "",
    val growerName: String = "",
    val inspectionLocalId: String = "",
    val numberOfBarns: String = "",
    val barnType: String = "FLUE_CURED",
    val curingCycles: String = "",
    val fuelSource: String = "WOOD",
    val curingStatus: String = "NOT_STARTED",
    val leafQuality: String = "GOOD",
    val gradingStatus: String = "NOT_GRADED",
    val gpsLatitude: String = "",
    val gpsLongitude: String = "",
    val deviceId: String = "",
    val inspectorRemarks: String = "",
)

data class ValidationForm(
    val growerId: String = "",
    val growerName: String = "",
    val growerNrc: String = "",
    val inspectionLocalId: String = "",
    val cropAllocationId: String = "",
    val sex: String = "MALE",
    val gpsLatitude: String = "",
    val gpsLongitude: String = "",
    val cropStage: String = "MAIN_FIELD",
    val tobaccoType: String = "FLUE_CURED",
    val tobaccoVariety: String = "",
    val validatedHectarage: String = "",
    val yieldPerHa: String = "",
    val sponsorId: String = "",
    val barnType: String = "FLUE_CURED",
    val numberOfBarns: String = "",
    val barnCapacitySufficient: Boolean = true,
    val stakeholdersPresent: Set<String> = emptySet(),
    val provinceId: String = "",
    val districtId: String = "",
    val districtText: String = "",
    val deviceId: String = "",
    val inspectorRemarks: String = "",
)

fun inspectionTypeLabel(type: String): String =
    InspectionFormChoices.inspectionTypes.firstOrNull { it.first == type }?.second
        ?: type.replace('_', ' ')
