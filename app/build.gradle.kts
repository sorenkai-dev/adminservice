import sun.jvmstat.monitor.MonitoredVmUtil.mainClass

plugins {
    application
}

dependencies {

    // Koin
    implementation(platform(libs.koinBom))
    implementation(libs.koinCore)

    // Modules
    implementation(project(":core"))
    implementation(project(":api"))
    implementation(project(":persistence"))

    // gRPC
    api(libs.grpcKotlinStub)
    implementation(libs.grpcNetty)
    implementation(libs.grpcServices)

    // Protobuf
    implementation(libs.protobufJava)

    // Monitoring
    implementation(platform(libs.micrometerBom))
    implementation(libs.micrometerCore)

    // Testing
    testImplementation(kotlin("test"))
//    testImplementation(platform(libs.koinBom))
    testImplementation(libs.koinTest)
    testImplementation(libs.koinTestJunit5)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.grpcInprocess)
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testImplementation(libs.mockk)
    testImplementation(libs.micrometer.test)
}

application {
    mainClass = "com.sorenkai.app.AppKt"
}

abstract class GenerateVersionInfo : DefaultTask() {
    @get:Input
    abstract val appVersion: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText("version=${appVersion.get()}\n")
    }
}

val generateVersionInfo =
    tasks.register<GenerateVersionInfo>("generateVersionInfo") {
        appVersion.set(project.version.toString())
        outputFile.set(layout.buildDirectory.file("generated/resources/version/version.properties"))
    }

tasks.named<ProcessResources>("processResources") {
    from(generateVersionInfo)
}
