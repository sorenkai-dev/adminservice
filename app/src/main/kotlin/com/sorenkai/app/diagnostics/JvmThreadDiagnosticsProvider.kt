package com.sorenkai.app.diagnostics

import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean

/**
 * Provider for JVM thread utilization and lifecycle metrics.
 *
 * Inspects the platform [ThreadMXBean] to retrieve counts of live threads, daemon threads, peak threads,
 * and total started threads.
 *
 * @param threadBean The [ThreadMXBean] instance used to gather thread stats. Defaults to the platform bean.
 */
class JvmThreadDiagnosticsProvider(
    private val threadBean: ThreadMXBean = ManagementFactory.getThreadMXBean(),
) {
    /**
     * Retrieves current JVM thread diagnostics.
     *
     * @return A [JvmThreadDiagnostics] object containing live, daemon, peak, and total started thread counts.
     */
    fun getInfo(): JvmThreadDiagnostics =
        JvmThreadDiagnostics(
            liveThreads = threadBean.threadCount,
            daemonThreads = threadBean.daemonThreadCount,
            peakThreads = threadBean.peakThreadCount,
            totalStartedThreads = threadBean.totalStartedThreadCount,
        )
}
