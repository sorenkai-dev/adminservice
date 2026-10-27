package com.sorenkai.app.diagnostics

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.management.ThreadMXBean

/**
 * Unit tests verifying [JvmThreadDiagnosticsProvider] thread metric extraction logic.
 */
class JvmThreadDiagnosticsTest {
    /**
     * Verifies that live, daemon, peak, and total started thread statistics are accurately mapped.
     */
    @Test
    fun `reports JVM thread statistics`() {
        val threadBean = mockk<ThreadMXBean>()

        every { threadBean.threadCount } returns 24
        every { threadBean.daemonThreadCount } returns 18
        every { threadBean.peakThreadCount } returns 32
        every { threadBean.totalStartedThreadCount } returns 150L

        val provider = JvmThreadDiagnosticsProvider(threadBean)
        val diagnostics = provider.getInfo()

        assertEquals(24, diagnostics.liveThreads)
        assertEquals(18, diagnostics.daemonThreads)
        assertEquals(32, diagnostics.peakThreads)
        assertEquals(150L, diagnostics.totalStartedThreads)
    }
}
