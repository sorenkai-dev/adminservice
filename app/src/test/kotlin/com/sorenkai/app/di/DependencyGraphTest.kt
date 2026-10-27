package com.sorenkai.app.di

import com.sorenkai.app.GrpcServer
import com.sorenkai.app.metrics.GrpcObservability
import com.sorenkai.app.service.AdminServiceImpl
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.koin.dsl.koinApplication

/**
 * Unit tests validating Koin dependency injection configuration and graph completeness.
 */
class DependencyGraphTest {
    /**
     * Verifies that all required singletons (server, services, registries) resolve successfully as singletons.
     */
    @Test
    fun `Koin resolves the Admin application dependency graph`() {
        val application =
            koinApplication {
                properties(
                    mapOf(
                        "grpc.port" to 0,
                    ),
                )

                modules(
                    serviceModule,
                    observabilityModule,
                    serverModule,
                )
            }

        try {
            val koin = application.koin

            val server = koin.get<GrpcServer>()
            val adminService = koin.get<AdminServiceImpl>()
            val observability = koin.get<GrpcObservability>()
            val meterRegistry = koin.get<MeterRegistry>()
            val observationRegistry = koin.get<ObservationRegistry>()

            assertNotNull(server)
            assertNotNull(adminService)
            assertNotNull(observability)

            assertSame(server, koin.get<GrpcServer>())
            assertSame(adminService, koin.get<AdminServiceImpl>())
            assertSame(observability, koin.get<GrpcObservability>())

            assertSame(meterRegistry, koin.get<MeterRegistry>())
            assertSame(observationRegistry, koin.get<ObservationRegistry>())
        } finally {
            application.close()
        }
    }
}
