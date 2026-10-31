package com.sorenkai.persistence.repository

import com.sorenkai.core.model.AdministrativeCase
import com.sorenkai.core.model.CaseDecision
import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.CaseOutcome
import com.sorenkai.core.model.CaseStatus
import com.sorenkai.core.model.CaseSubject
import com.sorenkai.core.model.EvidenceReference
import com.sorenkai.core.model.IdentityId
import com.sorenkai.persistence.PostgresIntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * PostgreSQL integration tests for [PostgresCaseRepository].
 */
class PostgresCaseRepositoryTest : PostgresIntegrationTestBase() {
    private lateinit var repository: PostgresCaseRepository

    private val creatorId = IdentityId("moderator-1")
    private val subject = CaseSubject("user-harassment-report")
    private val reason = "Inappropriate behavior in community chat."
    private val evidence =
        listOf(
            EvidenceReference("https://evidence.example.com/item-1"),
            EvidenceReference("https://evidence.example.com/item-2"),
        )
    private val baseTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    @BeforeEach
    fun initRepository() {
        repository = PostgresCaseRepository(dataSource)
    }

    @Test
    fun `persists open case and retrieves all creation fields and evidence accurately`() {
        val caseId = CaseId("case-001")
        val openCase =
            AdministrativeCase(
                id = caseId,
                createdBy = creatorId,
                createdAt = baseTime,
                subject = subject,
                reason = reason,
                evidence = evidence,
            )

        val saved = repository.save(openCase)
        assertEquals(openCase, saved)

        val retrieved = repository.findById(caseId)
        assertNotNull(retrieved)
        assertEquals(caseId, retrieved?.id)
        assertEquals(creatorId, retrieved?.createdBy)
        assertEquals(baseTime, retrieved?.createdAt)
        assertEquals(subject, retrieved?.subject)
        assertEquals(reason, retrieved?.reason)
        assertEquals(evidence, retrieved?.evidence)
        assertEquals(CaseStatus.OPEN, retrieved?.status)
        assertTrue(retrieved?.isOpen == true)
        assertFalse(retrieved?.isResolved == true)
        assertNull(retrieved?.decision)
    }

    @Test
    fun `resolves open case recording decision and preserving original creation metadata`() {
        val caseId = CaseId("case-002")
        val openCase =
            AdministrativeCase(
                id = caseId,
                createdBy = creatorId,
                createdAt = baseTime,
                subject = subject,
                reason = reason,
                evidence = evidence,
            )
        repository.save(openCase)

        val deciderId = IdentityId("admin-1")
        val decidedAt = baseTime.plusSeconds(300)
        val decision =
            CaseDecision(
                decidedBy = deciderId,
                decidedAt = decidedAt,
                outcome = CaseOutcome("SUSPEND_ACCOUNT"),
                justification = "Confirmed violation upon review.",
            )

        val resolvedCase = openCase.resolve(decision)
        repository.save(resolvedCase)

        val retrieved = repository.findById(caseId)
        assertNotNull(retrieved)
        assertEquals(caseId, retrieved?.id)
        assertEquals(creatorId, retrieved?.createdBy)
        assertEquals(baseTime, retrieved?.createdAt)
        assertEquals(subject, retrieved?.subject)
        assertEquals(reason, retrieved?.reason)
        assertEquals(evidence, retrieved?.evidence)
        assertEquals(CaseStatus.RESOLVED, retrieved?.status)
        assertTrue(retrieved?.isResolved == true)
        assertEquals(decision, retrieved?.decision)
        assertEquals(deciderId, retrieved?.decision?.decidedBy)
        assertEquals(decidedAt, retrieved?.decision?.decidedAt)
        assertEquals(CaseOutcome("SUSPEND_ACCOUNT"), retrieved?.decision?.outcome)
        assertEquals("Confirmed violation upon review.", retrieved?.decision?.justification)
    }

    @Test
    fun `prevents resolved case from being decided again`() {
        val caseId = CaseId("case-003")
        val openCase =
            AdministrativeCase(
                id = caseId,
                createdBy = creatorId,
                createdAt = baseTime,
                subject = subject,
                reason = reason,
            )
        repository.save(openCase)

        val decision1 =
            CaseDecision(
                decidedBy = IdentityId("admin-1"),
                decidedAt = baseTime.plusSeconds(60),
                outcome = CaseOutcome.DISMISSED,
                justification = "Dismissed as unsubstantiated.",
            )
        val resolvedCase = openCase.resolve(decision1)
        repository.save(resolvedCase)

        val decision2 =
            CaseDecision(
                decidedBy = IdentityId("admin-2"),
                decidedAt = baseTime.plusSeconds(120),
                outcome = CaseOutcome.BAN_ACCOUNT,
                justification = "Attempted re-resolution.",
            )

        assertThrows(IllegalStateException::class.java) {
            resolvedCase.resolve(decision2)
        }
    }

    @Test
    fun `returns null for non-existent case`() {
        assertNull(repository.findById(CaseId("non-existent-case")))
    }

    @Test
    fun `lists all cases in ascending chronological order`() {
        val case1 =
            AdministrativeCase(
                id = CaseId("case-a"),
                createdBy = creatorId,
                createdAt = baseTime,
                subject = CaseSubject("subject-1"),
                reason = "Reason 1",
            )
        val case2 =
            AdministrativeCase(
                id = CaseId("case-b"),
                createdBy = creatorId,
                createdAt = baseTime.plusSeconds(100),
                subject = CaseSubject("subject-2"),
                reason = "Reason 2",
            )
        repository.save(case2)
        repository.save(case1)

        val all = repository.findAll()
        assertEquals(2, all.size)
        assertEquals(CaseId("case-a"), all[0].id)
        assertEquals(CaseId("case-b"), all[1].id)
    }

    @Test
    fun `concurrent resolution attempts on separate connections allow only one successful decision`() {
        val caseId = CaseId("case-concurrent-001")
        val openCase =
            AdministrativeCase(
                id = caseId,
                createdBy = creatorId,
                createdAt = baseTime,
                subject = subject,
                reason = reason,
            )
        repository.save(openCase)

        val threadCount = 4
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        for (i in 1..threadCount) {
            executor.submit {
                try {
                    startLatch.await()
                    val repo = PostgresCaseRepository(dataSource)
                    val current = repo.findById(caseId) ?: return@submit
                    val decision =
                        CaseDecision(
                            decidedBy = IdentityId("admin-$i"),
                            decidedAt = baseTime.plusSeconds(i.toLong()),
                            outcome = CaseOutcome("OUTCOME_$i"),
                            justification = "Resolution by thread $i",
                        )
                    val resolved = current.resolve(decision)
                    repo.save(resolved)
                    successCount.incrementAndGet()
                } catch (expected: Exception) {
                    failureCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS))
        executor.shutdown()

        assertEquals(1, successCount.get())
        assertEquals(threadCount - 1, failureCount.get())

        val finalCase = repository.findById(caseId)
        assertNotNull(finalCase)
        assertEquals(CaseStatus.RESOLVED, finalCase?.status)
    }

    @Test
    fun `rolls back transaction on error without corrupting or leaving partial data`() {
        val caseId = CaseId("case-rollback-test")
        val openCase =
            AdministrativeCase(
                id = caseId,
                createdBy = creatorId,
                createdAt = baseTime,
                subject = subject,
                reason = reason,
                evidence = evidence,
            )
        repository.save(openCase)

        // Simulate a transaction failure: attempt an illegal state update
        assertThrows(IllegalStateException::class.java) {
            // Attempting to save resolved case when existing status in DB is altered or already resolved
            val dec = CaseDecision(IdentityId("admin-1"), baseTime, CaseOutcome.DISMISSED, "test")
            val resolved = openCase.resolve(dec)
            repository.save(resolved)
            // Attempting second resolution should fail and rollback
            val dec2 = CaseDecision(IdentityId("admin-2"), baseTime, CaseOutcome.BAN_ACCOUNT, "test2")
            repository.save(openCase.resolve(dec2))
        }

        // Verify the original resolved decision is preserved intact
        val persisted = repository.findById(caseId)
        assertNotNull(persisted)
        assertEquals(IdentityId("admin-1"), persisted?.decision?.decidedBy)
        assertEquals(CaseOutcome.DISMISSED, persisted?.decision?.outcome)
    }
}
