plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    id("com.google.devtools.ksp") version "1.9.21-1.0.15" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
    id("com.google.firebase.appdistribution") version "4.0.1" apply false
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

/**
 * Load key-values from "production.env" properties file into extension properties (ext)
 */
task("loadProductionEnv") {
    if(!project.hasProperty("SKIP_PRODUCTION_ENV")) {
        val env = rootProject.file("production.env")

        if (env.exists()) {
            env.forEachLine {
                val keyValuePair = it.split("=")
                if (keyValuePair.size > 1) {
                    project.ext.set(keyValuePair[0], keyValuePair[1])
                }
            }
        } else {
            throw GradleException(
                "`production.env` not found. " +
                    "Please follow the \"building\" section on the README or the CONTRIBUTING guide" +
                    " and use the respective template to create `production.env`."
            )
        }
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
    gradleVersion = "8.5"
    distributionType = Wrapper.DistributionType.ALL
}
