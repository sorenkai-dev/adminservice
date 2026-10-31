dependencies {
    implementation(project(":core"))
    api(libs.bundles.postgresqlPersistence)
    implementation(libs.bundles.kotlinxEcosystem)

    // JetBrains Exposed
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)

    // Koin
    implementation(platform(libs.koinBom))
    implementation(libs.koinCore)

    testImplementation(kotlin("test"))
    testImplementation(libs.junitJupiter)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation("io.zonky.test:embedded-postgres:2.1.0")
}
