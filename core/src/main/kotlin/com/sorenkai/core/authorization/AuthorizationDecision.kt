package com.sorenkai.core.authorization

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Permission
import com.sorenkai.core.model.Role

/**
 * Represents the outcome of an administrative authorization evaluation.
 */
sealed interface AuthorizationDecision {
    /** Whether the requested permission is granted. */
    val isGranted: Boolean

    /** Whether the requested permission is denied (default is denial). */
    val isDenied: Boolean get() = !isGranted

    /** The identity evaluated, or `null` if evaluating a role in isolation. */
    val identityId: IdentityId?

    /** The administrative role resolved during evaluation, or `null` if none. */
    val role: Role?

    /** The specific permission requested. */
    val permission: Permission

    /** A descriptive explanation for the authorization decision. */
    val reason: String

    /**
     * Decision indicating that the requested permission is granted.
     */
    data class Granted(
        override val identityId: IdentityId?,
        override val role: Role,
        override val permission: Permission,
        override val reason: String = "Permission '${permission.name}' is granted for role '${role.name}'.",
    ) : AuthorizationDecision {
        override val isGranted: Boolean get() = true
    }

    /**
     * Decision indicating that the requested permission is denied.
     */
    data class Denied(
        override val identityId: IdentityId?,
        override val role: Role?,
        override val permission: Permission,
        override val reason: String,
    ) : AuthorizationDecision {
        override val isGranted: Boolean get() = false
    }
}
