package com.sorenkai.core.cases

import com.sorenkai.core.model.CaseId
import com.sorenkai.core.model.IdentityId
import java.time.Instant

/**
 * Base exception for administrative case management errors.
 */
sealed class CaseManagementException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Thrown when an administrative case cannot be found by its [caseId].
 */
class CaseNotFoundException(
    val caseId: CaseId,
) : CaseManagementException("Administrative case '${caseId.value}' was not found.")

/**
 * Thrown when attempting an operation on an administrative case that has already been resolved.
 */
class CaseAlreadyResolvedException(
    val caseId: CaseId,
) : CaseManagementException("Administrative case '${caseId.value}' has already reached a final resolution.")

/**
 * Thrown when attempting to acquire an exclusive lock on a case that is actively held by another administrator.
 */
class CaseAlreadyLockedException(
    val caseId: CaseId,
    val holderId: IdentityId,
    val expiresAt: Instant,
) : CaseManagementException(
        "Administrative case '${caseId.value}' is already exclusively locked by administrator '${holderId.value}' until '$expiresAt'.",
    )

/**
 * Thrown when the creator of a case attempts to render a decision on their own case.
 */
class CaseCreatorSelfDecisionException(
    val caseId: CaseId,
    val creatorId: IdentityId,
) : CaseManagementException(
        "Case creator '${creatorId.value}' is strictly prohibited from deciding their own case '${caseId.value}'.",
    )

/**
 * Thrown when an actor attempts an administrative case operation without the required permission or role.
 */
class UnauthorizedCaseOperationException(
    message: String,
) : CaseManagementException(message)

/**
 * Thrown when a lease token provided for an operation is invalid, mismatched, or superseded.
 */
class InvalidLockTokenException(
    val caseId: CaseId,
    message: String,
) : CaseManagementException(message)

/**
 * Thrown when attempting to decide or renew a case using an expired lock lease.
 */
class LockExpiredException(
    val caseId: CaseId,
    message: String,
) : CaseManagementException(message)
