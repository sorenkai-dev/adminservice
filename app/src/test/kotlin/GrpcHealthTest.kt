
package com.sorenkai.app

import io.grpc.ManagedChannelBuilder
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.health.v1.HealthGrpc
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.protobuf.services.HealthStatusManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class GrpcHealthTest {
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

    @Test
    fun `netty health service returns SERVING`() {
        val grpcServer = GrpcServer(0)
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
