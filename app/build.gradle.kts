plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.google.services)
    alias(libs.plugins.grgit)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    jacoco // Alternative is Kover but still in beta: https://github.com/Kotlin/kotlinx-kover
}

jacoco {
    toolVersion = "0.8.12"
}

fun getVersionGitTags(printForDebugging: Boolean = false): List<String> {
    if (printForDebugging) {
        println("Filter and print ordered release tags")
    }
    val versionTagsWithOptionalRCRegex = Regex("[RC-]*[0-9]*_*[0-9]+[.][0-9]+[.][0-9]+")
    return grgit.tag.list().filter {
        it.name.matches(versionTagsWithOptionalRCRegex)
    }.sortedBy {
        it.dateTime
    }.map {
        if (printForDebugging) {
            println("${it.name} --- (${it.dateTime})")
        }
        it.name
    }
}

fun getLastVersionGitTag(printForDebugging: Boolean = true): String {
    var lastVersionTag = getVersionGitTags(printForDebugging).last()
    if (lastVersionTag.startsWith("RC")) {
        lastVersionTag = lastVersionTag.substringAfterLast("_")
    }
    println("Last Version Tag: $lastVersionTag")
    return lastVersionTag
}

fun getGitCommitHash(): String {
    return grgit.head().abbreviatedId
}

fun hasProperty(name: String): Boolean {
    return project.hasProperty(name)
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
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.weatherxm.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 10 + getVersionGitTags().size
        val skipTagsLogging = !project.hasProperty("SKIP_TAGS_LOGGING")
        versionName = getLastVersionGitTag(skipTagsLogging)

        // Resource value fields
        resValue("string", "mapbox_access_token", getStringProperty("MAPBOX_ACCESS_TOKEN"))
        resValue("string", "mapbox_style", getStringProperty("MAPBOX_STYLE"))

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
        if (hasProperty("DEBUG_KEYSTORE")) {
            create("debug-config") {
                storeFile = file("${rootDir.path}/${getStringProperty("DEBUG_KEYSTORE")}")
                storePassword = getStringProperty("DEBUG_KEYSTORE_PASSWORD")
                keyAlias = getStringProperty("DEBUG_KEY_ALIAS")
                keyPassword = getStringProperty("DEBUG_KEY_PASSWORD")
            }
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
            val mixpanelToken = getFlavorProperty("MIXPANEL_TOKEN", "remotemock.env")
            dimension = "server"
            applicationIdSuffix = ".mock"
            resValue("string", "app_name", "WXM Remote Mock")
            manifestPlaceholders["auth_host"] = "app-mock.weatherxm.com"
            manifestPlaceholders["explorer_host"] = "explorer-mock.weatherxm.com"
            buildConfigField("String", "API_URL", "\"$apiURL\"")
            buildConfigField("String", "AUTH_URL", "\"$apiURL\"")
            buildConfigField("String", "CLAIM_APP_URL", "\"$claimDAppUrl\"")
            buildConfigField("String", "MIXPANEL_TOKEN", "\"$mixpanelToken\"")
        }
        create("staging") {
            val apiURL = getFlavorProperty("API_URL", "staging.env")
            val claimDAppUrl = getFlavorProperty("CLAIM_APP_URL", "staging.env")
            val mixpanelToken = getFlavorProperty("MIXPANEL_TOKEN", "staging.env")
            dimension = "server"
            applicationIdSuffix = ".staging"
            resValue("string", "app_name", "WXM Staging")
            manifestPlaceholders["auth_host"] = "app-staging.weatherxm.com"
            manifestPlaceholders["explorer_host"] = "explorer-staging.weatherxm.com"
            buildConfigField("String", "API_URL", "\"$apiURL\"")
            buildConfigField("String", "AUTH_URL", "\"$apiURL\"")
            buildConfigField("String", "CLAIM_APP_URL", "\"$claimDAppUrl\"")
            buildConfigField("String", "MIXPANEL_TOKEN", "\"$mixpanelToken\"")
            firebaseAppDistribution {
                artifactType = "APK"
                releaseNotes = "Release notes for staging version"
                serviceCredentialsFile = "${rootDir.path}/ci-service-account.json"
                groups = getStringProperty("FIREBASE_TEST_GROUP")
            }
        }
        create("dev") {
            val apiURL = getFlavorProperty("API_URL", "development.env")
            val claimDAppUrl = getFlavorProperty("CLAIM_APP_URL", "development.env")
            val mixpanelToken = getFlavorProperty("MIXPANEL_TOKEN", "development.env")
            dimension = "server"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "WXM Dev")
            manifestPlaceholders["auth_host"] = "app-dev.weatherxm.com"
            manifestPlaceholders["explorer_host"] = "explorer-dev.weatherxm.com"
            buildConfigField("String", "API_URL", "\"$apiURL\"")
            buildConfigField("String", "AUTH_URL", "\"$apiURL\"")
            buildConfigField("String", "CLAIM_APP_URL", "\"$claimDAppUrl\"")
            buildConfigField("String", "MIXPANEL_TOKEN", "\"$mixpanelToken\"")
            firebaseAppDistribution {
                artifactType = "APK"
                releaseNotes = "Release notes for development version"
                serviceCredentialsFile = "${rootDir.path}/ci-service-account.json"
                groups = getStringProperty("FIREBASE_TEST_GROUP")
            }
        }
        create("prod") {
            val apiURL = getFlavorProperty("API_URL", "production.env")
            val claimDAppUrl = getFlavorProperty("CLAIM_APP_URL", "production.env")
            val mixpanelToken = getFlavorProperty("MIXPANEL_TOKEN", "production.env")
            dimension = "server"
            resValue("string", "app_name", "WeatherXM")
            manifestPlaceholders["auth_host"] = "app.weatherxm.com"
            manifestPlaceholders["explorer_host"] = "explorer.weatherxm.com"
            buildConfigField("String", "API_URL", "\"$apiURL\"")
            buildConfigField("String", "AUTH_URL", "\"$apiURL\"")
            buildConfigField("String", "CLAIM_APP_URL", "\"$claimDAppUrl\"")
            buildConfigField("String", "MIXPANEL_TOKEN", "\"$mixpanelToken\"")
            firebaseAppDistribution {
                artifactType = "APK"
                releaseNotes = "Release notes for production version"
                serviceCredentialsFile = "${rootDir.path}/ci-service-account.json"
                groups = getStringProperty("FIREBASE_TEST_GROUP")
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
            signingConfigs.firstOrNull { it.name == "debug-config" }?.let {
                signingConfig = it
            }
            // Change minifyEnabled to true if you want to test code obfuscation in debug mode
            isMinifyEnabled = false
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
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
        var version = versionName

        // Add short Git Commit Hash after the version
        version = "$version-${getGitCommitHash()}"

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
            .map {
                (it as com.android.build.gradle.internal.api.ApkVariantOutputImpl).apply {
                    outputFileName = "weatherxm-$version.apk"
                }
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

    android.testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    sourceSets {
        getByName("test") {
            resources.srcDirs("src/main/assets")
        }
    }
}

tasks.register("jacocoCoverageTestReport", type = JacocoReport::class) {
    dependsOn("testRemoteProdDebugUnitTest", "createRemoteProdDebugUnitTestCoverageReport")
    group = "Reporting"
    description = "Generate Jacoco coverage reports after running unit tests."

    reports {
        xml.required = true
        csv.required = false
        html.outputLocation = layout.buildDirectory.dir("reports/jacoco/coverage-report")
        xml.outputLocation = layout.buildDirectory.file("reports/jacoco/report.xml")
    }

    val fileFilter = listOf(
        "**/databinding/*.*",
        "**/app/*.*",
        "**/data/Modules*.*",
        "**/data/ClientIdentificationHelper*.*",
        "**/data/ApiServiceModule*.*",
        "**/data/*Error*.*",
        "**/data/*Adapter*.*",
        "**/data/bluetooth/**",
        "**/data/models/**",
        "**/data/database/AppDatabase*.*",
        "**/data/database/dao/**",
        "**/data/database/entities/**",
        "**/data/network/**",
        "**/service/**",
        "**/ui/**/*Activity*.*",
        "**/ui/**/*Fragment*.*",
        "**/ui/**/*Adapter*.*",
        "**/ui/**/*DiffCallback*.*",
        "**/ui/**/*CardView*.*",
        "**/ui/common/Animation*.*",
        "**/ui/common/Views*.*",
        "**/ui/components/Base*.*",
        "**/ui/components/*View.*",
        "**/ui/components/ChartsView*.*",
        "**/ui/components/DailyRewardsCardView*.*",
        "**/ui/components/HidingBottomNavigationView*.*",
        "**/ui/components/DateNavigator*.class",
        "**/ui/components/DatePicker.class",
        "**/ui/widgets/RemoteViews*.*",
        "**/ui/widgets/currentweather/*.*",
        "**/ui/Navigator.class",
        "**/util/ChartsKt*.*",
        "**/util/ImageFileHelperKt*.*",
        "**/util/CountDownTimerHelper*.*",
        "**/BuildConfig.class"
    )
    val debugTree = fileTree(
        mapOf(
            "dir" to "${layout.buildDirectory.get()}/" +
                "intermediates/classes/remoteProdDebug/" +
                "transformRemoteProdDebugClassesWithAsm/dirs/com",
            "excludes" to fileFilter
        )
    )
    val mainSrc = "/src/main/java"

    sourceDirectories = files(listOf(mainSrc))
    classDirectories = files(listOf(debugTree))
    executionData = fileTree(
        mapOf(
            "dir" to "${layout.buildDirectory.get()}",
            "includes" to listOf(
                "/outputs/unit_test_code_coverage/" +
                    "remoteProdDebugUnitTest/" +
                    "testRemoteProdDebugUnitTest.exec"
            )
        )
    )
}

dependencies {
    // Testing
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.koin.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.koin.test)
    testImplementation(libs.kotest)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.koin)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.json)

    // Desugaring for Java8 feature support
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Android Jetpack libraries
    implementation(libs.androidx.activity)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.collection.ktx)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.work.runtime.ktx)

    // Material Components for Android
    implementation(libs.material)

    // Google Play Services for Location & Maps & QR Scanner
    implementation(libs.play.services.location)
    implementation(libs.play.services.scanner)

    // Barcode Scanner
    implementation(libs.barcode.scanner)

    // Logging
    implementation(libs.timber)

    // JSON serialization/deserialization
    implementation(libs.moshi)
    implementation(libs.moshi.adapters)
    ksp(libs.moshi.kotlin.codegen)

    // HTTP client
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Network response wrapper
    implementation(libs.networkResponseAdapter)

    // JWT auth utilities
    implementation(libs.jwtdecode)

    // Animations
    implementation(libs.lottie)

    // Charts
    implementation(libs.mpAndroidCharts)

    // Permissions
    implementation(libs.kpermissions)

    // Dependency Injection
    implementation(libs.koin.android)

    // Data types and more
    implementation(platform(libs.arrow.stack.bom))
    implementation(libs.arrow.core)

    // Mapbox (SDK Services used for the Minimap in Station Settings)
    implementation(libs.mapbox)
    implementation(libs.mapbox.search.android)
    implementation(libs.mapbox.sdk.services)

    // Insetter
    implementation(libs.insetter)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.installations)

    // Mixpanel Analytics
    implementation(libs.mixpanel)

    // Retromock for mocking responses
    implementation(libs.retromock)

    // Input masking (for Serial Number input)
    implementation(libs.input.mask.android)

    // Better link handling in text views
    implementation(libs.better.link.movement.method)

    // BLE Scanning and Communication
    implementation(libs.kable)
    implementation(libs.dfu)

    // Image Loader
    implementation(libs.coil.base)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Chucker - HTTP Inspector
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.no.op)

    // Upload Service
    implementation(libs.upload.service)
    implementation(libs.upload.service.okhttp)
}
