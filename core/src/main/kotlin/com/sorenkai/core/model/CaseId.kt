package com.sorenkai.core.model

import java.util.UUID

/**
 * Value object uniquely representing an administrative case.
 *
 * @property value The unique string identifier for the case.
 */
@JvmInline
value class CaseId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Case ID cannot be blank." }
    }

    override fun toString(): String = value

    companion object {
        /**
         * Generates a new random unique [CaseId].
         */
        fun generate(): CaseId = CaseId(UUID.randomUUID().toString())
    }
}
