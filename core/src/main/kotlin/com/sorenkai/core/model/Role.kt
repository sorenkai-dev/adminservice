package com.sorenkai.core.model

/**
 * Fixed administrative roles within the administrative security model.
 *
 * Each role possesses a set of direct permissions and explicitly inherits permissions from lower-tier roles.
 *
 * @property directPermissions The set of permissions directly granted at this role level.
 * @property inheritedRoles The immediate lower-tier roles whose permissions are inherited by this role.
 */
enum class Role(
    val directPermissions: Set<Permission>,
    val inheritedRoles: Set<Role> = emptySet(),
) {
    /**
     * Moderator role with permissions to moderate discussions and suspend accounts.
     */
    MODERATOR(
        directPermissions =
            setOf(
                Permission.MODERATE_DISCUSSIONS,
                Permission.SUSPEND_ACCOUNTS,
            ),
    ),

    /**
     * Administrator role inheriting all Moderator permissions, with additional privileges
     * to decide suspensions, ban accounts, manage moderators, review appeals, and access site analytics.
     */
    ADMIN(
        directPermissions =
            setOf(
                Permission.DECIDE_SUSPENSIONS,
                Permission.BAN_ACCOUNTS,
                Permission.MANAGE_MODERATORS,
                Permission.REVIEW_APPEALS,
                Permission.ACCESS_SITE_ANALYTICS,
            ),
        inheritedRoles = setOf(MODERATOR),
    ),

    /**
     * Owner role inheriting all Admin and Moderator permissions, with additional privileges
     * to add and remove administrators.
     */
    OWNER(
        directPermissions =
            setOf(
                Permission.ADD_ADMINS,
                Permission.REMOVE_ADMINS,
            ),
        inheritedRoles = setOf(ADMIN),
    ),
    ;

    /**
     * The full effective set of permissions granted to this role, computed from direct permissions
     * and recursive permission inheritance.
     */
    val permissions: Set<Permission> by lazy {
        val all = mutableSetOf<Permission>()
        all.addAll(directPermissions)
        for (inherited in inheritedRoles) {
            all.addAll(inherited.permissions)
        }
        all
    }

    /**
     * Checks if this role has the specified [permission], either directly or via inheritance.
     *
     * @param permission The [Permission] to check.
     * @return `true` if the permission is granted to this role; `false` otherwise.
     */
    fun hasPermission(permission: Permission): Boolean = permissions.contains(permission)

    /**
     * Checks if this role inherits permissions from the [other] role directly or transitively.
     *
     * @param other The potential ancestor role.
     * @return `true` if this role inherits from [other]; `false` otherwise.
     */
    fun inheritsFrom(other: Role): Boolean {
        if (inheritedRoles.contains(other)) return true
        return inheritedRoles.any { it.inheritsFrom(other) }
    }
}
