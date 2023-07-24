plugins {
    id("com.android.application") version "8.0.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.devtools.ksp") version "1.9.0-1.0.12" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
    id("com.google.firebase.crashlytics") version "2.9.7" apply false
    id("com.google.firebase.appdistribution") version "4.0.0" apply false
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.0"
}

/**
 * Load key-values from ".env" properties file into extension properties (ext)
 */
task("loadEnv") {
    val envInfo = mutableMapOf<String, String>()

    val env = rootProject.file(".env")

    if (env.exists()) {
        env.forEachLine {
            val keyValuePair = it.split("=")
            if (keyValuePair.size > 1) {
                envInfo[keyValuePair[0]] = keyValuePair[1]
            }
        }
    }

    envInfo.forEach {
        project.ext.set(it.key, it.value)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()

        maven {
            authentication {
                create<BasicAuthentication>("basic")
            }
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = project.property("MAPBOX_DOWNLOADS_TOKEN") as String
            }
        }
        maven("https://jitpack.io")
    }

    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        config.setFrom(rootProject.files("config/detekt.yml"))
        buildUponDefaultConfig = true
    }
}

task("clean") {
    delete(rootProject.buildDir)
}

/**
 * Configures gradle wrapper for this project
 */
tasks.wrapper {
    gradleVersion = "8.2.1"
    distributionType = Wrapper.DistributionType.ALL
}
