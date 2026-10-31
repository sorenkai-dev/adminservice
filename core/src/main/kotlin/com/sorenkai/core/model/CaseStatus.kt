package com.sorenkai.core.model

/**
 * Lifecycle status of an administrative case.
 *
 * Cases transition strictly from [OPEN] to [RESOLVED].
 */
enum class CaseStatus {
    /** Case is actively open and awaiting handling or administrative determination. */
    OPEN,

    /** Case has reached a final administrative decision and resolution. */
    RESOLVED,
}
