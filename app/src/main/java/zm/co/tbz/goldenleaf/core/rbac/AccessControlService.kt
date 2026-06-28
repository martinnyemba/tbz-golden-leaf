package zm.co.tbz.goldenleaf.core.rbac

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import zm.co.tbz.goldenleaf.data.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessControlService @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    fun hasPermission(permission: String): Boolean {
        val profile = runBlocking { profileRepository.profile.first() } ?: return false
        return profile.permissions[permission] == true
    }

    fun hasModule(module: String): Boolean {
        val profile = runBlocking { profileRepository.profile.first() } ?: return false
        return profile.modules[module] == true
    }

    fun hasAnyRole(vararg roles: String): Boolean {
        val profile = runBlocking { profileRepository.profile.first() } ?: return false
        return profile.roles.any { it in roles }
    }

    suspend fun hasPermissionAsync(permission: String): Boolean {
        val profile = profileRepository.profile.first() ?: return false
        return profile.permissions[permission] == true
    }

    suspend fun canAccessRegistration(): Boolean =
        hasPermissionAsync("growers.create_grower") ||
            hasPermissionAsync("growers.update_grower") ||
            hasPermissionAsync("growers.approve_grower")

    suspend fun canAccessInspection(): Boolean =
        hasPermissionAsync("validation.create_validation") ||
            hasPermissionAsync("validation.approve_validation") ||
            hasAnyRoleAsync("INSPECTOR", "RTI")

    suspend fun canAccessMarketing(): Boolean =
        hasPermissionAsync("bale.create_bale") || hasAnyRoleAsync("DATA_CLERK")

    suspend fun canAccessPermits(): Boolean =
        hasPermissionAsync("permits.create_permit") ||
            hasPermissionAsync("permits.approve_permit")

    suspend fun canAccessArbitration(): Boolean =
        hasPermissionAsync("arbitration.create_arbitration") || hasAnyRoleAsync("ARBITRATOR")

    private suspend fun hasAnyRoleAsync(vararg roles: String): Boolean {
        val profile = profileRepository.profile.first() ?: return false
        return profile.roles.any { it in roles }
    }
}
