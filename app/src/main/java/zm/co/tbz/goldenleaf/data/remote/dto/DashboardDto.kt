package zm.co.tbz.goldenleaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardResponse(
    val pending_registrations: Int = 0,
    val returned_registrations: Int = 0,
    val pending_permits: Int = 0,
    val returned_permits: Int = 0,
    val scheduled_inspections: Int = 0,
    val returned_group_permits: Int = 0,
    val bales_today: Int = 0,
    val bales_this_season: Int = 0,
    val unread_notifications: Int = 0
)
