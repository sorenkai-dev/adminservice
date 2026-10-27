package com.sorenkai.app.service

import com.sorenkai.admin.api.v1.AdminServiceGrpcKt
import com.sorenkai.admin.api.v1.DiagnosticsRequest
import com.sorenkai.admin.api.v1.DiagnosticsResponse
import com.sorenkai.admin.api.v1.ServiceInfoRequest
import com.sorenkai.admin.api.v1.ServiceInfoResponse
import com.sorenkai.admin.api.v1.diagnosticsResponse
import com.sorenkai.admin.api.v1.garbageCollectorInfo
import com.sorenkai.admin.api.v1.jvmMemoryInfo
import com.sorenkai.admin.api.v1.jvmThreadInfo
import com.sorenkai.admin.api.v1.serviceInfoResponse
import com.sorenkai.app.AppVersion
import com.sorenkai.app.DeploymentInfoProvider
import com.sorenkai.app.RuntimeInfoProvider
import com.sorenkai.app.diagnostics.JvmGarbageCollectionDiagnosticsProvider
import com.sorenkai.app.diagnostics.JvmMemoryDiagnosticsProvider
import com.sorenkai.app.diagnostics.JvmThreadDiagnosticsProvider

/**
 * Implementation of the gRPC Admin Service defined in `admin.proto`.
 *
 * Provides RPC endpoints for querying administrative status, application runtime and deployment
 * metadata, and real-time JVM diagnostic metrics (memory, threads, and garbage collection).
 *
 * @param runtimeInfoProvider Provider for JVM runtime metrics and system specifications.
 * @param deploymentInfoProvider Provider for environment and instance deployment metadata.
 * @param memoryProvider Provider for JVM heap and non-heap memory diagnostics.
 * @param threadProvider Provider for JVM thread state diagnostics.
 * @param garbageCollectionProvider Provider for JVM garbage collection diagnostics.
 */
class AdminServiceImpl(
    private val runtimeInfoProvider: RuntimeInfoProvider,
    private val deploymentInfoProvider: DeploymentInfoProvider,
    private val memoryProvider: JvmMemoryDiagnosticsProvider = JvmMemoryDiagnosticsProvider(),
    private val threadProvider: JvmThreadDiagnosticsProvider = JvmThreadDiagnosticsProvider(),
    private val garbageCollectionProvider: JvmGarbageCollectionDiagnosticsProvider =
        JvmGarbageCollectionDiagnosticsProvider(),
) : AdminServiceGrpcKt.AdminServiceCoroutineImplBase() {
    /**
     * Handles the `GetServiceInfo` RPC request.
     *
     * @param request The [ServiceInfoRequest] payload.
     * @return A [ServiceInfoResponse] containing service name, application version, runtime stats,
     * and deployment environment information.
     */
    override suspend fun getServiceInfo(request: ServiceInfoRequest): ServiceInfoResponse =
        serviceInfoResponse {
            serviceName = "Admin"
            version = AppVersion.value
            runtime = runtimeInfoProvider.getInfo()
            deployment = deploymentInfoProvider.getInfo()
        }

    /**
     * Handles the `GetDiagnostics` RPC request.
     *
     * @param request The [DiagnosticsRequest] payload.
     * @return A [DiagnosticsResponse] containing JVM memory usage, thread statistics,
     * and garbage collector performance metrics.
     */
    override suspend fun getDiagnostics(request: DiagnosticsRequest): DiagnosticsResponse {
        val memoryInfo = memoryProvider.getInfo()
        val threadInfo = threadProvider.getInfo()
        val gcInfo = garbageCollectionProvider.getInfo()

        return diagnosticsResponse {
            memory =
                jvmMemoryInfo {
                    heapUsedBytes = memoryInfo.heapUsedBytes
                    heapCommittedBytes = memoryInfo.heapCommittedBytes
                    heapMaxBytes = memoryInfo.heapMaxBytes
                    nonHeapUsedBytes = memoryInfo.nonHeapUsedBytes
                    nonHeapCommittedBytes = memoryInfo.nonHeapCommittedBytes
                    nonHeapMaxBytes = memoryInfo.nonHeapMaxBytes
                }

            threads =
                jvmThreadInfo {
                    liveThreads = threadInfo.liveThreads
                    daemonThreads = threadInfo.daemonThreads
                    peakThreads = threadInfo.peakThreads
                    totalStartedThreads = threadInfo.totalStartedThreads
                }

            gcInfo.forEach { collector ->
                garbageCollectors +=
                    garbageCollectorInfo {
                        name = collector.name
                        collectionCount = collector.collectionCount
                        collectionTimeMillis = collector.collectionTimeMillis
                    }
            }
        }
    }
}
