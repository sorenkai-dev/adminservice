package com.sorenkai.app.diagnostics

import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean

/**
 * Provider for JVM heap and non-heap memory diagnostics.
 *
 * Queries the platform [MemoryMXBean] to retrieve current memory utilization and limit metrics.
 *
 * @param memoryBean The [MemoryMXBean] instance used to inspect JVM memory usage. Defaults to platform bean.
 */
class JvmMemoryDiagnosticsProvider(
    private val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean(),
) {
    /**
     * Retrieves current JVM heap and non-heap memory diagnostic metrics.
     *
     * @return A [JvmMemoryDiagnostics] instance containing used, committed, and max bytes.
     */
    fun getInfo(): JvmMemoryDiagnostics {
        val heap = memoryBean.heapMemoryUsage
        val nonHeap = memoryBean.nonHeapMemoryUsage

        return JvmMemoryDiagnostics(
            heapUsedBytes = heap.used,
            heapCommittedBytes = heap.committed,
            heapMaxBytes = heap.max,
            nonHeapUsedBytes = nonHeap.used,
            nonHeapCommittedBytes = nonHeap.committed,
            nonHeapMaxBytes = nonHeap.max,
        )
    }
}
