package com.sorenkai.core.authorization

/**
 * Exception thrown when an administrative actor attempts an unauthorized role-management operation.
 */
class UnauthorizedRoleOperationException(
    message: String,
) : IllegalStateException(message)
