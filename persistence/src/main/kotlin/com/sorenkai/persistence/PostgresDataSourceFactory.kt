package com.sorenkai.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

private const val DEFAULT_POSTGRES_PORT = 5432
private const val DEFAULT_MAX_POOL_SIZE = 10
private const val DEFAULT_MIN_IDLE = 2

/**
 * Configuration parameters for connecting to PostgreSQL.
 */
data class DatabaseConfig(
    val host: String = System.getenv("DB_HOST") ?: "localhost",
    val port: Int = System.getenv("DB_PORT")?.toIntOrNull() ?: DEFAULT_POSTGRES_PORT,
    val database: String = System.getenv("DB_NAME") ?: "adminservice",
    val user: String = System.getenv("DB_USER") ?: "admin",
    val password: String = System.getenv("DB_PASSWORD") ?: "admin",
    val maximumPoolSize: Int = DEFAULT_MAX_POOL_SIZE,
    val minimumIdle: Int = DEFAULT_MIN_IDLE,
)

/**
 * Factory for creating configured [HikariDataSource] instances.
 */
object PostgresDataSourceFactory {
    /**
     * Creates a new [HikariDataSource] using the specified [config].
     */
    fun create(config: DatabaseConfig = DatabaseConfig()): HikariDataSource {
        val jdbcUrl = "jdbc:postgresql://${config.host}:${config.port}/${config.database}"
        val hikariConfig =
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.username = config.user
                this.password = config.password
                this.maximumPoolSize = config.maximumPoolSize
                this.minimumIdle = config.minimumIdle
                this.initializationFailTimeout = -1L
                this.poolName = "AdminServiceHikariPool"
            }
        return HikariDataSource(hikariConfig)
    }
}
