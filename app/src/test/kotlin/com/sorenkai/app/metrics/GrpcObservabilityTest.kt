package com.sorenkai.app.metrics

import com.sorenkai.admin.api.v1.AdminServiceGrpcKt
import com.sorenkai.admin.api.v1.DiagnosticsRequest
import com.sorenkai.admin.api.v1.ServiceInfoRequest
import com.sorenkai.admin.api.v1.ServiceInfoResponse
import com.sorenkai.admin.api.v1.serviceInfoResponse
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.tck.TestObservationRegistry
import io.micrometer.observation.tck.TestObservationRegistryAssert
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

/**
 * Unit and in-process integration tests verifying Micrometer observation and timer metrics
 * produced by [GrpcObservability].
 */
class GrpcObservabilityTest {
    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var observationRegistry: TestObservationRegistry
    private lateinit var observability: GrpcObservability

    /**
     * Initializes test observation and meter registries before each test execution.
     */
    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        observationRegistry = TestObservationRegistry.create()

        observability =
            GrpcObservability(
                observationRegistry = observationRegistry,
                meterRegistry = meterRegistry,
            )
    }

    /**
     * Cleans up and closes the meter registry after each test run.
     */
    @AfterEach
    fun tearDown() {
        meterRegistry.close()
    }

    /**
     * Verifies that a successful gRPC call records an observation and meter timer with `OK` status code.
     */
    @Test
    fun `successful RPC produces observation and timer`() =
        runTest {
            val service =
                object : AdminServiceGrpcKt.AdminServiceCoroutineImplBase() {
                    override suspend fun getServiceInfo(request: ServiceInfoRequest): ServiceInfoResponse =
                        serviceInfoResponse {
                            serviceName = "Admin"
                            version = "1.0.0"
                        }
                }

            withGrpcClient(service) { stub ->
                val response =
                    stub.getServiceInfo(
                        ServiceInfoRequest.getDefaultInstance(),
                    )

                assertEquals("Admin", response.serviceName)
            }

            TestObservationRegistryAssert
                .assertThat(observationRegistry)
                .hasAnObservation { observation ->
                    observation
                        .hasNameEqualTo("grpc.server")
                        .hasLowCardinalityKeyValue(
                            "rpc.service",
                            "sorenkai.admin.v1.AdminService",
                        ).hasLowCardinalityKeyValue(
                            "rpc.method",
                            "GetServiceInfo",
                        ).hasLowCardinalityKeyValue(
                            "grpc.status_code",
                            "OK",
                        )
                }

            val timer =
                meterRegistry
                    .find("grpc.server")
                    .tag("rpc.service", "sorenkai.admin.v1.AdminService")
                    .tag("rpc.method", "GetServiceInfo")
                    .tag("grpc.status_code", "OK")
                    .timer()

            assertNotNull(timer)
            assertEquals(1L, timer!!.count())
            assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) >= 0.0)
        }

    /**
     * Verifies that a failed or unimplemented RPC call records an observation and meter timer reflecting the actual status code.
     */
    @Test
    fun `failed RPC produces observation and timer with actual status`() =
        runTest {
            val service =
                object : AdminServiceGrpcKt.AdminServiceCoroutineImplBase() {}

            withGrpcClient(service) { stub ->
                val exception =
                    assertFailsWith<StatusException> {
                        stub.getDiagnostics(
                            DiagnosticsRequest.getDefaultInstance(),
                        )
                    }

                assertEquals(
                    Status.Code.UNIMPLEMENTED,
                    exception.status.code,
                )
            }

            TestObservationRegistryAssert
                .assertThat(observationRegistry)
                .hasAnObservation { observation ->
                    observation
                        .hasNameEqualTo("grpc.server")
                        .hasLowCardinalityKeyValue(
                            "rpc.service",
                            "sorenkai.admin.v1.AdminService",
                        ).hasLowCardinalityKeyValue(
                            "rpc.method",
                            "GetDiagnostics",
                        ).hasLowCardinalityKeyValue(
                            "grpc.status_code",
                            "UNIMPLEMENTED",
                        )
                }

            val timer =
                meterRegistry
                    .find("grpc.server")
                    .tag("rpc.service", "sorenkai.admin.v1.AdminService")
                    .tag("rpc.method", "GetDiagnostics")
                    .tag("grpc.status_code", "UNIMPLEMENTED")
                    .timer()

            assertNotNull(timer)
            assertEquals(1L, timer!!.count())
        }

    private suspend fun withGrpcClient(
        service: AdminServiceGrpcKt.AdminServiceCoroutineImplBase,
        block: suspend (AdminServiceGrpcKt.AdminServiceCoroutineStub) -> Unit,
    ) {
        val serverName = InProcessServerBuilder.generateName()

        val server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(service)
                .intercept(observability.serverInterceptor)
                .build()
                .start()

        val channel =
            InProcessChannelBuilder
                .forName(serverName)
                .directExecutor()
                .build()

        try {
            val stub =
                AdminServiceGrpcKt
                    .AdminServiceCoroutineStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS)

            block(stub)
        } finally {
            channel.shutdownNow()
            server.shutdownNow()
        }
    }
}
