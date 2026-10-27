package com.sorenkai.app.service

import com.sorenkai.admin.api.v1.AdminServiceGrpcKt
import com.sorenkai.admin.api.v1.DiagnosticsRequest
import com.sorenkai.app.DeploymentInfoProvider
import com.sorenkai.app.RuntimeInfoProvider
import com.sorenkai.app.diagnostics.JvmGarbageCollectionDiagnosticsProvider
import com.sorenkai.app.diagnostics.JvmGarbageCollectorDiagnostics
import com.sorenkai.app.diagnostics.JvmMemoryDiagnostics
import com.sorenkai.app.diagnostics.JvmMemoryDiagnosticsProvider
import com.sorenkai.app.diagnostics.JvmThreadDiagnostics
import com.sorenkai.app.diagnostics.JvmThreadDiagnosticsProvider
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * In-process integration tests validating the `GetDiagnostics` gRPC endpoint in [AdminServiceImpl].
 */
class AdminServiceDiagnosticsTest {
    private suspend fun withGrpcClient(
        service: AdminServiceImpl,
        block: suspend (AdminServiceGrpcKt.AdminServiceCoroutineStub) -> Unit,
    ) {
        val serverName = InProcessServerBuilder.generateName()

        val server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(service)
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

    /**
     * Verifies that `GetDiagnostics` returns non-empty memory and thread telemetry when using live platform providers.
     */
    @Test
    fun `GetDiagnostics returns JVM memory and thread information`() =
        runTest {
            val service =
                AdminServiceImpl(
                    RuntimeInfoProvider(),
                    DeploymentInfoProvider(),
                )

            withGrpcClient(service) { stub ->
                val response =
                    stub.getDiagnostics(
                        DiagnosticsRequest.getDefaultInstance(),
                    )

                assertTrue(response.hasMemory())
                assertTrue(response.hasThreads())
                assertTrue(response.memory.heapCommittedBytes >= response.memory.heapUsedBytes)
                assertTrue(response.threads.liveThreads > 0)
            }
        }

    /**
     * Verifies that `GetDiagnostics` accurately maps memory, thread, and GC metrics from mocked providers into Protobuf messages.
     */
    @Test
    fun `GetDiagnostics maps JVM measurements correctly`() =
        runTest {
            val memoryProvider = mockk<JvmMemoryDiagnosticsProvider>()
            val threadProvider = mockk<JvmThreadDiagnosticsProvider>()
            val gcProvider = mockk<JvmGarbageCollectionDiagnosticsProvider>()

            every { memoryProvider.getInfo() } returns
                JvmMemoryDiagnostics(
                    heapUsedBytes = 128_000_000,
                    heapCommittedBytes = 256_000_000,
                    heapMaxBytes = 512_000_000,
                    nonHeapUsedBytes = 32_000_000,
                    nonHeapCommittedBytes = 64_000_000,
                    nonHeapMaxBytes = -1,
                )

            every { threadProvider.getInfo() } returns
                JvmThreadDiagnostics(
                    liveThreads = 24,
                    daemonThreads = 18,
                    peakThreads = 32,
                    totalStartedThreads = 150,
                )

            every { gcProvider.getInfo() } returns
                listOf(
                    JvmGarbageCollectorDiagnostics(
                        name = "Young Collector",
                        collectionCount = 125,
                        collectionTimeMillis = 840,
                    ),
                    JvmGarbageCollectorDiagnostics(
                        name = "Old Collector",
                        collectionCount = -1,
                        collectionTimeMillis = -1,
                    ),
                )

            val service =
                AdminServiceImpl(
                    runtimeInfoProvider = RuntimeInfoProvider(),
                    deploymentInfoProvider = DeploymentInfoProvider(),
                    memoryProvider = memoryProvider,
                    threadProvider = threadProvider,
                    garbageCollectionProvider = gcProvider,
                )

            withGrpcClient(service) { stub ->
                val response =
                    stub.getDiagnostics(
                        DiagnosticsRequest.getDefaultInstance(),
                    )

                // Memory measurements
                assertTrue(response.hasMemory())
                assertEquals(128_000_000L, response.memory.heapUsedBytes)
                assertEquals(256_000_000L, response.memory.heapCommittedBytes)
                assertEquals(512_000_000L, response.memory.heapMaxBytes)
                assertEquals(32_000_000L, response.memory.nonHeapUsedBytes)
                assertEquals(64_000_000L, response.memory.nonHeapCommittedBytes)
                assertEquals(-1L, response.memory.nonHeapMaxBytes)

                // Thread measurements
                assertTrue(response.hasThreads())
                assertEquals(24, response.threads.liveThreads)
                assertEquals(18, response.threads.daemonThreads)
                assertEquals(32, response.threads.peakThreads)
                assertEquals(150L, response.threads.totalStartedThreads)

                // Garbage collection measurements
                assertEquals(2, response.garbageCollectorsCount)

                val young = response.getGarbageCollectors(0)
                assertEquals("Young Collector", young.name)
                assertEquals(125L, young.collectionCount)
                assertEquals(840L, young.collectionTimeMillis)

                val old = response.getGarbageCollectors(1)
                assertEquals("Old Collector", old.name)
                assertEquals(-1L, old.collectionCount)
                assertEquals(-1L, old.collectionTimeMillis)
            }
        }
}
