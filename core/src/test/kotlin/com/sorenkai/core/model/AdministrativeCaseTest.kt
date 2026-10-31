package com.sorenkai.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests verifying domain behavior and immutability of [AdministrativeCase] and [CaseDecision].
 */
class AdministrativeCaseTest {
    private val caseId = CaseId("case-101")
    private val creatorId = IdentityId("moderator-1")
    private val subject = CaseSubject("user-bad-behavior")
    private val reason = "Suspected harassment in public channels."
    private val evidence = listOf(EvidenceReference("https://evidence.example.com/item-1"))
    private val createdAt = Instant.parse("2026-10-30T10:00:00Z")

    @Test
    fun `creates a valid open administrative case preserving all creation metadata`() {
        val case =
            AdministrativeCase(
                id = caseId,
                createdBy = creatorId,
                createdAt = createdAt,
                subject = subject,
                reason = reason,
                evidence = evidence,
            )

        assertEquals(caseId, case.id)
        assertEquals(creatorId, case.createdBy)
        assertEquals(createdAt, case.createdAt)
        assertEquals(subject, case.subject)
        assertEquals(reason, case.reason)
        assertEquals(evidence, case.evidence)
        assertEquals(CaseStatus.OPEN, case.status)
        assertTrue(case.isOpen)
        assertFalse(case.isResolved)
        assertNull(case.decision)
    }

    @Test
    fun `resolving an open case produces a resolved case preserving original creation information`() {
        val openCase =
            AdministrativeCase(
                id = caseId,
                createdBy = creatorId,
                createdAt = createdAt,
                subject = subject,
                reason = reason,
                evidence = evidence,
            )

        val deciderId = IdentityId("admin-1")
        val decidedAt = Instant.parse("2026-10-30T11:00:00Z")
        val decision =
            CaseDecision(
                decidedBy = deciderId,
                decidedAt = decidedAt,
                outcome = CaseOutcome("SUSPEND_ACCOUNT"),
                justification = "Confirmed violation of community guidelines.",
            )

        val resolvedCase = openCase.resolve(decision)

        // Preserves original creation fields
        assertEquals(caseId, resolvedCase.id)
        assertEquals(creatorId, resolvedCase.createdBy)
        assertEquals(createdAt, resolvedCase.createdAt)
        assertEquals(subject, resolvedCase.subject)
        assertEquals(reason, resolvedCase.reason)
        assertEquals(evidence, resolvedCase.evidence)

        // Verifies resolved state
        assertEquals(CaseStatus.RESOLVED, resolvedCase.status)
        assertTrue(resolvedCase.isResolved)
        assertFalse(resolvedCase.isOpen)
        assertEquals(decision, resolvedCase.decision)
        assertEquals(deciderId, resolvedCase.decision?.decidedBy)
        assertEquals(decidedAt, resolvedCase.decision?.decidedAt)
        assertEquals(CaseOutcome("SUSPEND_ACCOUNT"), resolvedCase.decision?.outcome)
        assertEquals("Confirmed violation of community guidelines.", resolvedCase.decision?.justification)
    }

    @Test
    fun `cannot resolve an already resolved case`() {
        val decision =
            CaseDecision(
                decidedBy = IdentityId("admin-1"),
                decidedAt = Instant.parse("2026-10-30T11:00:00Z"),
                outcome = CaseOutcome("DISMISS"),
                justification = "No violation found.",
            )

        val resolvedCase =
            AdministrativeCase(
                id = caseId,
                createdBy = creatorId,
                createdAt = createdAt,
                subject = subject,
                reason = reason,
                status = CaseStatus.RESOLVED,
                decision = decision,
            )

        val secondDecision =
            CaseDecision(
                decidedBy = IdentityId("admin-2"),
                decidedAt = Instant.parse("2026-10-30T12:00:00Z"),
                outcome = CaseOutcome("BAN"),
                justification = "Re-evaluation.",
            )

        assertThrows(IllegalStateException::class.java) {
            resolvedCase.resolve(secondDecision)
        }
    }

    @Test
    fun `validates non-blank identifier value objects`() {
        assertThrows(IllegalArgumentException::class.java) { CaseId("") }
        assertThrows(IllegalArgumentException::class.java) { CaseId("   ") }
        assertThrows(IllegalArgumentException::class.java) { CaseSubject("") }
        assertThrows(IllegalArgumentException::class.java) { CaseOutcome("") }
        assertThrows(IllegalArgumentException::class.java) { LeaseToken("") }
        assertThrows(IllegalArgumentException::class.java) { EvidenceReference("") }
    }
}
