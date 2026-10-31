package com.sorenkai.persistence.repository

import com.sorenkai.core.model.AdministrativeCase
import com.sorenkai.core.model.CaseDecision
import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.CaseOutcome
import com.sorenkai.core.model.CaseStatus
import com.sorenkai.core.model.CaseSubject
import com.sorenkai.core.model.EvidenceReference
import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.repository.CaseRepository
import com.sorenkai.persistence.schema.AdministrativeCasesTable
import com.sorenkai.persistence.schema.CaseEvidenceTable
import com.sorenkai.persistence.transaction.inExposedTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.ZoneOffset
import javax.sql.DataSource

/**
 * JetBrains Exposed DSL implementation of [CaseRepository] ensuring ACID transaction boundaries,
 * immutable preservation of creation metadata, and prevention of duplicate decisions.
 */
class PostgresCaseRepository(
    private val database: Database? = null,
) : CaseRepository {
    constructor(dataSource: DataSource) : this(Database.connect(dataSource))

    override fun save(case: AdministrativeCase): AdministrativeCase =
        inExposedTransaction(database) {
            val existingRow =
                AdministrativeCasesTable
                    .selectAll()
                    .where { AdministrativeCasesTable.id eq case.id.value }
                    .forUpdate()
                    .singleOrNull()

            if (existingRow == null) {
                insertNewCase(case)
            } else {
                val existingStatus = CaseStatus.valueOf(existingRow[AdministrativeCasesTable.status])
                updateExistingCase(case, existingStatus)
            }
            case
        }

    override fun findById(id: CaseId): AdministrativeCase? =
        inExposedTransaction(database) {
            val caseRow =
                AdministrativeCasesTable
                    .selectAll()
                    .where { AdministrativeCasesTable.id eq id.value }
                    .singleOrNull() ?: return@inExposedTransaction null

            val evidence = loadEvidence(id)
            mapRowToCase(caseRow, evidence)
        }

    override fun findAll(): List<AdministrativeCase> =
        inExposedTransaction(database) {
            val caseRows =
                AdministrativeCasesTable
                    .selectAll()
                    .orderBy(
                        AdministrativeCasesTable.createdAt to SortOrder.ASC,
                        AdministrativeCasesTable.id to SortOrder.ASC,
                    ).toList()

            caseRows.map { row ->
                val caseId = CaseId(row[AdministrativeCasesTable.id])
                val evidence = loadEvidence(caseId)
                mapRowToCase(row, evidence)
            }
        }

    private fun insertNewCase(case: AdministrativeCase) {
        AdministrativeCasesTable.insert {
            it[id] = case.id.value
            it[createdBy] = case.createdBy.value
            it[createdAt] = case.createdAt.atOffset(ZoneOffset.UTC)
            it[subject] = case.subject.value
            it[reason] = case.reason
            it[status] = case.status.name

            val decision = case.decision
            if (decision != null) {
                it[decidedBy] = decision.decidedBy.value
                it[decidedAt] = decision.decidedAt.atOffset(ZoneOffset.UTC)
                it[outcome] = decision.outcome.value
                it[justification] = decision.justification
            }
        }

        insertEvidence(case.id, case.evidence)
    }

    private fun updateExistingCase(
        case: AdministrativeCase,
        existingStatus: CaseStatus,
    ) {
        check(existingStatus == CaseStatus.OPEN) {
            "Cannot modify or resolve case '${case.id.value}' because it is already resolved."
        }

        if (case.status == CaseStatus.RESOLVED) {
            val decision =
                requireNotNull(case.decision) {
                    "A resolved case must contain a valid decision."
                }

            val rowsUpdated =
                AdministrativeCasesTable.update(
                    where = {
                        (AdministrativeCasesTable.id eq case.id.value) and
                            (AdministrativeCasesTable.status eq CaseStatus.OPEN.name)
                    },
                ) {
                    it[status] = CaseStatus.RESOLVED.name
                    it[decidedBy] = decision.decidedBy.value
                    it[decidedAt] = decision.decidedAt.atOffset(ZoneOffset.UTC)
                    it[outcome] = decision.outcome.value
                    it[justification] = decision.justification
                }

            check(rowsUpdated > 0) {
                "Failed to resolve case '${case.id.value}': case was already resolved concurrently."
            }
        }
    }

    private fun insertEvidence(
        caseId: CaseId,
        evidence: List<EvidenceReference>,
    ) {
        if (evidence.isEmpty()) return
        CaseEvidenceTable.batchInsert(evidence.indices) { index ->
            this[CaseEvidenceTable.caseId] = caseId.value
            this[CaseEvidenceTable.evidenceOrder] = index
            this[CaseEvidenceTable.evidenceReference] = evidence[index].value
        }
    }

    private fun loadEvidence(caseId: CaseId): List<EvidenceReference> =
        CaseEvidenceTable
            .selectAll()
            .where { CaseEvidenceTable.caseId eq caseId.value }
            .orderBy(CaseEvidenceTable.evidenceOrder to SortOrder.ASC)
            .map { EvidenceReference(it[CaseEvidenceTable.evidenceReference]) }

    private fun mapRowToCase(
        row: ResultRow,
        evidence: List<EvidenceReference>,
    ): AdministrativeCase {
        val id = CaseId(row[AdministrativeCasesTable.id])
        val createdBy = IdentityId(row[AdministrativeCasesTable.createdBy])
        val createdAt = row[AdministrativeCasesTable.createdAt].toInstant()
        val subject = CaseSubject(row[AdministrativeCasesTable.subject])
        val reason = row[AdministrativeCasesTable.reason]
        val status = CaseStatus.valueOf(row[AdministrativeCasesTable.status])
        val decision = mapRowToDecision(row)

        return AdministrativeCase(
            id = id,
            createdBy = createdBy,
            createdAt = createdAt,
            subject = subject,
            reason = reason,
            status = status,
            decision = decision,
            evidence = evidence,
        )
    }

    private fun mapRowToDecision(row: ResultRow): CaseDecision? {
        val decidedByStr = row[AdministrativeCasesTable.decidedBy] ?: return null
        val decidedAt = row[AdministrativeCasesTable.decidedAt]?.toInstant() ?: return null
        val outcomeStr = row[AdministrativeCasesTable.outcome] ?: return null
        val justification = row[AdministrativeCasesTable.justification] ?: return null

        return CaseDecision(
            decidedBy = IdentityId(decidedByStr),
            decidedAt = decidedAt,
            outcome = CaseOutcome(outcomeStr),
            justification = justification,
        )
    }
}
