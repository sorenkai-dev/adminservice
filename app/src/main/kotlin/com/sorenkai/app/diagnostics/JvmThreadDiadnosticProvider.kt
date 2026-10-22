package com.sorenkai.app.diagnostics

import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean

class JvmThreadDiagnosticsProvider(
    private val threadBean: ThreadMXBean = ManagementFactory.getThreadMXBean(),
) {
    fun getInfo(): JvmThreadDiagnostics =
        JvmThreadDiagnostics(
            liveThreads = threadBean.threadCount,
            daemonThreads = threadBean.daemonThreadCount,
            peakThreads = threadBean.peakThreadCount,
            totalStartedThreads = threadBean.totalStartedThreadCount,
        )
}
