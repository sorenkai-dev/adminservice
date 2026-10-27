package com.sorenkai.app

import com.sorenkai.admin.api.v1.AdminServiceGrpcKt
import com.sorenkai.app.metrics.GrpcObservability
import com.sorenkai.app.service.AdminServiceImpl
import io.grpc.Server
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.HealthStatusManager
import io.grpc.protobuf.services.ProtoReflectionServiceV1
import java.util.concurrent.TimeUnit

/**
 * Netty-based gRPC server hosting the Admin Service.
 *
 * Configures service endpoints, registers standard gRPC health checks and reflection services,
 * and attaches Micrometer observability interceptors for request metrics and latency tracking.
 *
 * @param port The TCP port to bind the server to (0 for dynamic port allocation).
 * @param adminService The administrative gRPC service implementation handling RPC calls.
 * @param observability The observability configuration providing metrics interceptors.
 */
class GrpcServer(
    port: Int,
    adminService: AdminServiceImpl,
    observability: GrpcObservability,
) {
    private companion object {
        const val SHUTDOWN_TIMEOUT_SECONDS = 10L
    }

    private val healthManager = HealthStatusManager()

    private val server: Server =
        NettyServerBuilder
            .forPort(port)
            .addService(adminService)
            .addService(healthManager.healthService)
            .addService(ProtoReflectionServiceV1.newInstance())
            .intercept(observability.serverInterceptor)
            .build()

    /**
     * Starts the Netty gRPC server and registers the Admin service as [ServingStatus.SERVING]
     * in the gRPC health manager.
     */
    fun start() {
        server.start()

        healthManager.setStatus(
            AdminServiceGrpcKt.SERVICE_NAME,
            ServingStatus.SERVING,
        )
        println(
            "Admin health: " +
                healthManager.healthService
                    .bindService()
                    .serviceDescriptor.name,
        )

        println("Admin gRPC server listening on port ${server.port}")
    }

    /**
     * Initiates a graceful shutdown of the gRPC server.
     *
     * Transitions the health status manager to terminal state and waits up to 10 seconds
     * for active requests to complete before forcing immediate termination.
     */
    fun stop() {
        healthManager.enterTerminalState()

        server.shutdown()
        if (!server.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            server.shutdownNow()
        }
    }

    /**
     * Blocks the current thread until the gRPC server has completely terminated.
     */
    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    /**
     * Retrieves the network port on which the server is currently listening.
     *
     * @return The active listening port number.
     */
    fun getPort(): Int = server.port
}
