package com.sorenkai.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests verifying role definitions, permission sets, and explicit permission inheritance.
 */
class RoleInheritanceTest {
    @Test
    fun `moderator role has expected direct and effective permissions`() {
        val direct = Role.MODERATOR.directPermissions
        val expectedDirect =
            setOf(
                Permission.MODERATE_DISCUSSIONS,
                Permission.SUSPEND_ACCOUNTS,
            )
        assertEquals(expectedDirect, direct)
        assertEquals(expectedDirect, Role.MODERATOR.permissions)
        assertTrue(Role.MODERATOR.inheritedRoles.isEmpty())

        assertTrue(Role.MODERATOR.hasPermission(Permission.MODERATE_DISCUSSIONS))
        assertTrue(Role.MODERATOR.hasPermission(Permission.SUSPEND_ACCOUNTS))
        assertFalse(Role.MODERATOR.hasPermission(Permission.DECIDE_SUSPENSIONS))
        assertFalse(Role.MODERATOR.hasPermission(Permission.BAN_ACCOUNTS))
        assertFalse(Role.MODERATOR.hasPermission(Permission.MANAGE_MODERATORS))
        assertFalse(Role.MODERATOR.hasPermission(Permission.REVIEW_APPEALS))
        assertFalse(Role.MODERATOR.hasPermission(Permission.ACCESS_SITE_ANALYTICS))
        assertFalse(Role.MODERATOR.hasPermission(Permission.ADD_ADMINS))
        assertFalse(Role.MODERATOR.hasPermission(Permission.REMOVE_ADMINS))
    }

    @Test
    fun `admin role inherits moderator permissions and has direct admin permissions`() {
        val direct = Role.ADMIN.directPermissions
        val expectedDirect =
            setOf(
                Permission.DECIDE_SUSPENSIONS,
                Permission.BAN_ACCOUNTS,
                Permission.MANAGE_MODERATORS,
                Permission.REVIEW_APPEALS,
                Permission.ACCESS_SITE_ANALYTICS,
            )
        assertEquals(expectedDirect, direct)
        assertEquals(setOf(Role.MODERATOR), Role.ADMIN.inheritedRoles)

        val expectedEffective = Role.MODERATOR.permissions + expectedDirect
        assertEquals(expectedEffective, Role.ADMIN.permissions)

        // Inherited moderator permissions
        assertTrue(Role.ADMIN.hasPermission(Permission.MODERATE_DISCUSSIONS))
        assertTrue(Role.ADMIN.hasPermission(Permission.SUSPEND_ACCOUNTS))

        // Direct admin permissions
        assertTrue(Role.ADMIN.hasPermission(Permission.DECIDE_SUSPENSIONS))
        assertTrue(Role.ADMIN.hasPermission(Permission.BAN_ACCOUNTS))
        assertTrue(Role.ADMIN.hasPermission(Permission.MANAGE_MODERATORS))
        assertTrue(Role.ADMIN.hasPermission(Permission.REVIEW_APPEALS))
        assertTrue(Role.ADMIN.hasPermission(Permission.ACCESS_SITE_ANALYTICS))

        // Owner permissions must be denied to admin
        assertFalse(Role.ADMIN.hasPermission(Permission.ADD_ADMINS))
        assertFalse(Role.ADMIN.hasPermission(Permission.REMOVE_ADMINS))
    }

    @Test
    fun `owner role inherits admin and moderator permissions and has direct owner permissions`() {
        val direct = Role.OWNER.directPermissions
        val expectedDirect =
            setOf(
                Permission.ADD_ADMINS,
                Permission.REMOVE_ADMINS,
            )
        assertEquals(expectedDirect, direct)
        assertEquals(setOf(Role.ADMIN), Role.OWNER.inheritedRoles)

        val expectedEffective = Role.ADMIN.permissions + expectedDirect
        assertEquals(expectedEffective, Role.OWNER.permissions)

        // Moderator permissions
        assertTrue(Role.OWNER.hasPermission(Permission.MODERATE_DISCUSSIONS))
        assertTrue(Role.OWNER.hasPermission(Permission.SUSPEND_ACCOUNTS))

        // Admin permissions
        assertTrue(Role.OWNER.hasPermission(Permission.DECIDE_SUSPENSIONS))
        assertTrue(Role.OWNER.hasPermission(Permission.BAN_ACCOUNTS))
        assertTrue(Role.OWNER.hasPermission(Permission.MANAGE_MODERATORS))
        assertTrue(Role.OWNER.hasPermission(Permission.REVIEW_APPEALS))
        assertTrue(Role.OWNER.hasPermission(Permission.ACCESS_SITE_ANALYTICS))

        // Owner permissions
        assertTrue(Role.OWNER.hasPermission(Permission.ADD_ADMINS))
        assertTrue(Role.OWNER.hasPermission(Permission.REMOVE_ADMINS))
    }

    @Test
    fun `verifies exact number of roles and permissions defined`() {
        assertEquals(3, Role.entries.size)
        assertEquals(9, Permission.entries.size)
    }

    @Test
    fun `verifies role inheritance relationship helper`() {
        assertTrue(Role.ADMIN.inheritsFrom(Role.MODERATOR))
        assertFalse(Role.ADMIN.inheritsFrom(Role.OWNER))
        assertFalse(Role.ADMIN.inheritsFrom(Role.ADMIN))

        assertTrue(Role.OWNER.inheritsFrom(Role.ADMIN))
        assertTrue(Role.OWNER.inheritsFrom(Role.MODERATOR))
        assertFalse(Role.OWNER.inheritsFrom(Role.OWNER))

        assertFalse(Role.MODERATOR.inheritsFrom(Role.ADMIN))
        assertFalse(Role.MODERATOR.inheritsFrom(Role.OWNER))
    }
}
