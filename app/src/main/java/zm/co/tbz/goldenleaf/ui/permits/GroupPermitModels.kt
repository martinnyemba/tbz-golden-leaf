package zm.co.tbz.goldenleaf.ui.permits

import zm.co.tbz.goldenleaf.data.remote.dto.GroupPermitEntryDto

data class GroupPermitHeaderForm(
    val licensePlate: String = "",
    val originProvince: String = "",
    val originDistrict: String = "",
    val destinationSalesFloor: String = "",
    val purpose: String = "SALES",
    val comments: String = "",
)

data class GroupPermitEntryForm(
    val localKey: String = "",
    val growerId: String = "",
    val growerLabel: String = "",
    val growerSearch: String = "",
    val growerCategory: String = "SMALL_SCALE",
    val totalBales: String = "",
    val totalWeightKg: String = "",
    val notes: String = "",
)

data class GroupPermitCreateUiState(
    val step: Int = 1,
    val header: GroupPermitHeaderForm = GroupPermitHeaderForm(),
    val entries: List<GroupPermitEntryForm> = emptyList(),
    val draftLocalId: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
)

data class GroupPermitValidateState(
    val permitToken: String = "",
    val isVerifying: Boolean = false,
    val verifyError: String? = null,
    val result: GroupPermitValidateResult? = null,
)

data class GroupPermitValidateResult(
    val valid: Boolean,
    val permitNumber: String?,
    val licensePlate: String?,
    val destination: String?,
    val purpose: String?,
    val totalBales: Int?,
    val totalWeightKg: Double?,
    val validFrom: String?,
    val validTo: String?,
    val status: String?,
    val entries: List<GroupPermitEntryDto>,
    val detail: String? = null,
)

data class GroupPermitsHubStats(
    val total: Int = 0,
    val pending: Int = 0,
    val approved: Int = 0,
    val returned: Int = 0,
    val drafts: Int = 0,
)
