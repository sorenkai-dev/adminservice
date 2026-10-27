package com.sorenkai.app

import com.sorenkai.app.di.observabilityModule
import com.sorenkai.app.di.serverModule
import com.sorenkai.app.di.serviceModule
import org.koin.dsl.koinApplication

/**
 * Entry point for the Soren Kai Admin Service.
 *
 * Initializes the Koin dependency injection container, binds configuration properties such as the gRPC port,
 * starts the [GrpcServer], and registers JVM shutdown hooks to ensure graceful termination of active connections
 * and resources.
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
                serviceModule,
                observabilityModule,
                serverModule,
            )
        }

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
