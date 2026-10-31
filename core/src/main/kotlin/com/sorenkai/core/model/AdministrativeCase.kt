package com.sorenkai.core.model

import java.time.Instant

/**
 * An administrative case representing an issue, report, or violation under administrative oversight.
 *
 * Cases maintain strict immutable integrity: original creation metadata (creator, timestamp, subject,
 * reason, evidence) is permanently preserved throughout lifecycle transitions.
 *
 * @property id Unique case identifier.
 * @property createdBy The identity that created the case.
 * @property createdAt Timestamp when the case was initiated.
 * @property subject Reference to the subject of the case.
 * @property reason The reason the case was created.
 * @property evidence List of supporting evidence references.
 * @property status Current lifecycle status ([CaseStatus.OPEN] or [CaseStatus.RESOLVED]).
 * @property decision The resolution decision, populated only when [status] is [CaseStatus.RESOLVED].
 */
data class AdministrativeCase(
    val id: CaseId,
    val createdBy: IdentityId,
    val createdAt: Instant,
    val subject: CaseSubject,
    val reason: String,
    val evidence: List<EvidenceReference> = emptyList(),
    val status: CaseStatus = CaseStatus.OPEN,
    val decision: CaseDecision? = null,
) {
    init {
        require(reason.isNotBlank()) { "Case reason cannot be blank." }
        if (status == CaseStatus.RESOLVED) {
            requireNotNull(decision) { "A resolved case must include a decision." }
        } else {
            require(decision == null) { "An open case cannot contain a decision." }
        }
    }

    /**
     * `true` if the case is in the [CaseStatus.OPEN] state.
     */
    val isOpen: Boolean get() = status == CaseStatus.OPEN

    /**
     * `true` if the case is in the [CaseStatus.RESOLVED] state.
     */
    val isResolved: Boolean get() = status == CaseStatus.RESOLVED

    /**
     * Transitions this open case to [CaseStatus.RESOLVED] by attaching the provided [decision].
     *
     * Preserves all original creation information unchanged.
     *
     * @param decision The administrative decision resolving the case.
     * @return A new [AdministrativeCase] instance with [status] set to [CaseStatus.RESOLVED].
     * @throws IllegalStateException if the case is not in the [CaseStatus.OPEN] state.
     */
    fun resolve(decision: CaseDecision): AdministrativeCase {
        check(isOpen) { "Cannot resolve case '$id' because it is already resolved." }
        return copy(
            status = CaseStatus.RESOLVED,
            decision = decision,
        )
    }
}
