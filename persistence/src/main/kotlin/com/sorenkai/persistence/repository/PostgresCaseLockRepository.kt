package com.sorenkai.persistence.repository

import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.CaseLock
import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.LeaseToken
import com.sorenkai.core.repository.CaseLockRepository
import com.sorenkai.core.repository.LockAcquisitionResult
import com.sorenkai.persistence.schema.CaseLocksTable
import com.sorenkai.persistence.transaction.inExposedTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.ZoneOffset
import javax.sql.DataSource

/**
 * JetBrains Exposed DSL implementation of [CaseLockRepository] with atomic lease acquisition,
 * renewal, and release guarantees.
 */
class PostgresCaseLockRepository(
    private val database: Database? = null,
) : CaseLockRepository {
    constructor(dataSource: DataSource) : this(Database.connect(dataSource))

    override fun tryAcquire(
        caseId: CaseId,
        holderId: IdentityId,
        token: LeaseToken,
        acquiredAt: Instant,
        expiresAt: Instant,
        now: Instant,
    ): LockAcquisitionResult =
        inExposedTransaction(database) {
            val newLock =
                CaseLock(
                    caseId = caseId,
                    holderId = holderId,
                    token = token,
                    acquiredAt = acquiredAt,
                    expiresAt = expiresAt,
                )

            val existing = selectLockForUpdate(caseId)
            when {
                existing == null -> {
                    val insertedCount =
                        CaseLocksTable
                            .insertIgnore {
                                it[this.caseId] = newLock.caseId.value
                                it[this.holderId] = newLock.holderId.value
                                it[this.token] = newLock.token.value
                                it[this.acquiredAt] = newLock.acquiredAt.atOffset(ZoneOffset.UTC)
                                it[this.expiresAt] = newLock.expiresAt.atOffset(ZoneOffset.UTC)
                            }.insertedCount

                    if (insertedCount > 0) {
                        LockAcquisitionResult.Acquired(newLock)
                    } else {
                        handleConcurrentInsert(caseId, newLock, now)
                    }
                }

                existing.isExpired(now) -> {
                    updateLock(newLock)
                    LockAcquisitionResult.Acquired(newLock)
                }

                else -> {
                    LockAcquisitionResult.AlreadyLocked(existing)
                }
            }
        }

    override fun tryRenew(
        caseId: CaseId,
        holderId: IdentityId,
        token: LeaseToken,
        renewedAt: Instant,
        newExpiresAt: Instant,
        now: Instant,
    ): CaseLock? =
        inExposedTransaction(database) {
            val rows =
                CaseLocksTable.update(
                    where = {
                        (CaseLocksTable.caseId eq caseId.value) and
                            (CaseLocksTable.holderId eq holderId.value) and
                            (CaseLocksTable.token eq token.value) and
                            (CaseLocksTable.expiresAt greater now.atOffset(ZoneOffset.UTC))
                    },
                ) {
                    it[this.acquiredAt] = renewedAt.atOffset(ZoneOffset.UTC)
                    it[this.expiresAt] = newExpiresAt.atOffset(ZoneOffset.UTC)
                }

            if (rows > 0) {
                CaseLock(
                    caseId = caseId,
                    holderId = holderId,
                    token = token,
                    acquiredAt = renewedAt,
                    expiresAt = newExpiresAt,
                )
            } else {
                null
            }
        }

    override fun findLock(caseId: CaseId): CaseLock? =
        inExposedTransaction(database) {
            CaseLocksTable
                .selectAll()
                .where { CaseLocksTable.caseId eq caseId.value }
                .singleOrNull()
                ?.let(::mapRowToLock)
        }

    override fun releaseLock(
        caseId: CaseId,
        holderId: IdentityId,
        token: LeaseToken,
    ): Boolean =
        inExposedTransaction(database) {
            val rows =
                CaseLocksTable.deleteWhere {
                    (CaseLocksTable.caseId eq caseId.value) and
                        (CaseLocksTable.holderId eq holderId.value) and
                        (CaseLocksTable.token eq token.value)
                }
            rows > 0
        }

    override fun forceRelease(caseId: CaseId) {
        inExposedTransaction(database) {
            CaseLocksTable.deleteWhere {
                CaseLocksTable.caseId eq caseId.value
            }
        }
    }

    private fun selectLockForUpdate(caseId: CaseId): CaseLock? =
        CaseLocksTable
            .selectAll()
            .where { CaseLocksTable.caseId eq caseId.value }
            .forUpdate()
            .singleOrNull()
            ?.let(::mapRowToLock)

    private fun handleConcurrentInsert(
        caseId: CaseId,
        newLock: CaseLock,
        now: Instant,
    ): LockAcquisitionResult {
        val existing =
            selectLockForUpdate(caseId)
                ?: error("Concurrent lock acquisition failed unexpectedly for case '${caseId.value}'.")
        return if (existing.isExpired(now)) {
            updateLock(newLock)
            LockAcquisitionResult.Acquired(newLock)
        } else {
            LockAcquisitionResult.AlreadyLocked(existing)
        }
    }

    private fun updateLock(lock: CaseLock) {
        CaseLocksTable.update(where = { CaseLocksTable.caseId eq lock.caseId.value }) {
            it[this.holderId] = lock.holderId.value
            it[this.token] = lock.token.value
            it[this.acquiredAt] = lock.acquiredAt.atOffset(ZoneOffset.UTC)
            it[this.expiresAt] = lock.expiresAt.atOffset(ZoneOffset.UTC)
        }
    }

    private fun mapRowToLock(row: ResultRow): CaseLock =
        CaseLock(
            caseId = CaseId(row[CaseLocksTable.caseId]),
            holderId = IdentityId(row[CaseLocksTable.holderId]),
            token = LeaseToken(row[CaseLocksTable.token]),
            acquiredAt = row[CaseLocksTable.acquiredAt].toInstant(),
            expiresAt = row[CaseLocksTable.expiresAt].toInstant(),
        )
}
