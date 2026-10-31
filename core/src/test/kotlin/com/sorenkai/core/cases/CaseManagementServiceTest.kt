package com.sorenkai.core.cases

import com.sorenkai.core.authorization.AuthorizationService
import com.sorenkai.core.authorization.DefaultAuthorizationService
import com.sorenkai.core.model.AdministrativeCase
import com.sorenkai.core.model.CaseDecision
import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.CaseOutcome
import com.sorenkai.core.model.CaseStatus
import com.sorenkai.core.model.CaseSubject
import com.sorenkai.core.model.EvidenceReference
import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.LeaseToken
import com.sorenkai.core.model.Permission
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment
import com.sorenkai.core.repository.InMemoryCaseLockRepository
import com.sorenkai.core.repository.InMemoryCaseRepository
import com.sorenkai.core.repository.InMemoryRoleAssignmentRepository
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

/**
 * Unit tests verifying case management lifecycle, exclusive locking, decision accountability,
 * and authorization rules using a controllable clock.
 */
class CaseManagementServiceTest {
    private lateinit var roleRepository: InMemoryRoleAssignmentRepository
    private lateinit var authService: AuthorizationService
    private lateinit var caseRepository: InMemoryCaseRepository
    private lateinit var lockRepository: InMemoryCaseLockRepository
    private lateinit var mutableClock: MutableClock
    private lateinit var caseService: CaseManagementService

    private val creatorId = IdentityId("creator-mod")
    private val admin1 = IdentityId("admin-1")
    private val admin2 = IdentityId("admin-2")
    private val unassignedUser = IdentityId("user-no-role")

    private val baseTime = Instant.parse("2026-10-30T10:00:00Z")

    @BeforeEach
    fun setUp() {
        roleRepository = InMemoryRoleAssignmentRepository()
        roleRepository.saveAssignment(RoleAssignment(creatorId, Role.MODERATOR))
        roleRepository.saveAssignment(RoleAssignment(admin1, Role.ADMIN))
        roleRepository.saveAssignment(RoleAssignment(admin2, Role.ADMIN))

        authService = DefaultAuthorizationService(roleRepository)
        caseRepository = InMemoryCaseRepository()
        lockRepository = InMemoryCaseLockRepository()
        mutableClock = MutableClock(baseTime)

        caseService =
            DefaultCaseManagementService(
                caseRepository = caseRepository,
                lockRepository = lockRepository,
                authorizationService = authService,
                clock = mutableClock,
                defaultLockDuration = Duration.ofMinutes(15),
            )
    }

    @Test
    fun `creates a valid open administrative case`() {
        val subject = CaseSubject("reported-post-123")
        val reason = "Spam links detected."
        val evidence = listOf(EvidenceReference("https://evidence.example.com/item-1"))

        val created =
            caseService.createCase(
                creatorId = creatorId,
                subject = subject,
                reason = reason,
                evidence = evidence,
            )

        assertNotNull(created.id)
        assertEquals(creatorId, created.createdBy)
        assertEquals(baseTime, created.createdAt)
        assertEquals(subject, created.subject)
        assertEquals(reason, created.reason)
        assertEquals(evidence, created.evidence)
        assertEquals(CaseStatus.OPEN, created.status)
        assertNull(created.decision)

        val retrieved = caseService.getCase(created.id)
        assertEquals(created, retrieved)
    }

    @Test
    fun `viewing an open case does not acquire a lock`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-456"),
                reason = "Inappropriate profile banner.",
            )

        val retrieved = caseService.getCase(case.id)
        assertNotNull(retrieved)
        assertNull(caseService.getActiveLock(case.id))
        assertNull(lockRepository.findLock(case.id))
    }

    @Test
    fun `case creator remains permitted to view their created case`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-789"),
                reason = "Reported violation.",
            )

        val retrieved = caseService.getCase(case.id)
        assertEquals(creatorId, retrieved?.createdBy)
    }

    @Test
    fun `authorized administrator acquires exclusive handling lock`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-1"),
                reason = "Investigation required.",
            )

        val lock = caseService.acquireLock(case.id, admin1, Duration.ofMinutes(10))

        assertEquals(case.id, lock.caseId)
        assertEquals(admin1, lock.holderId)
        assertNotNull(lock.token)
        assertEquals(baseTime, lock.acquiredAt)
        assertEquals(baseTime.plus(Duration.ofMinutes(10)), lock.expiresAt)
        assertTrue(lock.isActive(baseTime))
        assertEquals(lock, caseService.getActiveLock(case.id))
    }

    @Test
    fun `prevents a second administrator from acquiring an active lock`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-1"),
                reason = "Investigation required.",
            )

        caseService.acquireLock(case.id, admin1, Duration.ofMinutes(10))

        val ex =
            assertThrows(CaseAlreadyLockedException::class.java) {
                caseService.acquireLock(case.id, admin2, Duration.ofMinutes(10))
            }

        assertEquals(case.id, ex.caseId)
        assertEquals(admin1, ex.holderId)
        assertEquals(baseTime.plus(Duration.ofMinutes(10)), ex.expiresAt)
    }

    @Test
    fun `releasing a lock without resolving allows another administrator to acquire lock`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-1"),
                reason = "Investigation required.",
            )

        val lock1 = caseService.acquireLock(case.id, admin1, Duration.ofMinutes(10))
        assertNotNull(caseService.getActiveLock(case.id))

        caseService.releaseLock(case.id, admin1, lock1.token)
        assertNull(caseService.getActiveLock(case.id))

        val lock2 = caseService.acquireLock(case.id, admin2, Duration.ofMinutes(10))
        assertEquals(admin2, lock2.holderId)
    }

    @Test
    fun `only current lock holder can release lock`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-1"),
                reason = "Investigation required.",
            )

        val lock = caseService.acquireLock(case.id, admin1, Duration.ofMinutes(10))

        assertThrows(InvalidLockTokenException::class.java) {
            caseService.releaseLock(case.id, admin2, lock.token)
        }

        assertThrows(InvalidLockTokenException::class.java) {
            caseService.releaseLock(case.id, admin1, LeaseToken("fake-token"))
        }

        assertNotNull(caseService.getActiveLock(case.id))
    }

    @Test
    fun `reacquiring a case after its lock expires`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-1"),
                reason = "Investigation required.",
            )

        val lock1 = caseService.acquireLock(case.id, admin1, Duration.ofMinutes(10))

        // Fast forward 11 minutes
        mutableClock.advance(Duration.ofMinutes(11))
        val advancedTime = baseTime.plus(Duration.ofMinutes(11))

        // Lock is now expired
        assertNull(caseService.getActiveLock(case.id))

        // Admin2 can acquire the lock because previous lease expired
        val lock2 = caseService.acquireLock(case.id, admin2, Duration.ofMinutes(10))
        assertEquals(admin2, lock2.holderId)
        assertEquals(advancedTime, lock2.acquiredAt)
        assertEquals(advancedTime.plus(Duration.ofMinutes(10)), lock2.expiresAt)
    }

    @Test
    fun `resolves an open case with valid decision and ends active lock`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-1"),
                reason = "Suspicious behavior.",
            )

        val lock = caseService.acquireLock(case.id, admin1, Duration.ofMinutes(15))

        mutableClock.advance(Duration.ofMinutes(5))
        val decisionTime = baseTime.plus(Duration.ofMinutes(5))

        val resolved =
            caseService.resolveCase(
                caseId = case.id,
                adminId = admin1,
                token = lock.token,
                outcome = CaseOutcome.SUSPEND_ACCOUNT,
                justification = "Confirmed malicious activity.",
            )

        assertEquals(CaseStatus.RESOLVED, resolved.status)
        assertTrue(resolved.isResolved)
        assertNotNull(resolved.decision)
        assertEquals(admin1, resolved.decision?.decidedBy)
        assertEquals(decisionTime, resolved.decision?.decidedAt)
        assertEquals(CaseOutcome.SUSPEND_ACCOUNT, resolved.decision?.outcome)
        assertEquals("Confirmed malicious activity.", resolved.decision?.justification)

        // Verifies original case details preserved
        assertEquals(creatorId, resolved.createdBy)
        assertEquals(baseTime, resolved.createdAt)
        assertEquals(case.subject, resolved.subject)
        assertEquals(case.reason, resolved.reason)

        // Active lock must be cleared upon resolution
        assertNull(caseService.getActiveLock(case.id))
        assertNull(lockRepository.findLock(case.id))
    }

    @Test
    fun `rejects decisions by the case creator regardless of administrative role`() {
        // Creator is also an admin
        val creatorAdmin = IdentityId("creator-and-admin")
        roleRepository.saveAssignment(RoleAssignment(creatorAdmin, Role.ADMIN))

        val case =
            caseService.createCase(
                creatorId = creatorAdmin,
                subject = CaseSubject("user-2"),
                reason = "Harassment report.",
            )

        val lock = caseService.acquireLock(case.id, creatorAdmin, Duration.ofMinutes(15))

        val ex =
            assertThrows(CaseCreatorSelfDecisionException::class.java) {
                caseService.resolveCase(
                    caseId = case.id,
                    adminId = creatorAdmin,
                    token = lock.token,
                    outcome = CaseOutcome.BAN_ACCOUNT,
                    justification = "Self deciding.",
                )
            }

        assertEquals(case.id, ex.caseId)
        assertEquals(creatorAdmin, ex.creatorId)

        // Case remains open
        val retrieved = caseService.getCase(case.id)
        assertEquals(CaseStatus.OPEN, retrieved?.status)
    }

    @Test
    fun `rejects lock acquisition and decisions without authorization`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-3"),
                reason = "Needs review.",
            )

        // Unassigned user cannot acquire lock
        assertThrows(UnauthorizedCaseOperationException::class.java) {
            caseService.acquireLock(case.id, unassignedUser)
        }

        // Moderator cannot resolve cases requiring DECIDE_SUSPENSIONS permission
        val lock = caseService.acquireLock(case.id, admin1)

        val mod2 = IdentityId("mod-2")
        roleRepository.saveAssignment(RoleAssignment(mod2, Role.MODERATOR))

        assertThrows(UnauthorizedCaseOperationException::class.java) {
            caseService.resolveCase(
                caseId = case.id,
                adminId = mod2,
                token = lock.token,
                outcome = CaseOutcome.SUSPEND_ACCOUNT,
                justification = "Mod resolving.",
                requiredPermission = Permission.DECIDE_SUSPENSIONS,
            )
        }
    }

    @Test
    fun `rejects decision if lock has expired`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-4"),
                reason = "Reviewing.",
            )

        val lock = caseService.acquireLock(case.id, admin1, Duration.ofMinutes(10))

        // Advance beyond expiration
        mutableClock.advance(Duration.ofMinutes(11))

        assertThrows(LockExpiredException::class.java) {
            caseService.resolveCase(
                caseId = case.id,
                adminId = admin1,
                token = lock.token,
                outcome = CaseOutcome.DISMISSED,
                justification = "Decision after expiry.",
            )
        }
    }

    @Test
    fun `rejects decision if lease token is superseded or invalid`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-5"),
                reason = "Reviewing.",
            )

        val lock1 = caseService.acquireLock(case.id, admin1, Duration.ofMinutes(10))

        // Admin1's lock expires
        mutableClock.advance(Duration.ofMinutes(11))

        // Admin2 acquires a new lock with a new token
        val lock2 = caseService.acquireLock(case.id, admin2, Duration.ofMinutes(10))

        // Admin1 attempts to decide with old superseded token
        assertThrows(InvalidLockTokenException::class.java) {
            caseService.resolveCase(
                caseId = case.id,
                adminId = admin1,
                token = lock1.token,
                outcome = CaseOutcome.DISMISSED,
                justification = "Late decision with old token.",
            )
        }

        // Admin2 can resolve with valid active token
        val resolved =
            caseService.resolveCase(
                caseId = case.id,
                adminId = admin2,
                token = lock2.token,
                outcome = CaseOutcome.DISMISSED,
                justification = "Valid decision by current holder.",
            )

        assertEquals(CaseStatus.RESOLVED, resolved.status)
    }

    @Test
    fun `prevents a resolved case from being handled or decided again`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-6"),
                reason = "Resolved test.",
            )

        val lock = caseService.acquireLock(case.id, admin1, Duration.ofMinutes(15))

        caseService.resolveCase(
            caseId = case.id,
            adminId = admin1,
            token = lock.token,
            outcome = CaseOutcome.NO_ACTION,
            justification = "Resolved.",
        )

        // Attempting to lock a resolved case must fail
        assertThrows(CaseAlreadyResolvedException::class.java) {
            caseService.acquireLock(case.id, admin2)
        }

        // Attempting to decide a resolved case must fail
        assertThrows(CaseAlreadyResolvedException::class.java) {
            caseService.resolveCase(
                caseId = case.id,
                adminId = admin2,
                token = lock.token,
                outcome = CaseOutcome.BAN_ACCOUNT,
                justification = "Second decision.",
            )
        }
    }

    @Test
    fun `lock renewal extends active lease when requested by valid holder`() {
        val case =
            caseService.createCase(
                creatorId = creatorId,
                subject = CaseSubject("user-7"),
                reason = "Long investigation.",
            )

        val initialLock = caseService.acquireLock(case.id, admin1, Duration.ofMinutes(10))

        mutableClock.advance(Duration.ofMinutes(8))
        val renewalTime = baseTime.plus(Duration.ofMinutes(8))

        val renewedLock =
            caseService.renewLock(
                caseId = case.id,
                adminId = admin1,
                token = initialLock.token,
                duration = Duration.ofMinutes(10),
            )

        assertEquals(initialLock.token, renewedLock.token)
        assertEquals(admin1, renewedLock.holderId)
        assertEquals(renewalTime, renewedLock.acquiredAt)
        assertEquals(renewalTime.plus(Duration.ofMinutes(10)), renewedLock.expiresAt)
    }
}

/**
 * Controllable [Clock] for deterministic time manipulation in unit tests.
 */
class MutableClock(
    private var currentInstant: Instant,
    private val zone: ZoneOffset = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneOffset = zone

    override fun withZone(zone: java.time.ZoneId?): Clock = MutableClock(currentInstant, zone as? ZoneOffset ?: ZoneOffset.UTC)

    override fun instant(): Instant = currentInstant

    fun advance(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }

    fun setInstant(newInstant: Instant) {
        currentInstant = newInstant
    }
}
