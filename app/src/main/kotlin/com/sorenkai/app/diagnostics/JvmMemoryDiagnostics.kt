package com.sorenkai.app.diagnostics

/**
 * Diagnostic metrics snapshot of JVM heap and non-heap memory utilization.
 *
 * @property heapUsedBytes The amount of used heap memory in bytes.
 * @property heapCommittedBytes The amount of committed heap memory in bytes.
 * @property heapMaxBytes The maximum amount of heap memory in bytes, or -1 if undefined.
 * @property nonHeapUsedBytes The amount of used non-heap memory in bytes.
 * @property nonHeapCommittedBytes The amount of committed non-heap memory in bytes.
 * @property nonHeapMaxBytes The maximum amount of non-heap memory in bytes, or -1 if undefined.
 */
data class JvmMemoryDiagnostics(
    val heapUsedBytes: Long,
    val heapCommittedBytes: Long,
    val heapMaxBytes: Long,
    val nonHeapUsedBytes: Long,
    val nonHeapCommittedBytes: Long,
    val nonHeapMaxBytes: Long,
)
