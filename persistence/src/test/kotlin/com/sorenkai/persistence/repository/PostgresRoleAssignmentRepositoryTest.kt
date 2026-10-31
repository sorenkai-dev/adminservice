package com.sorenkai.persistence.repository

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment
import com.sorenkai.persistence.PostgresIntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * PostgreSQL integration tests for [PostgresRoleAssignmentRepository].
 */
class PostgresRoleAssignmentRepositoryTest : PostgresIntegrationTestBase() {
    private lateinit var repository: PostgresRoleAssignmentRepository

    @BeforeEach
    fun initRepository() {
        repository = PostgresRoleAssignmentRepository(dataSource)
    }

    @Test
    fun `saves and retrieves role assignments for fixed administrative roles`() {
        val modId = IdentityId("moderator-1")
        val adminId = IdentityId("admin-1")
        val ownerId = IdentityId("owner-1")
        val assignedAt =
            java.time.Instant
                .now()
                .truncatedTo(java.time.temporal.ChronoUnit.MILLIS)

        val modAssignment = RoleAssignment(modId, Role.MODERATOR, assignedAt, IdentityId("system"))
        val adminAssignment = RoleAssignment(adminId, Role.ADMIN, assignedAt, IdentityId("owner-1"))
        val ownerAssignment = RoleAssignment(ownerId, Role.OWNER, assignedAt, null)

        repository.saveAssignment(modAssignment)
        repository.saveAssignment(adminAssignment)
        repository.saveAssignment(ownerAssignment)

        assertEquals(Role.MODERATOR, repository.findRoleByIdentity(modId))
        assertEquals(Role.ADMIN, repository.findRoleByIdentity(adminId))
        assertEquals(Role.OWNER, repository.findRoleByIdentity(ownerId))

        assertEquals(modAssignment, repository.findAssignmentByIdentity(modId))
        assertEquals(adminAssignment, repository.findAssignmentByIdentity(adminId))
        assertEquals(ownerAssignment, repository.findAssignmentByIdentity(ownerId))
    }

    @Test
    fun `returns null for unassigned identity`() {
        val unknownId = IdentityId("unknown-user")
        assertNull(repository.findRoleByIdentity(unknownId))
        assertNull(repository.findAssignmentByIdentity(unknownId))
    }

    @Test
    fun `updates existing role assignment for an identity`() {
        val identity = IdentityId("user-promoted")
        repository.saveAssignment(RoleAssignment(identity, Role.MODERATOR))
        assertEquals(Role.MODERATOR, repository.findRoleByIdentity(identity))

        repository.saveAssignment(RoleAssignment(identity, Role.ADMIN))
        assertEquals(Role.ADMIN, repository.findRoleByIdentity(identity))
    }

    @Test
    fun `removes role assignment and returns boolean indicating existence`() {
        val identity = IdentityId("user-to-remove")
        repository.saveAssignment(RoleAssignment(identity, Role.MODERATOR))

        val removedFirst = repository.removeAssignment(identity)
        assertTrue(removedFirst)
        assertNull(repository.findRoleByIdentity(identity))

        val removedSecond = repository.removeAssignment(identity)
        assertFalse(removedSecond)
    }

    @Test
    fun `lists all active role assignments ordered by identity`() {
        repository.saveAssignment(RoleAssignment(IdentityId("charlie"), Role.MODERATOR))
        repository.saveAssignment(RoleAssignment(IdentityId("alice"), Role.ADMIN))
        repository.saveAssignment(RoleAssignment(IdentityId("bob"), Role.OWNER))

        val all = repository.findAllAssignments()
        assertEquals(3, all.size)
        assertEquals(IdentityId("alice"), all[0].identityId)
        assertEquals(Role.ADMIN, all[0].role)
        assertEquals(IdentityId("bob"), all[1].identityId)
        assertEquals(Role.OWNER, all[1].role)
        assertEquals(IdentityId("charlie"), all[2].identityId)
        assertEquals(Role.MODERATOR, all[2].role)
    }
}
