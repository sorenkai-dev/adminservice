package com.sorenkai.app.diagnostics

import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import kotlin.collections.map

/**
 * Provider for JVM garbage collection performance and execution diagnostics.
 *
 * Inspects all registered [GarbageCollectorMXBean] instances in the JVM to extract collection counts
 * and cumulative collection times.
 *
 * @param collectors The list of [GarbageCollectorMXBean] instances to query. Defaults to the platform MXBeans.
 */
class JvmGarbageCollectionDiagnosticsProvider(
    private val collectors: List<GarbageCollectorMXBean> =
        ManagementFactory.getGarbageCollectorMXBeans(),
) {
    /**
     * Gathers garbage collection diagnostics for all monitored garbage collectors.
     *
     * @return A list of [JvmGarbageCollectorDiagnostics] objects containing stats for each collector.
     */
    fun getInfo(): List<JvmGarbageCollectorDiagnostics> =
        collectors.map { collector ->
            JvmGarbageCollectorDiagnostics(
                name = collector.name,
                collectionCount = collector.collectionCount,
                collectionTimeMillis = collector.collectionTime,
            )
        }
}
