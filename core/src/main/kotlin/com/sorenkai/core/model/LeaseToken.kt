package com.sorenkai.core.model

import java.util.UUID

/**
 * Value object representing a unique cryptographic lease token associated with an exclusive handling lock.
 *
 * @property value The unique token string.
 */
@JvmInline
value class LeaseToken(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Lease token cannot be blank." }
    }

    override fun toString(): String = value

    companion object {
        /**
         * Generates a new random unique [LeaseToken].
         */
        fun generate(): LeaseToken = LeaseToken(UUID.randomUUID().toString())
    }
}
