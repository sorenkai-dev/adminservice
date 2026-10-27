package com.sorenkai.app

import com.sorenkai.admin.api.v1.RuntimeInfo
import com.sorenkai.admin.api.v1.runtimeInfo
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Provider for JVM runtime metrics and system specifications.
 *
 * Uses the Java Management [java.lang.management.RuntimeMXBean] to gather process uptime, start time,
 * active Java version, and available CPU core count.
 */
class RuntimeInfoProvider {
    private val runtime = ManagementFactory.getRuntimeMXBean()

    /**
     * Resolves and returns current JVM runtime metrics.
     *
     * @return A [RuntimeInfo] protobuf message containing process start timestamp (ISO-8601),
     * uptime in seconds, runtime Java version, and available processor cores.
     */
    fun getInfo(): RuntimeInfo =
        runtimeInfo {
            startedAt = Instant.ofEpochMilli(runtime.startTime).toString()
            uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(runtime.uptime)
            javaVersion = System.getProperty("java.version")
            availableProcessors = Runtime.getRuntime().availableProcessors()
        }
}
