package com.sorenkai.core.repository

import com.sorenkai.core.model.AdministrativeCase
import com.sorenkai.core.model.CaseId

/**
 * Storage and retrieval contract for [AdministrativeCase] aggregates.
 */
interface CaseRepository {
    /**
     * Persists or updates the specified [case].
     *
     * @param case The administrative case to save.
     * @return The persisted [AdministrativeCase].
     */
    fun save(case: AdministrativeCase): AdministrativeCase

    /**
     * Retrieves an administrative case by its [id].
     *
     * @param id The unique identifier of the case.
     * @return The [AdministrativeCase] if found; `null` otherwise.
     */
    fun findById(id: CaseId): AdministrativeCase?

    /**
     * Returns all stored administrative cases.
     */
    fun findAll(): List<AdministrativeCase>
}
