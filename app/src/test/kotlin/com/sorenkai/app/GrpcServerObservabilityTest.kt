package com.sorenkai.app

import com.sorenkai.admin.api.v1.AdminServiceGrpcKt
import com.sorenkai.admin.api.v1.ServiceInfoRequest
import com.sorenkai.app.di.observabilityModule
import com.sorenkai.app.di.serverModule
import com.sorenkai.app.di.serviceModule
import io.grpc.ManagedChannelBuilder
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import java.util.concurrent.TimeUnit

/**
 * End-to-end integration tests verifying metrics collection on the live Netty gRPC server.
 *
 * Boots the complete Koin application context, executes RPC calls over local channels, and verifies
 * that Micrometer timers record expected metrics tags.
 */
class GrpcServerObservabilityTest {
    private lateinit var application: KoinApplication

    /**
     * Initializes the Koin application context with dynamic port assignment before each test.
     */
    @BeforeEach
    fun setUp() {
        application =
            koinApplication {
                properties(mapOf("grpc.port" to 0))
                modules(
                    serviceModule,
                    observabilityModule,
                    serverModule,
                )
            }
    }

    /**
     * Closes the Koin application context after each test execution.
     */
    @AfterEach
    fun tearDown() {
        application.close()
    }

    /**
     * Verifies that executing a successful RPC on the live server records metric timers tagged with service, method, and status.
     */
    @Test
    fun `production gRPC server records successful RPC metrics`() =
        runTest {
            val server = application.koin.get<GrpcServer>()
            val meterRegistry = application.koin.get<MeterRegistry>()

            server.start()

            val channel =
                ManagedChannelBuilder
                    .forAddress("localhost", server.getPort())
                    .usePlaintext()
                    .build()

            try {
                val stub =
                    AdminServiceGrpcKt
                        .AdminServiceCoroutineStub(channel)
                        .withDeadlineAfter(5, TimeUnit.SECONDS)

                val response =
                    stub.getServiceInfo(
                        ServiceInfoRequest.getDefaultInstance(),
                    )

                assertEquals("Admin", response.serviceName)

                val timer =
                    meterRegistry
                        .find("grpc.server")
                        .tag("rpc.service", "sorenkai.admin.v1.AdminService")
                        .tag("rpc.method", "GetServiceInfo")
                        .tag("grpc.status_code", "OK")
                        .timer()

                assertNotNull(timer)
                assertEquals(1L, timer!!.count())
            } finally {
                channel.shutdownNow()
                server.stop()
            }
        }
}
