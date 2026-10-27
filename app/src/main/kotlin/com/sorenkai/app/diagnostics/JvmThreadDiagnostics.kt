package com.sorenkai.app.diagnostics

/**
 * Diagnostic metrics snapshot of JVM thread execution and allocation state.
 *
 * @property liveThreads The current number of live threads including both daemon and non-daemon threads.
 * @property daemonThreads The current number of live daemon threads.
 * @property peakThreads The peak live thread count since the Java virtual machine started or peak was reset.
 * @property totalStartedThreads The total number of threads created and also started since the JVM started.
 */
data class JvmThreadDiagnostics(
    val liveThreads: Int,
    val daemonThreads: Int,
    val peakThreads: Int,
    val totalStartedThreads: Long,
)
