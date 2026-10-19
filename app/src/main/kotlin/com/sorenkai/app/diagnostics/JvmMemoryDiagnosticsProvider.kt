package com.sorenkai.app.diagnostics

import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean

class JvmMemoryDiagnosticsProvider(
    private val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean(),
) {
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
