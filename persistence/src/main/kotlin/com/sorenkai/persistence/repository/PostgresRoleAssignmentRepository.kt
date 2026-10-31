package com.sorenkai.persistence.repository

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Role
import com.sorenkai.core.model.RoleAssignment
import com.sorenkai.core.repository.RoleAssignmentRepository
import com.sorenkai.persistence.schema.RoleAssignmentsTable
import com.sorenkai.persistence.transaction.inExposedTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import java.time.ZoneOffset
import javax.sql.DataSource

/**
 * JetBrains Exposed DSL implementation of [RoleAssignmentRepository].
 */
class PostgresRoleAssignmentRepository(
    private val database: Database? = null,
) : RoleAssignmentRepository {
    constructor(dataSource: DataSource) : this(Database.connect(dataSource))

    override fun findRoleByIdentity(identityId: IdentityId): Role? =
        inExposedTransaction(database) {
            RoleAssignmentsTable
                .selectAll()
                .where { RoleAssignmentsTable.identityId eq identityId.value }
                .singleOrNull()
                ?.let { Role.valueOf(it[RoleAssignmentsTable.role]) }
        }

    override fun findAssignmentByIdentity(identityId: IdentityId): RoleAssignment? =
        inExposedTransaction(database) {
            RoleAssignmentsTable
                .selectAll()
                .where { RoleAssignmentsTable.identityId eq identityId.value }
                .singleOrNull()
                ?.let(::mapRowToAssignment)
        }

    override fun saveAssignment(assignment: RoleAssignment): RoleAssignment =
        inExposedTransaction(database) {
            RoleAssignmentsTable.upsert {
                it[identityId] = assignment.identityId.value
                it[role] = assignment.role.name
                it[assignedAt] = assignment.assignedAt.atOffset(ZoneOffset.UTC)
                it[assignedBy] = assignment.assignedBy?.value
            }
            assignment
        }

    override fun removeAssignment(identityId: IdentityId): Boolean =
        inExposedTransaction(database) {
            RoleAssignmentsTable.deleteWhere {
                RoleAssignmentsTable.identityId eq identityId.value
            } > 0
        }

    override fun findAllAssignments(): List<RoleAssignment> =
        inExposedTransaction(database) {
            RoleAssignmentsTable
                .selectAll()
                .orderBy(RoleAssignmentsTable.identityId to SortOrder.ASC)
                .map(::mapRowToAssignment)
        }

    private fun mapRowToAssignment(row: ResultRow): RoleAssignment {
        val identity = IdentityId(row[RoleAssignmentsTable.identityId])
        val role = Role.valueOf(row[RoleAssignmentsTable.role])
        val assignedAt = row[RoleAssignmentsTable.assignedAt].toInstant()
        val assignedByStr = row[RoleAssignmentsTable.assignedBy]
        val assignedBy = assignedByStr?.let { IdentityId(it) }
        return RoleAssignment(identity, role, assignedAt, assignedBy)
    }
}
