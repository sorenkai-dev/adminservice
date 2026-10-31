package com.sorenkai.app

import com.zaxxer.hikari.HikariDataSource
import org.koin.core.KoinApplication
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Manages the graceful shutdown lifecycle of the application.
 *
 * Coordinates orderly stopping of the [GrpcServer], closing connection pools, and closure of the [KoinApplication]
 * dependency injection container, guaranteeing idempotent execution upon termination.
 *
 * @property server The running gRPC server instance to be stopped.
 * @property application The Koin dependency container to be closed after server termination.
 */
@OptIn(ExperimentalAtomicApi::class)
class ApplicationLifecycle(
    private val server: GrpcServer,
    private val application: KoinApplication,
) {
    private val stopped = AtomicBoolean(false)

    /**
     * Executes the graceful shutdown sequence idempotently.
     *
     * Stops the [server] to drain active in-flight requests, closes database connection pools,
     * and subsequently releases all Koin DI singletons.
     */
    fun shutdown() {
        if (!stopped.compareAndSet(false, true)) {
            return
        }

        try {
            server.stop()
        } finally {
            try {
                val dataSource = application.koin.getOrNull<HikariDataSource>()
                dataSource?.close()
            } finally {
                application.close()
            }
        }
    }
}
