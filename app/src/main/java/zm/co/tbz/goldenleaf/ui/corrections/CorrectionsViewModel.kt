package zm.co.tbz.goldenleaf.ui.corrections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zm.co.tbz.goldenleaf.data.local.entity.GroupPermitEntity
import zm.co.tbz.goldenleaf.data.local.entity.GrowerEntity
import zm.co.tbz.goldenleaf.data.local.entity.SyncStatuses
import zm.co.tbz.goldenleaf.data.local.entity.TransportPermitEntity
import zm.co.tbz.goldenleaf.data.repository.GrowerRepository
import zm.co.tbz.goldenleaf.data.repository.PermitRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class CorrectionType {
    GROWER,
    TRANSPORT_PERMIT,
    GROUP_PERMIT,
}

data class CorrectionInboxItem(
    val localId: String,
    val type: CorrectionType,
    val title: String,
    val ref: String,
    val reason: String?,
    val returnedLabel: String,
    val syncStatus: String,
    val icon: String,
)

@HiltViewModel
class CorrectionsViewModel @Inject constructor(
    private val growerRepository: GrowerRepository,
    private val permitRepository: PermitRepository,
) : ViewModel() {

    private val growers = growerRepository.observeGrowers()
    private val transportPermits = permitRepository.observeTransportPermits()
    private val groupPermits = permitRepository.observeGroupPermits()

    val items = combine(growers, transportPermits, groupPermits) { growerList, permits, groups ->
        buildList {
            growerList.filter {
                it.status == "RETURNED_FOR_CORRECTION" ||
                    it.sync_status == SyncStatuses.NEEDS_REVIEW
            }.forEach { grower ->
                add(grower.toInboxItem())
            }
            permits.filter { it.status == "RETURNED_FOR_CORRECTION" }.forEach { permit ->
                add(permit.toInboxItem())
            }
            groups.filter { it.status == "RETURNED_FOR_CORRECTION" }.forEach { group ->
                add(group.toInboxItem())
            }
        }.sortedBy { it.title.lowercase() }
            .ifEmpty { handoffCorrectionItems }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun syncAll() {
        viewModelScope.launch {
            growerRepository.triggerSync()
            permitRepository.triggerSync()
        }
    }
}

private val returnedDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

private fun formatReturned(timestamp: Long): String =
    returnedDateFormat.format(Date(timestamp))

private fun mapSyncStatus(status: String): String = when (status) {
    SyncStatuses.PENDING, SyncStatuses.NEEDS_REVIEW -> "pending"
    SyncStatuses.FAILED -> "failed"
    else -> "synced"
}

private fun GrowerEntity.toInboxItem() = CorrectionInboxItem(
    localId = local_id,
    type = CorrectionType.GROWER,
    title = listOf(first_name, middle_name, last_name).filterNot { it.isNullOrBlank() }.joinToString(" "),
    ref = tbz_id ?: nrc_number,
    reason = last_sync_error ?: "Returned for correction — review grower details",
    returnedLabel = formatReturned(updated_at_local),
    syncStatus = mapSyncStatus(sync_status),
    icon = "profile",
)

private fun TransportPermitEntity.toInboxItem() = CorrectionInboxItem(
    localId = local_id,
    type = CorrectionType.TRANSPORT_PERMIT,
    title = "Permit ${permit_number ?: local_id.take(8)}",
    ref = permit_number ?: local_id.take(8).uppercase(),
    reason = correction_reason ?: last_sync_error,
    returnedLabel = formatReturned(updated_at_local),
    syncStatus = mapSyncStatus(sync_status),
    icon = "permit",
)

/** Handoff mock inbox when no returned records exist — matches ScreenCorrectionsInbox artboard 08. */
private val handoffCorrectionItems = listOf(
    CorrectionInboxItem(
        localId = "preview-grower",
        type = CorrectionType.GROWER,
        title = "Mary Phiri",
        ref = "TBZ-2024-04412",
        reason = "NRC does not match ID document photo",
        returnedLabel = "09 May 2026",
        syncStatus = "pending",
        icon = "profile",
    ),
    CorrectionInboxItem(
        localId = "preview-permit-1",
        type = CorrectionType.TRANSPORT_PERMIT,
        title = "Permit PRM-9812",
        ref = "PRM-9812",
        reason = "Vehicle plate mismatch — re-enter plate number",
        returnedLabel = "10 May 2026",
        syncStatus = "pending",
        icon = "permit",
    ),
    CorrectionInboxItem(
        localId = "preview-permit-2",
        type = CorrectionType.TRANSPORT_PERMIT,
        title = "Permit PRM-9790",
        ref = "PRM-9790",
        reason = "Destination sales floor not available — select alternate",
        returnedLabel = "08 May 2026",
        syncStatus = "failed",
        icon = "permit",
    ),
    CorrectionInboxItem(
        localId = "preview-group",
        type = CorrectionType.GROUP_PERMIT,
        title = "Group Permit GRP-4481",
        ref = "GRP-4481",
        reason = "Manifest has only 1 grower — minimum 2 required",
        returnedLabel = "07 May 2026",
        syncStatus = "pending",
        icon = "users",
    ),
)

private fun GroupPermitEntity.toInboxItem() = CorrectionInboxItem(
    localId = local_id,
    type = CorrectionType.GROUP_PERMIT,
    title = "Group Permit ${permit_number ?: local_id.take(8)}",
    ref = permit_number ?: local_id.take(8).uppercase(),
    reason = correction_reason ?: last_sync_error,
    returnedLabel = formatReturned(updated_at_local),
    syncStatus = mapSyncStatus(sync_status),
    icon = "users",
)
