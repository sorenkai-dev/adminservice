package com.sorenkai.persistence

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import javax.sql.DataSource

/**
 * Executes versioned database migrations using Flyway.
 */
class DatabaseMigrator(
    private val dataSource: DataSource,
) {
    /**
     * Runs all pending Flyway database migrations against the configured [dataSource].
     *
     * @return The [MigrateResult] containing details of applied migrations.
     */
    fun migrate(): MigrateResult {
        val flyway =
            Flyway
                .configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()

        return flyway.migrate()
    }
}
