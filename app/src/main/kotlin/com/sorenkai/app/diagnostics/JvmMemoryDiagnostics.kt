package com.sorenkai.app.diagnostics

data class JvmMemoryDiagnostics(
    val heapUsedBytes: Long,
    val heapCommittedBytes: Long,
    val heapMaxBytes: Long,
    val nonHeapUsedBytes: Long,
    val nonHeapCommittedBytes: Long,
    val nonHeapMaxBytes: Long,
)
