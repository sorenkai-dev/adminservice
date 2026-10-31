package com.sorenkai.core.model

import java.time.Instant

/**
 * An immutable record of an administrative decision resolving a case.
 *
 * Captures accountability details: who made the decision, when it occurred, the outcome,
 * and the justification.
 *
 * @property decidedBy The administrator identity who made the decision.
 * @property decidedAt The timestamp at which the decision was rendered.
 * @property outcome The determined outcome for the case.
 * @property justification The justification or rationale explaining the outcome.
 */
data class CaseDecision(
    val decidedBy: IdentityId,
    val decidedAt: Instant,
    val outcome: CaseOutcome,
    val justification: String,
) {
    init {
        require(justification.isNotBlank()) { "Decision justification cannot be blank." }
    }
}
