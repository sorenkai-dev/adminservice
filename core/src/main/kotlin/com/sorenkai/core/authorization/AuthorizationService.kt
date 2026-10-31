package com.sorenkai.core.authorization

import com.sorenkai.core.model.IdentityId
import com.sorenkai.core.model.Permission
import com.sorenkai.core.model.Role
import com.sorenkai.core.repository.RoleAssignmentRepository

/**
 * Service evaluating authorization requests against administrative identities and roles.
 *
 * Implements default-denial: any unassigned identity, null role, or missing permission
 * strictly evaluates to denied.
 */
interface AuthorizationService {
    /**
     * Evaluates whether the given [identityId] has the requested [permission].
     */
    fun evaluate(
        identityId: IdentityId,
        permission: Permission,
    ): AuthorizationDecision

    /**
     * Evaluates whether the given [role] has the requested [permission].
     */
    fun evaluate(
        role: Role?,
        permission: Permission,
        identityId: IdentityId? = null,
    ): AuthorizationDecision

    /**
     * Convenience method returning `true` only if [identityId] is authorized for [permission].
     */
    fun isAuthorized(
        identityId: IdentityId,
        permission: Permission,
    ): Boolean

    /**
     * Convenience method returning `true` only if [role] is authorized for [permission].
     */
    fun isAuthorized(
        role: Role?,
        permission: Permission,
    ): Boolean

    /**
     * Returns the full set of effective permissions granted to the specified [identityId].
     */
    fun getEffectivePermissions(identityId: IdentityId): Set<Permission>
}

/**
 * Default implementation of [AuthorizationService] evaluating permissions against a [RoleAssignmentRepository].
 */
class DefaultAuthorizationService(
    private val repository: RoleAssignmentRepository,
) : AuthorizationService {
    override fun evaluate(
        identityId: IdentityId,
        permission: Permission,
    ): AuthorizationDecision {
        val role =
            repository.findRoleByIdentity(identityId)
                ?: return AuthorizationDecision.Denied(
                    identityId = identityId,
                    role = null,
                    permission = permission,
                    reason = "Access denied: identity '$identityId' has no administrative role assigned.",
                )

        return evaluate(role = role, permission = permission, identityId = identityId)
    }

    override fun evaluate(
        role: Role?,
        permission: Permission,
        identityId: IdentityId?,
    ): AuthorizationDecision {
        if (role == null) {
            return AuthorizationDecision.Denied(
                identityId = identityId,
                role = null,
                permission = permission,
                reason = "Access denied: no administrative role specified.",
            )
        }

        return if (role.hasPermission(permission)) {
            AuthorizationDecision.Granted(
                identityId = identityId,
                role = role,
                permission = permission,
            )
        } else {
            AuthorizationDecision.Denied(
                identityId = identityId,
                role = role,
                permission = permission,
                reason = "Access denied: role '${role.name}' does not possess permission '${permission.name}'.",
            )
        }
    }

    override fun isAuthorized(
        identityId: IdentityId,
        permission: Permission,
    ): Boolean = evaluate(identityId, permission).isGranted

    override fun isAuthorized(
        role: Role?,
        permission: Permission,
    ): Boolean = evaluate(role, permission).isGranted

    override fun getEffectivePermissions(identityId: IdentityId): Set<Permission> {
        val role = repository.findRoleByIdentity(identityId) ?: return emptySet()
        return role.permissions
    }
}
