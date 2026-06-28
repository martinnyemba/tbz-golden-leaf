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
