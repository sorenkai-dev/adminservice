package com.sorenkai.core.authorization

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment
import com.sorenkai.core.repository.InMemoryRoleAssignmentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests verifying administrative role-management policies and restrictions.
 */
class RoleManagementTest {
    private lateinit var repository: InMemoryRoleAssignmentRepository
    private lateinit var rolePolicy: RoleManagementPolicy
    private lateinit var roleService: RoleManagementService

    private val moderatorActor = IdentityId("moderator-actor")
    private val adminActor = IdentityId("admin-actor")
    private val ownerActor = IdentityId("owner-actor")
    private val unassignedActor = IdentityId("unassigned-actor")

    private val targetUser = IdentityId("target-user")

    @BeforeEach
    fun setUp() {
        repository = InMemoryRoleAssignmentRepository()
        repository.saveAssignment(RoleAssignment(moderatorActor, Role.MODERATOR))
        repository.saveAssignment(RoleAssignment(adminActor, Role.ADMIN))
        repository.saveAssignment(RoleAssignment(ownerActor, Role.OWNER))

        rolePolicy = DefaultRoleManagementPolicy()
        roleService = DefaultRoleManagementService(repository, rolePolicy)
    }

    @Test
    fun `policy enforces that unassigned identity cannot appoint or remove any role`() {
        for (targetRole in Role.entries) {
            assertFalse(rolePolicy.canAppointRole(actorRole = null, targetRole = targetRole))
            assertFalse(rolePolicy.canRemoveRole(actorRole = null, targetRole = targetRole))
        }

        assertThrows(UnauthorizedRoleOperationException::class.java) {
            roleService.assignRole(unassignedActor, targetUser, Role.MODERATOR)
        }
    }

    @Test
    fun `policy enforces that moderator cannot appoint or remove any role`() {
        for (targetRole in Role.entries) {
            assertFalse(rolePolicy.canAppointRole(actorRole = Role.MODERATOR, targetRole = targetRole))
            assertFalse(rolePolicy.canRemoveRole(actorRole = Role.MODERATOR, targetRole = targetRole))
        }

        assertThrows(UnauthorizedRoleOperationException::class.java) {
            roleService.assignRole(moderatorActor, targetUser, Role.MODERATOR)
        }
        assertThrows(UnauthorizedRoleOperationException::class.java) {
            roleService.assignRole(moderatorActor, targetUser, Role.ADMIN)
        }
    }

    @Test
    fun `policy allows admin to appoint and remove moderator`() {
        assertTrue(rolePolicy.canAppointRole(actorRole = Role.ADMIN, targetRole = Role.MODERATOR))
        assertTrue(rolePolicy.canRemoveRole(actorRole = Role.ADMIN, targetRole = Role.MODERATOR))

        val assignment = roleService.assignRole(adminActor, targetUser, Role.MODERATOR)
        assertEquals(Role.MODERATOR, assignment.role)
        assertEquals(targetUser, assignment.identityId)
        assertEquals(adminActor, assignment.assignedBy)
        assertEquals(Role.MODERATOR, repository.findRoleByIdentity(targetUser))

        val removed = roleService.revokeRole(adminActor, targetUser)
        assertTrue(removed)
        assertNull(repository.findRoleByIdentity(targetUser))
    }

    @Test
    fun `policy strictly prevents admins from appointing other admins`() {
        assertFalse(rolePolicy.canAppointRole(actorRole = Role.ADMIN, targetRole = Role.ADMIN))

        val ex =
            assertThrows(UnauthorizedRoleOperationException::class.java) {
                roleService.assignRole(adminActor, targetUser, Role.ADMIN)
            }
        assertTrue(ex.message!!.contains("ADMIN", ignoreCase = true))
        assertNull(repository.findRoleByIdentity(targetUser))
    }

    @Test
    fun `policy strictly prevents admins from removing other admins`() {
        assertFalse(rolePolicy.canRemoveRole(actorRole = Role.ADMIN, targetRole = Role.ADMIN))

        // Set up target as admin first
        repository.saveAssignment(RoleAssignment(targetUser, Role.ADMIN))

        val ex =
            assertThrows(UnauthorizedRoleOperationException::class.java) {
                roleService.revokeRole(adminActor, targetUser)
            }
        assertTrue(ex.message!!.contains("ADMIN", ignoreCase = true))
        assertEquals(Role.ADMIN, repository.findRoleByIdentity(targetUser))
    }

    @Test
    fun `policy strictly prevents admins from appointing or removing owners`() {
        assertFalse(rolePolicy.canAppointRole(actorRole = Role.ADMIN, targetRole = Role.OWNER))
        assertFalse(rolePolicy.canRemoveRole(actorRole = Role.ADMIN, targetRole = Role.OWNER))

        assertThrows(UnauthorizedRoleOperationException::class.java) {
            roleService.assignRole(adminActor, targetUser, Role.OWNER)
        }

        repository.saveAssignment(RoleAssignment(targetUser, Role.OWNER))
        assertThrows(UnauthorizedRoleOperationException::class.java) {
            roleService.revokeRole(adminActor, targetUser)
        }
    }

    @Test
    fun `policy allows owner to appoint and remove admins and moderators`() {
        // Moderator
        assertTrue(rolePolicy.canAppointRole(actorRole = Role.OWNER, targetRole = Role.MODERATOR))
        assertTrue(rolePolicy.canRemoveRole(actorRole = Role.OWNER, targetRole = Role.MODERATOR))

        // Admin
        assertTrue(rolePolicy.canAppointRole(actorRole = Role.OWNER, targetRole = Role.ADMIN))
        assertTrue(rolePolicy.canRemoveRole(actorRole = Role.OWNER, targetRole = Role.ADMIN))

        // Assign Admin
        val adminAssignment = roleService.assignRole(ownerActor, targetUser, Role.ADMIN)
        assertEquals(Role.ADMIN, adminAssignment.role)
        assertEquals(ownerActor, adminAssignment.assignedBy)
        assertEquals(Role.ADMIN, repository.findRoleByIdentity(targetUser))

        // Revoke Admin
        assertTrue(roleService.revokeRole(ownerActor, targetUser))
        assertNull(repository.findRoleByIdentity(targetUser))

        // Assign Moderator
        val modAssignment = roleService.assignRole(ownerActor, targetUser, Role.MODERATOR)
        assertEquals(Role.MODERATOR, modAssignment.role)
        assertEquals(ownerActor, modAssignment.assignedBy)
        assertEquals(Role.MODERATOR, repository.findRoleByIdentity(targetUser))

        // Revoke Moderator
        assertTrue(roleService.revokeRole(ownerActor, targetUser))
        assertNull(repository.findRoleByIdentity(targetUser))
    }

    @Test
    fun `policy denies appointing or removing owner role without custom permission`() {
        assertFalse(rolePolicy.canAppointRole(actorRole = Role.OWNER, targetRole = Role.OWNER))
        assertFalse(rolePolicy.canRemoveRole(actorRole = Role.OWNER, targetRole = Role.OWNER))

        assertThrows(UnauthorizedRoleOperationException::class.java) {
            roleService.assignRole(ownerActor, targetUser, Role.OWNER)
        }

        repository.saveAssignment(RoleAssignment(targetUser, Role.OWNER))
        assertThrows(UnauthorizedRoleOperationException::class.java) {
            roleService.revokeRole(ownerActor, targetUser)
        }
    }

    @Test
    fun `revoking role of identity with no role returns false`() {
        val nonExistentUser = IdentityId("nobody")
        val removed = roleService.revokeRole(ownerActor, nonExistentUser)
        assertFalse(removed)
    }

    @Test
    fun `reassigning role updates existing assignment`() {
        roleService.assignRole(ownerActor, targetUser, Role.MODERATOR)
        assertEquals(Role.MODERATOR, repository.findRoleByIdentity(targetUser))

        roleService.assignRole(ownerActor, targetUser, Role.ADMIN)
        assertEquals(Role.ADMIN, repository.findRoleByIdentity(targetUser))
    }
}
