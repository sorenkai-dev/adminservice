package com.sorenkai.app

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val grpcServer = GrpcServer(port)

    Runtime.getRuntime().addShutdownHook(
        Thread {
            grpcServer.stop()
        },
    )

    grpcServer.start()
    grpcServer.blockUntilShutdown()
}
