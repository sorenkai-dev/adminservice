package com.sorenkai.persistence.transaction

import com.sorenkai.core.transaction.TransactionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager as ExposedInternalTransactionManager

/**
 * Implementation of [TransactionManager] backed by JetBrains Exposed DSL.
 */
class ExposedTransactionManager(
    private val database: Database? = null,
) : TransactionManager {
    private val log = LoggerFactory.getLogger(ExposedTransactionManager::class.java)

    override suspend fun <T> inTransaction(block: suspend () -> T): T = dbQuery(database, log) { block() }
}

@Suppress("DEPRECATION")
internal suspend fun <T> dbQuery(
    database: Database? = null,
    logger: Logger? = null,
    block: suspend () -> T,
): T =
    withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            newSuspendedTransaction(Dispatchers.IO, db = database) {
                block()
            }
        } finally {
            val duration = System.currentTimeMillis() - start
            logger?.debug("Transaction executed in {} ms", duration)
        }
    }

/**
 * Helper to execute an Exposed block within the current active transaction if present,
 * or within a newly created transaction if called standalone.
 */
internal fun <T> inExposedTransaction(
    db: Database? = null,
    statement: Transaction.() -> T,
): T {
    val current = ExposedInternalTransactionManager.currentOrNull()
    return if (current != null) {
        statement(current)
    } else {
        transaction(db, statement = statement)
    }
}
