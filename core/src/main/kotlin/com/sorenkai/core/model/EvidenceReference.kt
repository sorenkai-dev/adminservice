package com.sorenkai.core.model

/**
 * Value object representing a reference to supporting evidence for an administrative case.
 *
 * @property value The URI, identifier, or locator string representing the evidence.
 */
@JvmInline
value class EvidenceReference(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Evidence reference cannot be blank." }
    }

    override fun toString(): String = value
}
