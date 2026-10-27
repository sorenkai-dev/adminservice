package com.sorenkai.app.diagnostics

/**
 * Diagnostic metrics snapshot for an individual JVM garbage collector.
 *
 * @property name The name of the garbage collector (e.g., "G1 Young Generation").
 * @property collectionCount The total number of collections that have occurred, or -1 if undefined.
 * @property collectionTimeMillis The approximate accumulated collection elapsed time in milliseconds, or -1 if undefined.
 */
data class JvmGarbageCollectorDiagnostics(
    val name: String,
    val collectionCount: Long,
    val collectionTimeMillis: Long,
)
