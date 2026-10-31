package com.sorenkai.core.repository

import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.CaseLock
import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.LeaseToken
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory implementation of [CaseLockRepository] with atomic compare-and-set semantics.
 */
class InMemoryCaseLockRepository : CaseLockRepository {
    private val locks = ConcurrentHashMap<CaseId, CaseLock>()

    @Synchronized
    override fun tryAcquire(
        caseId: CaseId,
        holderId: IdentityId,
        token: LeaseToken,
        acquiredAt: Instant,
        expiresAt: Instant,
        now: Instant,
    ): LockAcquisitionResult {
        val existing = locks[caseId]
        if (existing != null && existing.isActive(now)) {
            return LockAcquisitionResult.AlreadyLocked(existing)
        }

        val newLock =
            CaseLock(
                caseId = caseId,
                holderId = holderId,
                token = token,
                acquiredAt = acquiredAt,
                expiresAt = expiresAt,
            )
        locks[caseId] = newLock
        return LockAcquisitionResult.Acquired(newLock)
    }

    @Synchronized
    override fun tryRenew(
        caseId: CaseId,
        holderId: IdentityId,
        token: LeaseToken,
        renewedAt: Instant,
        newExpiresAt: Instant,
        now: Instant,
    ): CaseLock? {
        val existing = locks[caseId] ?: return null
        if (!existing.isValid(holderId, token, now)) {
            return null
        }

        val renewedLock =
            existing.copy(
                acquiredAt = renewedAt,
                expiresAt = newExpiresAt,
            )
        locks[caseId] = renewedLock
        return renewedLock
    }

    override fun findLock(caseId: CaseId): CaseLock? = locks[caseId]

    @Synchronized
    override fun releaseLock(
        caseId: CaseId,
        holderId: IdentityId,
        token: LeaseToken,
    ): Boolean {
        val existing = locks[caseId] ?: return false
        if (existing.holderId == holderId && existing.token == token) {
            locks.remove(caseId)
            return true
        }
        return false
    }

    @Synchronized
    override fun forceRelease(caseId: CaseId) {
        locks.remove(caseId)
    }

    /**
     * Clears all stored locks.
     */
    fun clear() {
        locks.clear()
    }
}
