package com.sorenkai.app

object AppVersion {
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
