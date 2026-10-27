package com.sorenkai.app

/**
 * Accessor for the application's runtime version metadata.
 *
 * Reads the project version packaged during build time from the `version.properties` classpath resource.
 */
object AppVersion {
    /**
     * The semver string of the running application (e.g., "1.0.0").
     *
     * @throws IllegalArgumentException if `version.properties` is missing from the classpath.
     */
    val value: String by lazy {
        requireNotNull(
            javaClass.classLoader.getResourceAsStream("version.properties"),
        ) {
            "Missing version.properties"
        }.use { stream ->
            java.util
                .Properties()
                .apply {
                    load(stream)
                }.getProperty("version")
        }
    }
}
