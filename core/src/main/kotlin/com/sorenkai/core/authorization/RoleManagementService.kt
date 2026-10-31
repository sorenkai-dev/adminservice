package com.sorenkai.core.authorization

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment
import com.sorenkai.core.repository.RoleAssignmentRepository

/**
 * Domain service managing administrative role assignments and enforcing role-management policies.
 */
interface RoleManagementService {
    /**
     * Assigns [role] to [targetId] on behalf of [actorId].
     *
     * @throws UnauthorizedRoleOperationException if [actorId] is not authorized to appoint [role].
     */
    fun assignRole(
        actorId: IdentityId,
        targetId: IdentityId,
        role: Role,
    ): RoleAssignment

    /**
     * Revokes the active administrative role from [targetId] on behalf of [actorId].
     *
     * @return `true` if a role was revoked; `false` if [targetId] had no active role.
     * @throws UnauthorizedRoleOperationException if [actorId] is not authorized to remove the target's role.
     */
    fun revokeRole(
        actorId: IdentityId,
        targetId: IdentityId,
    ): Boolean
}

/**
 * Default implementation of [RoleManagementService].
 */
class DefaultRoleManagementService(
    private val repository: RoleAssignmentRepository,
    private val policy: RoleManagementPolicy = DefaultRoleManagementPolicy(),
) : RoleManagementService {
    override fun assignRole(
        actorId: IdentityId,
        targetId: IdentityId,
        role: Role,
    ): RoleAssignment {
        val actorRole = repository.findRoleByIdentity(actorId)
        if (!policy.canAppointRole(actorRole, role)) {
            throw UnauthorizedRoleOperationException(
                "Actor '$actorId' with role '${actorRole?.name}' is not authorized to assign role '${role.name}'.",
            )
        }
        val assignment =
            RoleAssignment(
                identityId = targetId,
                role = role,
                assignedBy = actorId,
            )
        return repository.saveAssignment(assignment)
    }

    override fun revokeRole(
        actorId: IdentityId,
        targetId: IdentityId,
    ): Boolean {
        val targetRole = repository.findRoleByIdentity(targetId) ?: return false
        val actorRole = repository.findRoleByIdentity(actorId)
        if (!policy.canRemoveRole(actorRole, targetRole)) {
            throw UnauthorizedRoleOperationException(
                "Actor '$actorId' with role '${actorRole?.name}' is not authorized to remove role '${targetRole.name}'.",
            )
        }
        return repository.removeAssignment(targetId)
    }
}
