plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":api"))
    implementation(project(":persistence"))
    testImplementation(kotlin("test"))
}

application {
    mainClass = "com.sorenkai.app.AppKt"
}
