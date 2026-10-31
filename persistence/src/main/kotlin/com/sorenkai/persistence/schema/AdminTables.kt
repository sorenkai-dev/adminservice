package com.sorenkai.persistence.schema

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

/**
 * Exposed table definition for administrative role assignments.
 */
object RoleAssignmentsTable : Table("role_assignments") {
    val identityId = varchar("identity_id", 255)
    val role = varchar("role", 50)
    val assignedAt = timestampWithTimeZone("assigned_at")
    val assignedBy = varchar("assigned_by", 255).nullable()

    override val primaryKey = PrimaryKey(identityId)
}

/**
 * Exposed table definition for administrative cases.
 */
object AdministrativeCasesTable : Table("administrative_cases") {
    val id = varchar("id", 255)
    val createdBy = varchar("created_by", 255)
    val createdAt = timestampWithTimeZone("created_at")
    val subject = varchar("subject", 255)
    val reason = text("reason")
    val status = varchar("status", 50)
    val decidedBy = varchar("decided_by", 255).nullable()
    val decidedAt = timestampWithTimeZone("decided_at").nullable()
    val outcome = varchar("outcome", 255).nullable()
    val justification = text("justification").nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Exposed table definition for administrative case evidence references.
 */
object CaseEvidenceTable : Table("case_evidence") {
    val caseId = varchar("case_id", 255).references(AdministrativeCasesTable.id)
    val evidenceOrder = integer("evidence_order")
    val evidenceReference = text("evidence_reference")

    override val primaryKey = PrimaryKey(caseId, evidenceOrder)
}

/**
 * Exposed table definition for administrative case handling locks.
 */
object CaseLocksTable : Table("case_locks") {
    val caseId = varchar("case_id", 255).references(AdministrativeCasesTable.id)
    val holderId = varchar("holder_id", 255)
    val token = varchar("token", 255)
    val acquiredAt = timestampWithTimeZone("acquired_at")
    val expiresAt = timestampWithTimeZone("expires_at")

    override val primaryKey = PrimaryKey(caseId)
}
