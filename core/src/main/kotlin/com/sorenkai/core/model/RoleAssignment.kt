package com.sorenkai.core.model

import java.time.Instant

/**
 * Represents the assignment of an administrative [Role] to an identity.
 *
 * @property identityId The identifier of the identity receiving the role assignment.
 * @property role The administrative role assigned to the identity.
 * @property assignedAt The timestamp at which the role was assigned.
 * @property assignedBy The identifier of the administrative actor who assigned the role, or `null` if system-assigned.
 */
data class RoleAssignment(
    val identityId: IdentityId,
    val role: Role,
    val assignedAt: Instant = Instant.now(),
    val assignedBy: IdentityId? = null,
)
