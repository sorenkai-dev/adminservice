package com.sorenkai.app.diagnostics

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.management.GarbageCollectorMXBean

/**
 * Unit tests verifying [JvmGarbageCollectionDiagnosticsProvider] mapping from [GarbageCollectorMXBean].
 */
class JvmGarbageCollectionDiagnosticsTest {
    /**
     * Verifies that stats from multiple collectors (e.g., Young and Old generations) are correctly mapped.
     */
    @Test
    fun `reports statistics for each garbage collector`() {
        val youngCollector = mockk<GarbageCollectorMXBean>()
        val oldCollector = mockk<GarbageCollectorMXBean>()

        every { youngCollector.name } returns "G1 Young Generation"
        every { youngCollector.collectionCount } returns 125L
        every { youngCollector.collectionTime } returns 840L

        every { oldCollector.name } returns "G1 Old Generation"
        every { oldCollector.collectionCount } returns 3L
        every { oldCollector.collectionTime } returns 210L

        val provider =
            JvmGarbageCollectionDiagnosticsProvider(
                listOf(youngCollector, oldCollector),
            )

        val diagnostics = provider.getInfo()

        assertEquals(2, diagnostics.size)

        assertEquals("G1 Young Generation", diagnostics[0].name)
        assertEquals(125L, diagnostics[0].collectionCount)
        assertEquals(840L, diagnostics[0].collectionTimeMillis)

        assertEquals("G1 Old Generation", diagnostics[1].name)
        assertEquals(3L, diagnostics[1].collectionCount)
        assertEquals(210L, diagnostics[1].collectionTimeMillis)
    }

    /**
     * Verifies that undefined statistics (e.g. -1 values) returned by MXBeans are preserved.
     */
    @Test
    fun `preserves undefined collection statistics`() {
        val collector = mockk<GarbageCollectorMXBean>()

        every { collector.name } returns "Unknown Collector"
        every { collector.collectionCount } returns -1L
        every { collector.collectionTime } returns -1L

        val provider =
            JvmGarbageCollectionDiagnosticsProvider(listOf(collector))

        val diagnostics = provider.getInfo()

        assertEquals(-1L, diagnostics.single().collectionCount)
        assertEquals(-1L, diagnostics.single().collectionTimeMillis)
    }

    /**
     * Verifies that an empty list is returned when no garbage collectors are registered in the JVM.
     */
    @Test
    fun `returns empty list when no collectors are available`() {
        val provider = JvmGarbageCollectionDiagnosticsProvider(emptyList())

        assertEquals(emptyList<JvmGarbageCollectorDiagnostics>(), provider.getInfo())
    }
}
