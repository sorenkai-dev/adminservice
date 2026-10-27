package com.sorenkai.app.metrics

import io.grpc.ServerInterceptor
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler
import io.micrometer.observation.ObservationRegistry

/**
 * Observability configuration for gRPC server metrics and distributed tracing observations.
 *
 * Configures a [DefaultMeterObservationHandler] on the given [ObservationRegistry] to export
 * timer and counter metrics to the [MeterRegistry], and produces an [ObservationGrpcServerInterceptor]
 * for intercepting incoming gRPC calls.
 *
 * @param observationRegistry The registry managing contextual observations and handlers.
 * @param meterRegistry The registry managing micrometer meters and metric exports.
 */
class GrpcObservability(
    observationRegistry: ObservationRegistry,
    meterRegistry: MeterRegistry,
) {
    /**
     * gRPC [ServerInterceptor] that measures call durations, method names, and status codes.
     */
    val serverInterceptor: ServerInterceptor =
        ObservationGrpcServerInterceptor(observationRegistry)

    init {
        observationRegistry
            .observationConfig()
            .observationHandler(
                DefaultMeterObservationHandler
                    .builder(meterRegistry)
                    .build(),
            )
    }
}
