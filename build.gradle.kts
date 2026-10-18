import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
}

subprojects {
    group = "com.sorenkai"
    version = "1.0.0"


    pluginManager.apply("org.jetbrains.kotlin.jvm")
    pluginManager.apply("com.diffplug.spotless")
    pluginManager.apply("dev.detekt")


    configure<KotlinJvmProjectExtension> {
        jvmToolchain(27)

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_26)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(26)
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**")
            ktlint()
        }

        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
    }

    plugins.withId("dev.detekt") {
        extensions.configure<DetektExtension> {
            // Keep using the root config
            // Only analyze main Kotlin sources for each module
            source.setFrom(files("src/main/kotlin"))
            config.setFrom(files("$rootDir/detekt.yml"))
            buildUponDefaultConfig = true
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events(
                TestLogEvent.FAILED,
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED
            )
        }
    }
}
