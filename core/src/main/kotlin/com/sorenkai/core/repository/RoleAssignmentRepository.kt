package com.sorenkai.core.repository

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment

/**
 * Repository interface for managing administrative role assignments to identities.
 */
interface RoleAssignmentRepository {
    /**
     * Resolves the assigned [Role] for a given [identityId], or returns `null` if unassigned.
     */
    fun findRoleByIdentity(identityId: IdentityId): Role?

    /**
     * Resolves the full [RoleAssignment] record for a given [identityId], or returns `null` if unassigned.
     */
    fun findAssignmentByIdentity(identityId: IdentityId): RoleAssignment?

    /**
     * Persists or updates a [RoleAssignment] for an identity.
     */
    fun saveAssignment(assignment: RoleAssignment): RoleAssignment

    /**
     * Removes the active role assignment for the specified [identityId].
     *
     * @return `true` if an assignment existed and was removed; `false` otherwise.
     */
    fun removeAssignment(identityId: IdentityId): Boolean

    /**
     * Returns all active role assignments.
     */
    fun findAllAssignments(): List<RoleAssignment>
}
