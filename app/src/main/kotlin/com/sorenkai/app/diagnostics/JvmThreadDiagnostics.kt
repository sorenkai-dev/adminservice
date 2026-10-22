package com.sorenkai.app.diagnostics

data class JvmThreadDiagnostics(
    val liveThreads: Int,
    val daemonThreads: Int,
    val peakThreads: Int,
    val totalStartedThreads: Long,
)
