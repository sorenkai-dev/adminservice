package com.sorenkai.app.di

import com.sorenkai.app.DeploymentInfoProvider
import com.sorenkai.app.GrpcServer
import com.sorenkai.app.RuntimeInfoProvider
import com.sorenkai.app.diagnostics.JvmGarbageCollectionDiagnosticsProvider
import com.sorenkai.app.diagnostics.JvmMemoryDiagnosticsProvider
import com.sorenkai.app.diagnostics.JvmThreadDiagnosticsProvider
import com.sorenkai.app.metrics.GrpcObservability
import com.sorenkai.app.service.AdminServiceImpl
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Dependency injection module for core providers and the administrative gRPC service.
 *
 * Provides singletons for:
 * - [RuntimeInfoProvider]
 * - [DeploymentInfoProvider]
 * - [JvmMemoryDiagnosticsProvider]
 * - [JvmThreadDiagnosticsProvider]
 * - [JvmGarbageCollectionDiagnosticsProvider]
 * - [AdminServiceImpl]
 */
val serviceModule =
    module {
        single { RuntimeInfoProvider() }
        single { DeploymentInfoProvider() }

        single { JvmMemoryDiagnosticsProvider() }
        single { JvmThreadDiagnosticsProvider() }
        single { JvmGarbageCollectionDiagnosticsProvider() }

        single {
            AdminServiceImpl(
                runtimeInfoProvider = get(),
                deploymentInfoProvider = get(),
                memoryProvider = get(),
                threadProvider = get(),
                garbageCollectionProvider = get(),
            )
        }
    }

/**
 * Dependency injection module for metrics collection and observability components.
 *
 * Provides singletons for:
 * - [MeterRegistry] (configured with [SimpleMeterRegistry] and closed on shutdown)
 * - [ObservationRegistry]
 * - [GrpcObservability]
 */
val observabilityModule =
    module {
        single<MeterRegistry> {
            SimpleMeterRegistry()
        } onClose { registry ->
            registry?.close()
        }

        single<ObservationRegistry> {
            ObservationRegistry.create()
        }

        single {
            GrpcObservability(
                observationRegistry = get(),
                meterRegistry = get(),
            )
        }
    }

/**
 * Dependency injection module for the gRPC server infrastructure.
 *
 * Configures and binds the [GrpcServer] singleton using the `grpc.port` property.
 */
val serverModule =
    module {
        single {
            GrpcServer(
                port = getProperty("grpc.port"),
                adminService = get(),
                observability = get(),
            )
        }
    }
