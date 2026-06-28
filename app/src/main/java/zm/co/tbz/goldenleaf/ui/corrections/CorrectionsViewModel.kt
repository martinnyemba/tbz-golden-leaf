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
    val subtitle: String,
    val reason: String?,
    val syncStatus: String,
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun syncAll() {
        viewModelScope.launch {
            growerRepository.triggerSync()
            permitRepository.triggerSync()
        }
    }
}

private fun GrowerEntity.toInboxItem() = CorrectionInboxItem(
    localId = local_id,
    type = CorrectionType.GROWER,
    title = "$first_name $last_name",
    subtitle = tbz_id ?: nrc_number,
    reason = null,
    syncStatus = sync_status,
)

private fun TransportPermitEntity.toInboxItem() = CorrectionInboxItem(
    localId = local_id,
    type = CorrectionType.TRANSPORT_PERMIT,
    title = permit_number ?: "Transport permit",
    subtitle = grower_name ?: license_plate,
    reason = correction_reason,
    syncStatus = sync_status,
)

private fun GroupPermitEntity.toInboxItem() = CorrectionInboxItem(
    localId = local_id,
    type = CorrectionType.GROUP_PERMIT,
    title = permit_number ?: "Group permit",
    subtitle = license_plate,
    reason = correction_reason,
    syncStatus = sync_status,
)
