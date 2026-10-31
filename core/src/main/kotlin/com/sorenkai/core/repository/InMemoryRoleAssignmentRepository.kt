package com.sorenkai.core.repository

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory implementation of [RoleAssignmentRepository].
 */
class InMemoryRoleAssignmentRepository : RoleAssignmentRepository {
    private val assignments = ConcurrentHashMap<IdentityId, RoleAssignment>()

    override fun findRoleByIdentity(identityId: IdentityId): Role? = assignments[identityId]?.role

    override fun findAssignmentByIdentity(identityId: IdentityId): RoleAssignment? = assignments[identityId]

    override fun saveAssignment(assignment: RoleAssignment): RoleAssignment {
        assignments[assignment.identityId] = assignment
        return assignment
    }

    override fun removeAssignment(identityId: IdentityId): Boolean = assignments.remove(identityId) != null

    override fun findAllAssignments(): List<RoleAssignment> = assignments.values.toList()
}
