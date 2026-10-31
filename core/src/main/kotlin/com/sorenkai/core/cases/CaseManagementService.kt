package com.sorenkai.core.cases

import com.sorenkai.core.authorization.AuthorizationService
import com.sorenkai.core.model.AdministrativeCase
import com.sorenkai.core.model.CaseDecision
import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.CaseLock
import com.sorenkai.core.model.CaseOutcome
import com.sorenkai.core.model.CaseSubject
import com.sorenkai.core.model.EvidenceReference
import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.LeaseToken
import com.sorenkai.core.model.Permission
import com.sorenkai.core.repository.CaseLockRepository
import com.sorenkai.core.repository.CaseRepository
import com.sorenkai.core.repository.LockAcquisitionResult
import com.sorenkai.core.transaction.DirectTransactionManager
import com.sorenkai.core.transaction.TransactionManager
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Service orchestrating administrative case lifecycle, exclusive handling leases, and decision resolutions.
 */
interface CaseManagementService {
    companion object {
        const val DEFAULT_LOCK_DURATION_MINUTES: Long = 15L
        val DEFAULT_LOCK_DURATION: Duration = Duration.ofMinutes(DEFAULT_LOCK_DURATION_MINUTES)
    }

    /**
     * Creates and stores a new administrative case in the open state.
     *
     * @param creatorId The identity creating the case.
     * @param subject Reference to the subject under review.
     * @param reason Justification for opening the case.
     * @param evidence Supporting evidence references.
     * @param id Optional explicit [CaseId]; generated automatically if omitted.
     * @return The newly created [AdministrativeCase].
     */
    fun createCase(
        creatorId: IdentityId,
        subject: CaseSubject,
        reason: String,
        evidence: List<EvidenceReference> = emptyList(),
        id: CaseId = CaseId.generate(),
    ): AdministrativeCase

    /**
     * Retrieves an administrative case by its [caseId] without acquiring any lock.
     *
     * @param caseId The unique identifier of the case.
     * @return The [AdministrativeCase] if found; `null` otherwise.
     */
    fun getCase(caseId: CaseId): AdministrativeCase?

    /**
     * Atomically acquires an exclusive handling lock on the specified case.
     *
     * @param caseId The case to lock.
     * @param adminId The administrator requesting exclusive handling.
     * @param duration The duration for which the lock lease is valid.
     * @param requiredPermission The administrative permission required to handle cases.
     * @return The acquired [CaseLock].
     * @throws UnauthorizedCaseOperationException if [adminId] lacks [requiredPermission].
     * @throws CaseNotFoundException if [caseId] does not exist.
     * @throws CaseAlreadyResolvedException if the case has already been resolved.
     * @throws CaseAlreadyLockedException if another administrator currently holds an active lock on the case.
     */
    fun acquireLock(
        caseId: CaseId,
        adminId: IdentityId,
        duration: Duration = DEFAULT_LOCK_DURATION,
        requiredPermission: Permission = Permission.MODERATE_DISCUSSIONS,
    ): CaseLock

    /**
     * Releases an active handling lock held by [adminId] with [token] without resolving the case.
     *
     * @param caseId The locked case.
     * @param adminId The current lock holder.
     * @param token The active lease token.
     * @throws InvalidLockTokenException if the lock does not exist, is held by another user, or has a mismatched token.
     */
    fun releaseLock(
        caseId: CaseId,
        adminId: IdentityId,
        token: LeaseToken,
    )

    /**
     * Renews an active handling lease for an additional [duration].
     *
     * @param caseId The locked case.
     * @param adminId The current lock holder.
     * @param token The active lease token.
     * @param duration The duration to extend the lease by.
     * @param requiredPermission The administrative permission required.
     * @return The renewed [CaseLock].
     * @throws UnauthorizedCaseOperationException if [adminId] lacks [requiredPermission].
     * @throws CaseNotFoundException if [caseId] does not exist.
     * @throws CaseAlreadyResolvedException if the case is already resolved.
     * @throws LockExpiredException if the current lock lease has already expired.
     * @throws InvalidLockTokenException if the lock is missing, held by another user, or has a mismatched token.
     */
    fun renewLock(
        caseId: CaseId,
        adminId: IdentityId,
        token: LeaseToken,
        duration: Duration = DEFAULT_LOCK_DURATION,
        requiredPermission: Permission = Permission.MODERATE_DISCUSSIONS,
    ): CaseLock

    /**
     * Resolves an open case by recording a decision, verifying active lock ownership, and releasing the lock.
     *
     * @param caseId The case to resolve.
     * @param adminId The deciding administrator identity.
     * @param token The active lease token proving lock ownership.
     * @param outcome The determined outcome.
     * @param justification Rationale for the decision.
     * @param requiredPermission Administrative permission required to render the decision.
     * @return The resolved [AdministrativeCase].
     * @throws UnauthorizedCaseOperationException if [adminId] lacks [requiredPermission].
     * @throws CaseNotFoundException if [caseId] does not exist.
     * @throws CaseAlreadyResolvedException if [caseId] is already resolved.
     * @throws CaseCreatorSelfDecisionException if [adminId] is the creator of the case.
     * @throws LockExpiredException if the active lock lease has expired.
     * @throws InvalidLockTokenException if no active lock exists, or if [token] / [adminId] does not match the active lock.
     */
    fun resolveCase(
        caseId: CaseId,
        adminId: IdentityId,
        token: LeaseToken,
        outcome: CaseOutcome,
        justification: String,
        requiredPermission: Permission = Permission.DECIDE_SUSPENSIONS,
    ): AdministrativeCase

    /**
     * Retrieves the currently active (unexpired) lock for [caseId], if one exists.
     */
    fun getActiveLock(caseId: CaseId): CaseLock?
}

/**
 * Default implementation of [CaseManagementService] enforcing atomic locking, authorization,
 * and creator separation policies.
 */
class DefaultCaseManagementService(
    private val caseRepository: CaseRepository,
    private val lockRepository: CaseLockRepository,
    private val authorizationService: AuthorizationService,
    private val tx: TransactionManager = DirectTransactionManager(),
    private val clock: Clock = Clock.systemUTC(),
    private val defaultLockDuration: Duration = CaseManagementService.DEFAULT_LOCK_DURATION,
) : CaseManagementService {
    override fun createCase(
        creatorId: IdentityId,
        subject: CaseSubject,
        reason: String,
        evidence: List<EvidenceReference>,
        id: CaseId,
    ): AdministrativeCase {
        val now = clock.instant()
        val case =
            AdministrativeCase(
                id = id,
                createdBy = creatorId,
                createdAt = now,
                subject = subject,
                reason = reason,
                evidence = evidence,
            )
        return caseRepository.save(case)
    }

    override fun getCase(caseId: CaseId): AdministrativeCase? = caseRepository.findById(caseId)

    override fun acquireLock(
        caseId: CaseId,
        adminId: IdentityId,
        duration: Duration,
        requiredPermission: Permission,
    ): CaseLock {
        ensureAuthorized(adminId, requiredPermission, "acquire handling lock on")

        val case = getRequiredOpenCase(caseId)
        val now = clock.instant()
        val token = LeaseToken.generate()
        val expiresAt = now.plus(duration)

        val result =
            lockRepository.tryAcquire(
                caseId = case.id,
                holderId = adminId,
                token = token,
                acquiredAt = now,
                expiresAt = expiresAt,
                now = now,
            )

        return when (result) {
            is LockAcquisitionResult.Acquired -> {
                result.lock
            }

            is LockAcquisitionResult.AlreadyLocked -> {
                throw CaseAlreadyLockedException(
                    caseId = caseId,
                    holderId = result.activeLock.holderId,
                    expiresAt = result.activeLock.expiresAt,
                )
            }
        }
    }

    override fun releaseLock(
        caseId: CaseId,
        adminId: IdentityId,
        token: LeaseToken,
    ) {
        val released = lockRepository.releaseLock(caseId, adminId, token)
        if (!released) {
            failRelease(caseId, adminId)
        }
    }

    override fun renewLock(
        caseId: CaseId,
        adminId: IdentityId,
        token: LeaseToken,
        duration: Duration,
        requiredPermission: Permission,
    ): CaseLock {
        ensureAuthorized(adminId, requiredPermission, "renew handling lock on")
        val case = getRequiredOpenCase(caseId)

        val now = clock.instant()
        val newExpiresAt = now.plus(duration)
        val renewed =
            lockRepository.tryRenew(
                caseId = case.id,
                holderId = adminId,
                token = token,
                renewedAt = now,
                newExpiresAt = newExpiresAt,
                now = now,
            )

        return renewed ?: failRenewal(caseId, adminId, now)
    }

    override fun resolveCase(
        caseId: CaseId,
        adminId: IdentityId,
        token: LeaseToken,
        outcome: CaseOutcome,
        justification: String,
        requiredPermission: Permission,
    ): AdministrativeCase {
        ensureAuthorized(adminId, requiredPermission, "resolve")

        return runBlocking {
            tx.inTransaction {
                val case = getRequiredOpenCase(caseId)

                if (case.createdBy == adminId) {
                    throw CaseCreatorSelfDecisionException(caseId = caseId, creatorId = adminId)
                }

                val now = clock.instant()
                validateActiveLockForDecision(caseId, adminId, token, now)

                val decision =
                    CaseDecision(
                        decidedBy = adminId,
                        decidedAt = now,
                        outcome = outcome,
                        justification = justification,
                    )

                val resolvedCase = case.resolve(decision)
                val savedCase = caseRepository.save(resolvedCase)

                // Resolving a case ends its active handling lock
                lockRepository.forceRelease(caseId)

                savedCase
            }
        }
    }

    override fun getActiveLock(caseId: CaseId): CaseLock? {
        val lock = lockRepository.findLock(caseId) ?: return null
        return if (lock.isActive(clock.instant())) lock else null
    }

    private fun getRequiredOpenCase(caseId: CaseId): AdministrativeCase {
        val case = caseRepository.findById(caseId) ?: throw CaseNotFoundException(caseId)
        if (case.isResolved) {
            throw CaseAlreadyResolvedException(caseId)
        }
        return case
    }

    private fun validateActiveLockForDecision(
        caseId: CaseId,
        adminId: IdentityId,
        token: LeaseToken,
        now: Instant,
    ) {
        val activeLock =
            lockRepository.findLock(caseId)
                ?: throw InvalidLockTokenException(
                    caseId,
                    "Cannot resolve case '${caseId.value}': no active handling lock exists.",
                )

        if (activeLock.isExpired(now)) {
            throw LockExpiredException(
                caseId,
                "Cannot resolve case '${caseId.value}': handling lock expired at '${activeLock.expiresAt}'.",
            )
        }

        if (activeLock.holderId != adminId) {
            throw InvalidLockTokenException(
                caseId,
                "Cannot resolve case '${caseId.value}': lock is held by administrator '${activeLock.holderId.value}'.",
            )
        }

        if (activeLock.token != token) {
            throw InvalidLockTokenException(
                caseId,
                "Cannot resolve case '${caseId.value}': provided lease token is invalid or superseded.",
            )
        }
    }

    private fun failRelease(
        caseId: CaseId,
        adminId: IdentityId,
    ): Nothing {
        val existing = lockRepository.findLock(caseId)
        when {
            existing == null -> {
                throw InvalidLockTokenException(caseId, "No active lock exists for case '${caseId.value}'.")
            }

            existing.holderId != adminId -> {
                throw InvalidLockTokenException(
                    caseId,
                    "Lock for case '${caseId.value}' is held by another administrator '${existing.holderId.value}'.",
                )
            }

            else -> {
                throw InvalidLockTokenException(
                    caseId,
                    "Provided lease token is invalid or superseded for case '${caseId.value}'.",
                )
            }
        }
    }

    private fun failRenewal(
        caseId: CaseId,
        adminId: IdentityId,
        now: Instant,
    ): Nothing {
        val existing = lockRepository.findLock(caseId)
        when {
            existing == null -> {
                throw InvalidLockTokenException(caseId, "No lock exists to renew for case '${caseId.value}'.")
            }

            existing.isExpired(now) -> {
                throw LockExpiredException(
                    caseId,
                    "Handling lock for case '${caseId.value}' has expired and cannot be renewed.",
                )
            }

            existing.holderId != adminId -> {
                throw InvalidLockTokenException(
                    caseId,
                    "Lock for case '${caseId.value}' is held by another administrator '${existing.holderId.value}'.",
                )
            }

            else -> {
                throw InvalidLockTokenException(
                    caseId,
                    "Provided lease token is invalid or superseded for case '${caseId.value}'.",
                )
            }
        }
    }

    private fun ensureAuthorized(
        identityId: IdentityId,
        permission: Permission,
        actionDescription: String,
    ) {
        val decision = authorizationService.evaluate(identityId, permission)
        if (decision.isDenied) {
            throw UnauthorizedCaseOperationException(
                "Identity '${identityId.value}' is not authorized to $actionDescription cases (missing permission '${permission.name}').",
            )
        }
    }
}
