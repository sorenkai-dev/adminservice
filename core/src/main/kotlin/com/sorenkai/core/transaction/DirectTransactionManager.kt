package com.sorenkai.core.transaction

/**
 * Direct [TransactionManager] implementation that executes blocks immediately in-place
 * without opening a database transaction.
 *
 * Intended for in-memory implementations and unit tests.
 */
class DirectTransactionManager : TransactionManager {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
}
