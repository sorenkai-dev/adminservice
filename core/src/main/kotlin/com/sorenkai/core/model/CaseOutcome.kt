package com.sorenkai.core.model

/**
 * Value object representing the outcome of an administrative decision on a case.
 *
 * @property value The outcome description or classification code.
 */
@JvmInline
value class CaseOutcome(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Case outcome cannot be blank." }
    }

    override fun toString(): String = value

    companion object {
        val DISMISSED = CaseOutcome("DISMISSED")
        val SUSPEND_ACCOUNT = CaseOutcome("SUSPEND_ACCOUNT")
        val BAN_ACCOUNT = CaseOutcome("BAN_ACCOUNT")
        val NO_ACTION = CaseOutcome("NO_ACTION")
    }
}
