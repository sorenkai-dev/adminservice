package com.sorenkai.core.repository

import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.CaseLock
import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.LeaseToken
import java.time.Instant

/**
 * Result of an atomic lock acquisition attempt.
 */
sealed interface LockAcquisitionResult {
    /**
     * Lock was successfully acquired.
     *
     * @property lock The acquired [CaseLock].
     */
    data class Acquired(
        val lock: CaseLock,
    ) : LockAcquisitionResult

    /**
     * Lock acquisition failed because another administrator holds an active lease.
     *
     * @property activeLock The existing unexpired [CaseLock].
     */
    data class AlreadyLocked(
        val activeLock: CaseLock,
    ) : LockAcquisitionResult
}

/**
 * Coordination contract for managing exclusive case handling locks with atomic guarantees.
 *
 * Persistence implementation guidelines:
 * - Implementations (such as PostgreSQL conditional updates with `SELECT ... FOR UPDATE` or
 *   optimistic concurrency tokens) MUST ensure atomic check-and-set semantics for acquisition,
 *   renewal, and release.
 * - Lock acquisition must succeed if no lock is present OR if the existing lock is expired relative
 *   to the evaluation timestamp.
 * - When an active lock exists for another holder, acquisition must fail without modifying the state.
 */
interface CaseLockRepository {
    /**
     * Atomically attempts to acquire an exclusive lock on [caseId].
     *
     * If no lock exists or the existing lock is expired relative to [now], a new lock is created and returned.
     * If an active lock is already held, returns [LockAcquisitionResult.AlreadyLocked].
     *
     * @param caseId The case to lock.
     * @param holderId The administrator identity requesting the lock.
     * @param token The proposed lease token.
     * @param acquiredAt Timestamp of acquisition.
     * @param expiresAt Timestamp when this lease expires.
     * @param now Current evaluation timestamp.
     * @return [LockAcquisitionResult.Acquired] if successful; [LockAcquisitionResult.AlreadyLocked] otherwise.
     */
    fun tryAcquire(
        caseId: CaseId,
        holderId: IdentityId,
        token: LeaseToken,
        acquiredAt: Instant,
        expiresAt: Instant,
        now: Instant,
    ): LockAcquisitionResult

    /**
     * Atomically renews an active lock held by [holderId] with [token].
     *
     * @param caseId The case whose lock is to be renewed.
     * @param holderId The current holder.
     * @param token The active lease token.
     * @param renewedAt Timestamp of renewal.
     * @param newExpiresAt New expiration timestamp.
     * @param now Current evaluation timestamp.
     * @return The updated [CaseLock] if renewed; `null` if the lock is missing, held by another user, expired, or has a mismatched token.
     */
    fun tryRenew(
        caseId: CaseId,
        holderId: IdentityId,
        token: LeaseToken,
        renewedAt: Instant,
        newExpiresAt: Instant,
        now: Instant,
    ): CaseLock?

    /**
     * Retrieves the current lock for [caseId], if present.
     */
    fun findLock(caseId: CaseId): CaseLock?

    /**
     * Atomically releases the lock for [caseId] if held by [holderId] with [token].
     *
     * @return `true` if the lock was found, matched, and removed; `false` otherwise.
     */
    fun releaseLock(
        caseId: CaseId,
        holderId: IdentityId,
        token: LeaseToken,
    ): Boolean

    /**
     * Unconditionally removes any lock associated with [caseId].
     */
    fun forceRelease(caseId: CaseId)
}
