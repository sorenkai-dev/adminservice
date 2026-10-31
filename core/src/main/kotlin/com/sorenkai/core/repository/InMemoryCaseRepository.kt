package com.sorenkai.core.repository

import com.sorenkai.core.model.AdministrativeCase
import com.sorenkai.core.model.CaseId
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory implementation of [CaseRepository].
 */
class InMemoryCaseRepository : CaseRepository {
    private val cases = ConcurrentHashMap<CaseId, AdministrativeCase>()

    override fun save(case: AdministrativeCase): AdministrativeCase {
        cases[case.id] = case
        return case
    }

    override fun findById(id: CaseId): AdministrativeCase? = cases[id]

    override fun findAll(): List<AdministrativeCase> = cases.values.toList()

    /**
     * Clears all stored cases.
     */
    fun clear() {
        cases.clear()
    }
}
