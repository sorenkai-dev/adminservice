package com.sorenkai.app.com.sorenkai.app

import com.sorenkai.app.DeploymentInfoProvider
import com.sorenkai.app.GrpcServer
import com.sorenkai.app.RuntimeInfoProvider
import com.sorenkai.app.metrics.GrpcObservability
import com.sorenkai.app.service.AdminServiceImpl
import io.grpc.ManagedChannelBuilder
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.health.v1.HealthGrpc
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.protobuf.services.HealthStatusManager
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * Integration tests verifying standard gRPC health checking functionality.
 *
 * Validates that both in-process and Netty gRPC server instances correctly bind the official
 * gRPC health protocol and report [ServingStatus.SERVING] for the Admin service.
 */
class GrpcHealthTest {
    /**
     * Verifies that the gRPC health status manager returns [ServingStatus.SERVING] when queried over an in-process channel.
     */
    @Test
    fun `health service returns SERVING`() {
        val manager = HealthStatusManager()
        val serverName = InProcessServerBuilder.generateName()

        val server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(manager.healthService)
                .build()
                .start()

        val channel =
            InProcessChannelBuilder
                .forName(serverName)
                .directExecutor()
                .build()

        try {
            val request =
                HealthCheckRequest
                    .newBuilder()
                    .setService("")
                    .build()

            val response =
                HealthGrpc
                    .newBlockingStub(channel)
                    .check(request)

            assertEquals(ServingStatus.SERVING, response.status)
        } finally {
            channel.shutdownNow()
            server.shutdownNow()
        }
    }

    /**
     * Verifies that the Netty [GrpcServer] instance binds and exposes the health service on its dynamic TCP port.
     */
    @Test
    fun `netty health service returns SERVING`() {
        val meterRegistry = SimpleMeterRegistry()
        val observability =
            GrpcObservability(
                observationRegistry = ObservationRegistry.create(),
                meterRegistry = meterRegistry,
            )

        val grpcServer =
            GrpcServer(
                port = 0,
                adminService =
                    AdminServiceImpl(
                        RuntimeInfoProvider(),
                        DeploymentInfoProvider(),
                    ),
                observability = observability,
            )
        grpcServer.start()

        val channel =
            ManagedChannelBuilder
                .forAddress("localhost", grpcServer.getPort())
                .usePlaintext()
                .build()

        try {
            val request =
                HealthCheckRequest
                    .newBuilder()
                    .setService("sorenkai.admin.v1.AdminService")
                    .build()

            val response =
                HealthGrpc
                    .newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .check(request)

            assertEquals(ServingStatus.SERVING, response.status)
        } finally {
            channel.shutdownNow()
            grpcServer.stop()
        }
    }
}
