plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.appdistribution) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.grgit) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply true
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
