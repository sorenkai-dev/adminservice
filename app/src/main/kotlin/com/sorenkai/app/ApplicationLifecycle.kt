package com.sorenkai.app

import org.koin.core.KoinApplication
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Manages the graceful shutdown lifecycle of the application.
 *
 * Coordinates orderly stopping of the [GrpcServer] and closure of the [KoinApplication]
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
     * Stops the [server] to drain active in-flight requests and subsequently releases all Koin DI singletons.
     */
    fun shutdown() {
        if (!stopped.compareAndSet(false, true)) {
            return
        }

        try {
            server.stop()
        } finally {
            application.close()
        }
    }
}
