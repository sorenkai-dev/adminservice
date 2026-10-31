package com.sorenkai.persistence

import com.sorenkai.persistence.transaction.ExposedTransactionManager
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import javax.sql.DataSource

/**
 * Base class for PostgreSQL integration tests providing an isolated embedded PostgreSQL instance,
 * automated Flyway migrations, and table cleanup between test runs.
 */
abstract class PostgresIntegrationTestBase {
    companion object {
        private lateinit var embeddedPostgres: EmbeddedPostgres
        lateinit var dataSource: DataSource
        lateinit var database: Database
        lateinit var transactionManager: ExposedTransactionManager

        @BeforeAll
        @JvmStatic
        fun startDatabase() {
            embeddedPostgres = EmbeddedPostgres.builder().start()
            dataSource = embeddedPostgres.postgresDatabase
            database = Database.connect(dataSource)
            transactionManager = ExposedTransactionManager(database)
            DatabaseMigrator(dataSource).migrate()
        }

        @AfterAll
        @JvmStatic
        fun stopDatabase() {
            if (::embeddedPostgres.isInitialized) {
                embeddedPostgres.close()
            }
        }
    }

    @BeforeEach
    fun cleanDatabase() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("TRUNCATE TABLE case_locks, case_evidence, administrative_cases, role_assignments CASCADE")
            }
        }
    }
}
