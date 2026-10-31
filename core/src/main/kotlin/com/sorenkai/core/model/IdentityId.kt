package com.sorenkai.core.model

/**
 * Value object uniquely representing an identity within the administrative domain.
 *
 * @property value The unique string identifier for the identity.
 */
@JvmInline
value class IdentityId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Identity ID cannot be blank." }
    }

    override fun toString(): String = value
}
