package com.sorenkai.app

import com.sorenkai.admin.api.v1.AdminServiceGrpcKt
import com.sorenkai.app.service.AdminServiceImpl
import io.grpc.Server
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.HealthStatusManager
import io.grpc.protobuf.services.ProtoReflectionServiceV1
import java.util.concurrent.TimeUnit

class GrpcServer(
    private val port: Int,
) {
    private companion object {
        const val SHUTDOWN_TIMEOUT_SECONDS = 10L
    }

    private val healthManager = HealthStatusManager()
    private val server: Server =
        NettyServerBuilder
            .forPort(port)
            .addService(
                AdminServiceImpl(
                    RuntimeInfoProvider(),
                    DeploymentInfoProvider(),
                ),
            ).addService(healthManager.healthService)
            .addService(healthManager.healthService)
            .addService(ProtoReflectionServiceV1.newInstance())
            .build()

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

    fun stop() {
        healthManager.enterTerminalState()

        server.shutdown()
        if (!server.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            server.shutdownNow()
        }
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    fun getPort(): Int = server.port
}
