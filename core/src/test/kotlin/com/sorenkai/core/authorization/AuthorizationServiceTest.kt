package com.sorenkai.core.authorization

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Permission
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment
import com.sorenkai.core.repository.InMemoryRoleAssignmentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests verifying authorization evaluation and default-denial policies.
 */
class AuthorizationServiceTest {
    private lateinit var repository: InMemoryRoleAssignmentRepository
    private lateinit var authService: AuthorizationService

    private val moderatorId = IdentityId("moderator-1")
    private val adminId = IdentityId("admin-1")
    private val ownerId = IdentityId("owner-1")
    private val unassignedId = IdentityId("unassigned-user")

    @BeforeEach
    fun setUp() {
        repository = InMemoryRoleAssignmentRepository()
        repository.saveAssignment(RoleAssignment(moderatorId, Role.MODERATOR))
        repository.saveAssignment(RoleAssignment(adminId, Role.ADMIN))
        repository.saveAssignment(RoleAssignment(ownerId, Role.OWNER))
        authService = DefaultAuthorizationService(repository)
    }

    @Test
    fun `evaluates default denial when identity has no assigned role`() {
        for (permission in Permission.entries) {
            val decision = authService.evaluate(unassignedId, permission)
            assertTrue(decision.isDenied)
            assertFalse(decision.isGranted)
            assertInstanceOf(AuthorizationDecision.Denied::class.java, decision)
            assertEquals(unassignedId, decision.identityId)
            assertEquals(null, decision.role)
            assertEquals(permission, decision.permission)
            assertTrue(decision.reason.isNotBlank())
            assertFalse(authService.isAuthorized(unassignedId, permission))
        }
    }

    @Test
    fun `evaluates default denial when role is null`() {
        for (permission in Permission.entries) {
            val decision = authService.evaluate(role = null, permission = permission)
            assertTrue(decision.isDenied)
            assertFalse(decision.isGranted)
            assertInstanceOf(AuthorizationDecision.Denied::class.java, decision)
            assertEquals(null, decision.role)
            assertEquals(permission, decision.permission)
            assertFalse(authService.isAuthorized(role = null, permission = permission))
        }
    }

    @Test
    fun `evaluates moderator identity permissions correctly`() {
        // Granted permissions
        val granted = setOf(Permission.MODERATE_DISCUSSIONS, Permission.SUSPEND_ACCOUNTS)
        for (permission in granted) {
            val decision = authService.evaluate(moderatorId, permission)
            assertTrue(decision.isGranted)
            assertFalse(decision.isDenied)
            assertInstanceOf(AuthorizationDecision.Granted::class.java, decision)
            assertEquals(moderatorId, decision.identityId)
            assertEquals(Role.MODERATOR, decision.role)
            assertEquals(permission, decision.permission)
            assertTrue(authService.isAuthorized(moderatorId, permission))
        }

        // Denied permissions
        val denied = Permission.entries.toSet() - granted
        for (permission in denied) {
            val decision = authService.evaluate(moderatorId, permission)
            assertTrue(decision.isDenied)
            assertFalse(decision.isGranted)
            assertInstanceOf(AuthorizationDecision.Denied::class.java, decision)
            assertEquals(moderatorId, decision.identityId)
            assertEquals(Role.MODERATOR, decision.role)
            assertEquals(permission, decision.permission)
            assertFalse(authService.isAuthorized(moderatorId, permission))
        }
    }

    @Test
    fun `evaluates admin identity permissions correctly including inheritance`() {
        val granted =
            setOf(
                Permission.MODERATE_DISCUSSIONS,
                Permission.SUSPEND_ACCOUNTS,
                Permission.DECIDE_SUSPENSIONS,
                Permission.BAN_ACCOUNTS,
                Permission.MANAGE_MODERATORS,
                Permission.REVIEW_APPEALS,
                Permission.ACCESS_SITE_ANALYTICS,
            )
        for (permission in granted) {
            val decision = authService.evaluate(adminId, permission)
            assertTrue(decision.isGranted, "Expected $permission to be granted for admin")
            assertTrue(authService.isAuthorized(adminId, permission))
        }

        val denied = setOf(Permission.ADD_ADMINS, Permission.REMOVE_ADMINS)
        for (permission in denied) {
            val decision = authService.evaluate(adminId, permission)
            assertTrue(decision.isDenied, "Expected $permission to be denied for admin")
            assertFalse(authService.isAuthorized(adminId, permission))
        }
    }

    @Test
    fun `evaluates owner identity permissions correctly for all permissions`() {
        for (permission in Permission.entries) {
            val decision = authService.evaluate(ownerId, permission)
            assertTrue(decision.isGranted, "Expected $permission to be granted for owner")
            assertTrue(authService.isAuthorized(ownerId, permission))
        }
    }

    @Test
    fun `returns effective permissions for identities`() {
        assertEquals(emptySet<Permission>(), authService.getEffectivePermissions(unassignedId))
        assertEquals(Role.MODERATOR.permissions, authService.getEffectivePermissions(moderatorId))
        assertEquals(Role.ADMIN.permissions, authService.getEffectivePermissions(adminId))
        assertEquals(Role.OWNER.permissions, authService.getEffectivePermissions(ownerId))
    }
}
