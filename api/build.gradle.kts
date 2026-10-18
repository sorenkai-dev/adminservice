plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

dependencies {
    // gRPC
    implementation(libs.grpcStub)
    implementation(libs.grpcProtobuf)
    implementation(libs.grpcKotlinStub)

    // Protobuf
    implementation(libs.protobufJava)
    implementation(libs.protobufKotlin)

    // Testing
    testImplementation(kotlin("test"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpcKotlin.get()}:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().configureEach {
            builtins {
                create("kotlin")
            }
            plugins {
                create("grpc")
                create("grpckt")
            }
        }
    }
}
