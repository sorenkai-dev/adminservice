package com.sorenkai.core.repository

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests verifying [InMemoryRoleAssignmentRepository] operations and integrity.
 */
class RoleAssignmentRepositoryTest {
    private lateinit var repository: InMemoryRoleAssignmentRepository

    private val user1 = IdentityId("user-1")
    private val user2 = IdentityId("user-2")

    @BeforeEach
    fun setUp() {
        repository = InMemoryRoleAssignmentRepository()
    }

    @Test
    fun `saves and retrieves role assignments`() {
        assertNull(repository.findRoleByIdentity(user1))
        assertNull(repository.findAssignmentByIdentity(user1))

        val assignment = RoleAssignment(user1, Role.MODERATOR)
        repository.saveAssignment(assignment)

        assertEquals(Role.MODERATOR, repository.findRoleByIdentity(user1))
        val retrieved = repository.findAssignmentByIdentity(user1)
        assertNotNull(retrieved)
        assertEquals(user1, retrieved?.identityId)
        assertEquals(Role.MODERATOR, retrieved?.role)
    }

    @Test
    fun `overwrites previous role assignment when saving new role for same identity`() {
        repository.saveAssignment(RoleAssignment(user1, Role.MODERATOR))
        assertEquals(Role.MODERATOR, repository.findRoleByIdentity(user1))

        repository.saveAssignment(RoleAssignment(user1, Role.ADMIN))
        assertEquals(Role.ADMIN, repository.findRoleByIdentity(user1))
    }

    @Test
    fun `removes role assignment`() {
        repository.saveAssignment(RoleAssignment(user1, Role.MODERATOR))
        assertTrue(repository.removeAssignment(user1))
        assertNull(repository.findRoleByIdentity(user1))
        assertFalse(repository.removeAssignment(user1))
    }

    @Test
    fun `finds all assignments`() {
        repository.saveAssignment(RoleAssignment(user1, Role.MODERATOR))
        repository.saveAssignment(RoleAssignment(user2, Role.ADMIN))

        val all = repository.findAllAssignments()
        assertEquals(2, all.size)
        assertTrue(all.any { it.identityId == user1 && it.role == Role.MODERATOR })
        assertTrue(all.any { it.identityId == user2 && it.role == Role.ADMIN })
    }
}
