import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.appdistribution")
    id("com.google.firebase.firebase-perf")
}

fun getGitCommitHash(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

fun getStringProperty(name: String): String {
    return project.property(name) as String
}

fun getFlavorProperty(propertyName: String, flavorEnvFile: String): String {
    // Default API URL
    var apiURL = getStringProperty(propertyName)

    val env = rootProject.file(flavorEnvFile)
    if (env.exists()) {
        env.forEachLine {
            val keyValuePair = it.split("=")
            if (keyValuePair.size > 1 && keyValuePair[0] == propertyName) {
                apiURL = keyValuePair[1]
                project.ext.set(keyValuePair[0], keyValuePair[1])
            }
        }
    }
    return apiURL
}

android {
    namespace = "com.weatherxm"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.weatherxm.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 57
        versionName = "3.6.2"

        // Resource value fields
        resValue("string", "mapbox_access_token", getStringProperty("MAPBOX_ACCESS_TOKEN"))

        // Instrumented Tests
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootDir.path}/${getStringProperty("RELEASE_KEYSTORE")}")
            storePassword = getStringProperty("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = getStringProperty("RELEASE_KEY_ALIAS")
            keyPassword = getStringProperty("RELEASE_KEY_PASSWORD")
        }
    }

    flavorDimensions.add("mode")
    flavorDimensions.add("server")

    productFlavors {
        create("local") {
            dimension = "mode"
            resValue("string", "app_name", "WXM Local Mock")
        }
        create("remote") {
            dimension = "mode"
        }
        create("mock") {
            val apiURL = getFlavorProperty("API_URL", "remotemock.env")
            val claimDAppUrl = getFlavorProperty("CLAIM_APP_URL", "remotemock.env")
            dimension = "server"
            applicationIdSuffix = ".mock"
            resValue("string", "app_name", "WXM Remote Mock")
            manifestPlaceholders["auth_host"] = "app-mock.weatherxm.com"
            manifestPlaceholders["explorer_host"] = "explorer-mock.weatherxm.com"
            buildConfigField("String", "API_URL", "\"$apiURL\"")
            buildConfigField("String", "AUTH_URL", "\"$apiURL\"")
            buildConfigField("String", "CLAIM_APP_URL", "\"$claimDAppUrl\"")
        }
        create("staging") {
            val apiURL = getFlavorProperty("API_URL", "staging.env")
            val claimDAppUrl = getFlavorProperty("CLAIM_APP_URL", "staging.env")
            dimension = "server"
            applicationIdSuffix = ".staging"
            resValue("string", "app_name", "WXM Staging")
            manifestPlaceholders["auth_host"] = "app-staging.weatherxm.com"
            manifestPlaceholders["explorer_host"] = "explorer-staging.weatherxm.com"
            buildConfigField("String", "API_URL", "\"$apiURL\"")
            buildConfigField("String", "AUTH_URL", "\"$apiURL\"")
            buildConfigField("String", "CLAIM_APP_URL", "\"$claimDAppUrl\"")
            firebaseAppDistribution {
                artifactType = "APK"
                releaseNotes = "Release notes for staging version"
                serviceCredentialsFile = "${rootDir.path}/ci-service-account.json"
                groups = getStringProperty("FIREBASE_INTERNAL_TEST_GROUP")
            }
        }
        create("dev") {
            val apiURL = getFlavorProperty("API_URL", "development.env")
            val claimDAppUrl = getFlavorProperty("CLAIM_APP_URL", "development.env")
            dimension = "server"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "WXM Dev")
            manifestPlaceholders["auth_host"] = "app-dev.weatherxm.com"
            manifestPlaceholders["explorer_host"] = "explorer-dev.weatherxm.com"
            buildConfigField("String", "API_URL", "\"$apiURL\"")
            buildConfigField("String", "AUTH_URL", "\"$apiURL\"")
            buildConfigField("String", "CLAIM_APP_URL", "\"$claimDAppUrl\"")
            firebaseAppDistribution {
                artifactType = "APK"
                releaseNotes = "Release notes for development version"
                serviceCredentialsFile = "${rootDir.path}/ci-service-account.json"
                groups = getStringProperty("FIREBASE_INTERNAL_TEST_GROUP")
            }
        }
        create("prod") {
            val apiURL = getFlavorProperty("API_URL", "production.env")
            val claimDAppUrl = getFlavorProperty("CLAIM_APP_URL", "production.env")
            dimension = "server"
            resValue("string", "app_name", "WeatherXM")
            manifestPlaceholders["auth_host"] = "app.weatherxm.com"
            manifestPlaceholders["explorer_host"] = "explorer.weatherxm.com"
            buildConfigField("String", "API_URL", "\"$apiURL\"")
            buildConfigField("String", "AUTH_URL", "\"$apiURL\"")
            buildConfigField("String", "CLAIM_APP_URL", "\"$claimDAppUrl\"")
            firebaseAppDistribution {
                artifactType = "APK"
                releaseNotes = "Release notes for production version"
                serviceCredentialsFile = "${rootDir.path}/ci-service-account.json"
                groups = getStringProperty("FIREBASE_PUBLIC_TEST_GROUP")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["crashlyticsEnabled"] = true
        }
        getByName("debug") {
            // Change minifyEnabled to true if you want to test code obfuscation in debug mode
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["crashlyticsEnabled"] = false
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"
    }

    // Filter out specific build variants
    androidComponents {
        beforeVariants { variantBuilder ->
            val name = variantBuilder.name.lowercase()
            val buildType = variantBuilder.buildType
            if (name.contains("local") && !name.contains("mock")) {
                variantBuilder.enable = false
            } else if (buildType != "debug" && name.contains("mock")) {
                variantBuilder.enable = false
            }
        }
    }

    // Flavor-specific version name & apk file name, also
    applicationVariants.all {
        val variant = this
        // Base version
        var version = "${variant.versionName}-${getGitCommitHash()}"

        // Add flavor in version, if mock or dev
        val flavor = variant.flavorName.lowercase()
        if (flavor.contains("mock")) {
            version = "$version-mock"
        } else if (flavor.contains("dev")) {
            version = "$version-development"
        }

        // Add debug build type in version
        if (variant.buildType.name.contains("debug")) {
            version = "$version-${variant.buildType.name}"
        }

        // Create app_version res string
        variant.resValue("string", "app_version", version)

        // Proper apk file name

        variant.outputs
            .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
            .forEach { output ->
                output.versionNameOverride = version
                output.outputFileName = "weatherxm-$version.apk"
            }

        // Delete mock files on release APK flavor
        if (variant.buildType.name.contains("release")) {
            variant.mergeAssetsProvider.get().doLast {
                val filesToDelete = fileTree(
                    mapOf(
                        "dir" to "variant.mergeAssetsProvider.get().outputDir",
                        "include" to listOf("**/mock/*")
                    )
                )
                delete(filesToDelete)
            }
        }
    }

    compileOptions {
        // Enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            excludes += listOf("META-INF/*")
        }
        resources {
            excludes += listOf("META-INF/*")
        }
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // Instrumented Tests
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // Desugaring for Java8 feature support
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Android Jetpack libraries
    implementation("androidx.activity:activity-ktx:1.8.1")
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.browser:browser:1.7.0")
    implementation("androidx.collection:collection-ktx:1.3.0")
    implementation("androidx.compose.foundation:foundation:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("androidx.startup:startup-runtime:1.1.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Material Components for Android
    implementation("com.google.android.material:material:1.10.0")

    // Google Play Services for Location & Maps
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // JSON serialization/deserialization
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")

    // HTTP client
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    // Network response wrapper
    implementation("com.github.haroldadmin:NetworkResponseAdapter:5.0.0")

    // JWT auth utilities
    implementation("com.auth0.android:jwtdecode:2.0.2")

    // Animations
    implementation("com.airbnb.android:lottie:6.2.0")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Permissions
    implementation("com.github.fondesa:kpermissions:3.4.0")

    // Dependency Injection
    implementation("io.insert-koin:koin-android:3.5.0")

    // Data types and more
    implementation(platform("io.arrow-kt:arrow-stack:1.2.1"))
    implementation("io.arrow-kt:arrow-core")
    implementation("io.arrow-kt:arrow-fx-coroutines")

    // Mapbox
    implementation("com.mapbox.maps:android:10.16.2")
    implementation("com.mapbox.search:mapbox-search-android:1.0.0-rc.7")
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-services:6.15.0")

    // Insetter
    implementation("dev.chrisbanes.insetter:insetter:0.6.1")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-perf")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-installations")

    // QR Code Scanner
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Retromock for mocking responses
    implementation("co.infinum:retromock:1.1.1")

    // Input masking (for Serial Number input)
    implementation("com.redmadrobot:input-mask-android:7.2.6")

    // Better link handling in text views
    implementation("me.saket:better-link-movement-method:2.2.0")

    // BLE Scanning and Communication
    implementation("com.juul.kable:core:0.27.0")
    implementation("com.github.espressif:esp-idf-provisioning-android:lib-2.1.2")
    implementation("no.nordicsemi.android:dfu:2.4.0")

    // Image Loader
    implementation("io.coil-kt:coil-base:2.5.0")
}
