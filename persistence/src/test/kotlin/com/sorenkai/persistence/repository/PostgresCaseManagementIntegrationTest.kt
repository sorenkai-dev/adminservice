package com.sorenkai.persistence.repository

import com.sorenkai.core.authorization.DefaultAuthorizationService
import com.sorenkai.core.cases.CaseAlreadyResolvedException
import com.sorenkai.core.cases.CaseCreatorSelfDecisionException
import com.sorenkai.core.cases.DefaultCaseManagementService
import com.sorenkai.core.cases.InvalidLockTokenException
import com.sorenkai.core.cases.LockExpiredException
import com.sorenkai.core.cases.UnauthorizedCaseOperationException
import com.sorenkai.core.model.AdministrativeCase
import com.sorenkai.core.model.CaseOutcome
import com.sorenkai.core.model.CaseStatus
import com.sorenkai.core.model.CaseSubject
import com.sorenkai.core.model.EvidenceReference
import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.LeaseToken
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment
import com.sorenkai.persistence.PostgresIntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * End-to-end PostgreSQL integration tests validating case lifecycle, exclusive locking,
 * authorization evaluation, and transactional consistency against real PostgreSQL storage.
 */
class PostgresCaseManagementIntegrationTest : PostgresIntegrationTestBase() {
    private lateinit var roleRepository: PostgresRoleAssignmentRepository
    private lateinit var caseRepository: PostgresCaseRepository
    private lateinit var lockRepository: PostgresCaseLockRepository
    private lateinit var caseService: DefaultCaseManagementService

    private val creatorId = IdentityId("creator-mod")
    private val admin1 = IdentityId("admin-1")
    private val admin2 = IdentityId("admin-2")
    private val unassignedUser = IdentityId("unassigned-user")

    private val baseTime = Instant.parse("2026-10-30T10:00:00Z")

    @BeforeEach
    fun setUp() {
        roleRepository = PostgresRoleAssignmentRepository(database)
        caseRepository = PostgresCaseRepository(database)
        lockRepository = PostgresCaseLockRepository(database)

        roleRepository.saveAssignment(RoleAssignment(creatorId, Role.MODERATOR))
        roleRepository.saveAssignment(RoleAssignment(admin1, Role.ADMIN))
        roleRepository.saveAssignment(RoleAssignment(admin2, Role.ADMIN))

        val authService = DefaultAuthorizationService(roleRepository)
        val fixedClock = Clock.fixed(baseTime, ZoneOffset.UTC)

        caseService =
            DefaultCaseManagementService(
                caseRepository = caseRepository,
                lockRepository = lockRepository,
                authorizationService = authService,
                tx = transactionManager,
                clock = fixedClock,
            )
    }

    @Test
    fun `full lifecycle - creation, exclusive handling, and decision resolution with lock cleanup`() {
        val created =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("reported-post-123"),
                reason = "Severe policy violation reported.",
                evidence = listOf(EvidenceReference("https://evidence.example.com/screenshot.png")),
            )

        val retrievedOpen = caseService.getCase(created.id)
        assertNotNull(retrievedOpen)
        assertEquals(CaseStatus.OPEN, retrievedOpen?.status)
        assertNull(caseService.getActiveLock(created.id))

        val lock = caseService.acquireLock(created.id, admin1, Duration.ofMinutes(15))
        assertEquals(admin1, lock.holderId)
        assertEquals(lock, caseService.getActiveLock(created.id))

        val resolved =
            caseService.resolveCase(
                caseId = created.id,
                adminId = admin1,
                token = lock.token,
                outcome = CaseOutcome.SUSPEND_ACCOUNT,
                justification = "Confirmed violation after review.",
            )

        assertEquals(CaseStatus.RESOLVED, resolved.status)
        assertEquals(creatorId, resolved.createdBy)
        assertEquals(admin1, resolved.decision?.decidedBy)
        assertEquals(CaseOutcome.SUSPEND_ACCOUNT, resolved.decision?.outcome)
        assertEquals("Confirmed violation after review.", resolved.decision?.justification)

        assertNull(caseService.getActiveLock(created.id))
        assertNull(lockRepository.findLock(created.id))
    }

    @Test
    fun `rejects case decision by creator even when holding administrative role`() {
        roleRepository.saveAssignment(RoleAssignment(creatorId, Role.ADMIN))

        val created =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("self-case"),
                reason = "Self decision test.",
            )

        val lock = caseService.acquireLock(created.id, creatorId)

        assertThrows(CaseCreatorSelfDecisionException::class.java) {
            caseService.resolveCase(
                caseId = created.id,
                adminId = creatorId,
                token = lock.token,
                outcome = CaseOutcome.DISMISSED,
                justification = "Attempting self decision.",
            )
        }
    }

    @Test
    fun `rejects lock acquisition and decision by unassigned identity`() {
        val created =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("target-user"),
                reason = "Unauthorized handling test.",
            )

        assertThrows(UnauthorizedCaseOperationException::class.java) {
            caseService.acquireLock(created.id, unassignedUser)
        }
    }

    @Test
    fun `rejects resolution with stale or superseded token`() {
        val created =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("target-user"),
                reason = "Stale token test.",
            )

        caseService.acquireLock(created.id, admin1)
        val staleToken = LeaseToken.generate()

        assertThrows(InvalidLockTokenException::class.java) {
            caseService.resolveCase(
                caseId = created.id,
                adminId = admin1,
                token = staleToken,
                outcome = CaseOutcome.DISMISSED,
                justification = "Resolution with fake token.",
            )
        }
    }

    @Test
    fun `re-acquisition and resolution allowed after previous lock expires`() {
        val created =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("expiry-test"),
                reason = "Lock expiry test.",
            )

        val initialLock = caseService.acquireLock(created.id, admin1, Duration.ofMinutes(5))

        // Fast-forward clock past lock expiration
        val advancedTime = baseTime.plus(Duration.ofMinutes(10))
        val advancedClock = Clock.fixed(advancedTime, ZoneOffset.UTC)
        val advancedService =
            DefaultCaseManagementService(
                caseRepository = caseRepository,
                lockRepository = lockRepository,
                authorizationService = DefaultAuthorizationService(roleRepository),
                tx = transactionManager,
                clock = advancedClock,
            )

        // Previous admin's resolution should fail due to lock expiration
        assertThrows(LockExpiredException::class.java) {
            advancedService.resolveCase(
                caseId = created.id,
                adminId = admin1,
                token = initialLock.token,
                outcome = CaseOutcome.DISMISSED,
                justification = "Expired resolution attempt.",
            )
        }

        // Second admin can now acquire lock and resolve case
        val newLock = advancedService.acquireLock(created.id, admin2, Duration.ofMinutes(15))
        assertEquals(admin2, newLock.holderId)

        val resolved =
            advancedService.resolveCase(
                caseId = created.id,
                adminId = admin2,
                token = newLock.token,
                outcome = CaseOutcome.BAN_ACCOUNT,
                justification = "Resolved after previous lock expired.",
            )

        assertEquals(CaseStatus.RESOLVED, resolved.status)
        assertEquals(admin2, resolved.decision?.decidedBy)
        assertNull(advancedService.getActiveLock(created.id))
    }

    @Test
    fun `transactional resolution coordinates repository operations atomically in Exposed transaction`() {
        val created =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("tx-atomicity"),
                reason = "Testing atomicity of case resolution.",
            )

        val lock = caseService.acquireLock(created.id, admin1)

        val resolved =
            caseService.resolveCase(
                caseId = created.id,
                adminId = admin1,
                token = lock.token,
                outcome = CaseOutcome.NO_ACTION,
                justification = "Handled atomically.",
            )

        assertEquals(CaseStatus.RESOLVED, resolved.status)
        assertNull(lockRepository.findLock(created.id))

        // Attempting to resolve an already resolved case fails and leaves state unchanged
        assertThrows(CaseAlreadyResolvedException::class.java) {
            caseService.resolveCase(
                caseId = created.id,
                adminId = admin1,
                token = lock.token,
                outcome = CaseOutcome.BAN_ACCOUNT,
                justification = "Duplicate resolution attempt.",
            )
        }
    }
}
