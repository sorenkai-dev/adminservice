package com.sorenkai.core.transaction

/**
 * Contract allowing domain and application services to define transaction boundaries
 * without coupling to Exposed, JDBC, PostgreSQL, or another specific database framework.
 */
interface TransactionManager {
    /**
     * Executes the given [block] within a single transaction boundary.
     *
     * Successful completion commits the transaction. Any unhandled exception rolls back the transaction.
     */
    suspend fun <T> inTransaction(block: suspend () -> T): T
}
