package com.sorenkai.core.model

import java.time.Instant

/**
 * Represents an active or expired exclusive handling lock held by an administrator on an administrative case.
 *
 * @property caseId The case protected by this exclusive lock.
 * @property holderId The identity of the administrator currently holding the lease.
 * @property token The lease token issued for this active handling session.
 * @property acquiredAt The timestamp when the lock lease was acquired or renewed.
 * @property expiresAt The timestamp when this lease expires if not renewed or released.
 */
data class CaseLock(
    val caseId: CaseId,
    val holderId: IdentityId,
    val token: LeaseToken,
    val acquiredAt: Instant,
    val expiresAt: Instant,
) {
    init {
        require(!expiresAt.isBefore(acquiredAt)) { "Lock expiration time cannot be before acquisition time." }
    }

    /**
     * Checks if this lock has expired relative to [now].
     */
    fun isExpired(now: Instant): Boolean = !now.isBefore(expiresAt)

    /**
     * Checks if this lock is actively unexpired relative to [now].
     */
    fun isActive(now: Instant): Boolean = !isExpired(now)

    /**
     * Verifies whether the lock is currently active and held by [identityId] with matching [leaseToken].
     */
    fun isValid(
        identityId: IdentityId,
        leaseToken: LeaseToken,
        now: Instant,
    ): Boolean = isActive(now) && holderId == identityId && token == leaseToken
}
