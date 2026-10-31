package com.sorenkai.core.model

/**
 * Value object representing a reference to the subject of an administrative case.
 *
 * @property value The unique identifier or reference string for the subject.
 */
@JvmInline
value class CaseSubject(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Case subject reference cannot be blank." }
    }

    override fun toString(): String = value
}
