package com.sorenkai.core.authorization

import com.sorenkai.core.model.Permission
import com.sorenkai.core.model.Role

/**
 * Domain policy enforcing restrictions on administrative role appointments and revocations.
 */
interface RoleManagementPolicy {
    /**
     * Determines whether an actor with [actorRole] is permitted to appoint an identity to [targetRole].
     */
    fun canAppointRole(
        actorRole: Role?,
        targetRole: Role,
    ): Boolean

    /**
     * Determines whether an actor with [actorRole] is permitted to remove [targetRole] from an identity.
     */
    fun canRemoveRole(
        actorRole: Role?,
        targetRole: Role,
    ): Boolean
}

/**
 * Default implementation of [RoleManagementPolicy] enforcing fixed role-management restrictions:
 * - Moderator cannot manage any roles.
 * - Admin can manage Moderator roles, but cannot appoint or remove Admin or Owner roles.
 * - Owner can manage Moderator and Admin roles, but cannot appoint or remove Owner roles.
 */
class DefaultRoleManagementPolicy : RoleManagementPolicy {
    override fun canAppointRole(
        actorRole: Role?,
        targetRole: Role,
    ): Boolean {
        if (actorRole == null) return false
        return when (targetRole) {
            Role.MODERATOR -> actorRole.hasPermission(Permission.MANAGE_MODERATORS)
            Role.ADMIN -> actorRole.hasPermission(Permission.ADD_ADMINS)
            Role.OWNER -> false
        }
    }

    override fun canRemoveRole(
        actorRole: Role?,
        targetRole: Role,
    ): Boolean {
        if (actorRole == null) return false
        return when (targetRole) {
            Role.MODERATOR -> actorRole.hasPermission(Permission.MANAGE_MODERATORS)
            Role.ADMIN -> actorRole.hasPermission(Permission.REMOVE_ADMINS)
            Role.OWNER -> false
        }
    }
}
