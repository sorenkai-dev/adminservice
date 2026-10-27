package com.sorenkai.app.diagnostics

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryUsage

/**
 * Unit tests verifying [JvmMemoryDiagnosticsProvider] memory extraction logic.
 */
class JvmMemoryDiagnosticsTest {
    /**
     * Verifies that heap and non-heap used, committed, and max byte measurements are accurately mapped.
     */
    @Test
    fun `reports heap and non-heap memory in bytes`() {
        val memoryBean = mockk<MemoryMXBean>()

        every { memoryBean.heapMemoryUsage } returns
            MemoryUsage(0, 128_000_000, 256_000_000, 512_000_000)

        every { memoryBean.nonHeapMemoryUsage } returns
            MemoryUsage(0, 32_000_000, 64_000_000, -1)

        val provider = JvmMemoryDiagnosticsProvider(memoryBean)
        val diagnostics = provider.getInfo()

        assertEquals(128_000_000, diagnostics.heapUsedBytes)
        assertEquals(256_000_000, diagnostics.heapCommittedBytes)
        assertEquals(512_000_000, diagnostics.heapMaxBytes)

        assertEquals(32_000_000, diagnostics.nonHeapUsedBytes)
        assertEquals(64_000_000, diagnostics.nonHeapCommittedBytes)
        assertEquals(-1, diagnostics.nonHeapMaxBytes)
    }
}
