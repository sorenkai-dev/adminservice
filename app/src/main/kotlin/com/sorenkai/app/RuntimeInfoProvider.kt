package com.sorenkai.app

import com.sorenkai.admin.api.v1.RuntimeInfo
import com.sorenkai.admin.api.v1.runtimeInfo
import io.grpc.netty.shaded.io.netty.util.NettyRuntime.availableProcessors
import io.grpc.netty.shaded.io.netty.util.internal.PlatformDependent.javaVersion
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.concurrent.TimeUnit

class RuntimeInfoProvider {
    private val runtime = ManagementFactory.getRuntimeMXBean()

    fun getInfo(): RuntimeInfo =
        runtimeInfo {
            startedAt = Instant.ofEpochMilli(runtime.startTime).toString()
            uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(runtime.uptime)
            javaVersion = System.getProperty("java.version")
            availableProcessors = Runtime.getRuntime().availableProcessors()
        }
}
