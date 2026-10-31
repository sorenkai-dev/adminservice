package com.sorenkai.app

import com.sorenkai.app.di.observabilityModule
import com.sorenkai.app.di.serverModule
import com.sorenkai.app.di.serviceModule
import com.sorenkai.persistence.DatabaseMigrator
import com.sorenkai.persistence.di.persistenceModule
import org.koin.dsl.koinApplication

/**
 * Entry point for the Soren Kai Admin Service.
 *
 * Initializes the Koin dependency injection container, binds configuration properties such as the gRPC port,
 * runs pending database migrations, starts the [GrpcServer], and registers JVM shutdown hooks to ensure graceful
 * termination of active connections, resources, and connection pools.
 */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    val application =
        koinApplication {
            properties(
                mapOf(
                    "grpc.port" to port,
                ),
            )

            modules(
                persistenceModule,
                serviceModule,
                observabilityModule,
                serverModule,
            )
        }

    // Run Flyway database migrations before database-dependent services start
    val migrator = application.koin.get<DatabaseMigrator>()
    migrator.migrate()

    val server = application.koin.get<GrpcServer>()
    val lifecycle = ApplicationLifecycle(server, application)

    Runtime.getRuntime().addShutdownHook(
        Thread {
            lifecycle.shutdown()
        },
    )

    try {
        server.start()
        server.blockUntilShutdown()
    } finally {
        lifecycle.shutdown()
    }
}
