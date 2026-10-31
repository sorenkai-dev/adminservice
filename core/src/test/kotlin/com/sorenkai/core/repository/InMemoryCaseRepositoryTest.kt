package com.sorenkai.core.repository

import com.sorenkai.core.model.AdministrativeCase
import com.sorenkai.core.model.CaseDecision
import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.CaseOutcome
import com.sorenkai.core.model.CaseSubject
import com.sorenkai.core.model.EvidenceReference
import com.sorenkai.core.model.IdentityId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests verifying [InMemoryCaseRepository] storage, update, and retrieval operations.
 */
class InMemoryCaseRepositoryTest {
    private lateinit var repository: InMemoryCaseRepository

    @BeforeEach
    fun setUp() {
        repository = InMemoryCaseRepository()
    }

    @Test
    fun `saves and retrieves cases by identifier`() {
        val caseId = CaseId("case-1")
        val case =
            AdministrativeCase(
                id = caseId,
                createdBy = IdentityId("mod-1"),
                createdAt = Instant.parse("2026-10-30T10:00:00Z"),
                subject = CaseSubject("user-1"),
                reason = "Harassment",
                evidence = listOf(EvidenceReference("https://evidence.example.com/item")),
            )

        assertNull(repository.findById(caseId))
        repository.save(case)

        val retrieved = repository.findById(caseId)
        assertNotNull(retrieved)
        assertEquals(case, retrieved)
        assertEquals(listOf(case), repository.findAll())
    }

    @Test
    fun `updates existing case preserving identity`() {
        val caseId = CaseId("case-2")
        val openCase =
            AdministrativeCase(
                id = caseId,
                createdBy = IdentityId("mod-1"),
                createdAt = Instant.parse("2026-10-30T10:00:00Z"),
                subject = CaseSubject("user-2"),
                reason = "Spam",
            )
        repository.save(openCase)

        val resolvedCase =
            openCase.resolve(
                CaseDecision(
                    decidedBy = IdentityId("admin-1"),
                    decidedAt = Instant.parse("2026-10-30T10:30:00Z"),
                    outcome = CaseOutcome.DISMISSED,
                    justification = "False positive",
                ),
            )
        repository.save(resolvedCase)

        val retrieved = repository.findById(caseId)
        assertNotNull(retrieved)
        assertEquals(resolvedCase, retrieved)
        assertEquals(1, repository.findAll().size)
    }
}
