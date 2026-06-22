import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// -----------------------------------------------------------------------------
// PLUGINS
// TODO - Find a way to apply these to only standard flavour
// -----------------------------------------------------------------------------
// 📦 Core plugins required for Android + Kotlin + Compose support.
plugins {
    alias(libs.plugins.android.application)   // Android application plugin
    alias(libs.plugins.kotlin.compose)        // Jetpack Compose UI toolkit
    alias(libs.plugins.crashanlytics)         // Firebase Crashlytics (should be flavor-scoped)
    alias(libs.plugins.google.services)       // Google Services (should be flavor-scoped)
}

// -----------------------------------------------------------------------------
// KOTLIN COMPILER OPTIONS
// -----------------------------------------------------------------------------
kotlin {
    compilerOptions {
        // Target JVM bytecode version (was "11" string, now typed enum)
        jvmTarget = JvmTarget.JVM_17

        // Add experimental/advanced compiler flags
        freeCompilerArgs.addAll(
            //   "-XXLanguage:+ExplicitBackingFields", //  Explicit backing fields
            "-XXLanguage:+NestedTypeAliases",
            "-Xopt-in=kotlin.RequiresOptIn", // Opt-in to @RequiresOptIn APIs
            "-Xwhen-guards",                 // Enable experimental when-guards
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi", // Compose foundation experimental
            "-Xopt-in=com.zs.compose.theme.ExperimentalThemeApi",             // Custom theme experimental
            "-Xnon-local-break-continue",    // Allow non-local break/continue
            "-Xcontext-sensitive-resolution",// Context-sensitive overload resolution
            "-Xcontext-parameters"           // Enable context parameters (experimental)
        )
    }
}

// -----------------------------------------------------------------------------
// COMPOSE COMPILER CONFIGURATION
// -----------------------------------------------------------------------------
// ⚙️ Controls advanced Compose compiler reporting and stability checks.
// Reports/metrics can be enabled for debugging but are usually disabled in release builds.
composeCompiler {
    // TODO - I guess disable these in release builds.reportsDestination =
    // layout.buildDirectory.dir("compose_compiler")
    // metricsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles = listOf(
        rootProject.layout.projectDirectory.file("stability.conf")
    )
}

// -----------------------------------------------------------------------------
// ANDROID CONFIGURATION
// -----------------------------------------------------------------------------
android {
    namespace = "com.zs.player"
    compileSdk { version = release(37) }
    buildFeatures { compose = true; resValues = true }

    //
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // -----------------------------------------------------------------------------
    // DEFAULT CONFIGURATION
    // -----------------------------------------------------------------------------
    // 📦 Core app settings: ID, SDK versions, versioning, and test runner.
    defaultConfig {
        applicationId = "com.googol.android.apps.oneplayer"
        minSdk = 28
        targetSdk = 37
        versionCode = 1000
        versionName = "2.0.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // -------------------------------------------------------------------------
    // PRODUCT FLAVORS
    // -------------------------------------------------------------------------
    flavorDimensions += "edition"
    productFlavors {
        // STANDARD (Default monetized edition: ads + telemetry + in-app purchases enabled)
        create("standard") { dimension = "edition"; }
        // COMMUNITY (Open-source edition: minimal free build, no ads, no telemetry, no purchases)
        create("community") { dimension = "edition"; versionNameSuffix = "-foss" }
        // PLUS (Privacy-friendly edition: ads + in-app purchases, but telemetry disabled)
        create("plus") {
            dimension = "edition"; versionNameSuffix = "-plus"; applicationIdSuffix = ".pro"
        }
        // PREMIUM (Full unlock edition: all features enabled, no ads, no telemetry, no purchases)
        create("gold") {
            dimension = "edition"; versionNameSuffix = "-gold"; applicationIdSuffix = ".full"
        }
    }

    // -----------------------------------------------------------------------------
    // Build Types
    // -----------------------------------------------------------------------------
    buildTypes {
        // -------------------------------------------------------------------------
        // RELEASE BUILD
        // -------------------------------------------------------------------------
        release {
            // ⚙️ Code shrinking/obfuscation (ProGuard/R8) and resource shrinking
            optimization { enable = true }
            // 🔑 Signing configuration (currently using debug keys for convenience)
            // signingConfig = signingConfigs.getByName("debug")
        }
        // -------------------------------------------------------------------------
        // DEBUG BUILD
        // -------------------------------------------------------------------------
        debug {
            // 📛 Appends ".debug" to the application ID so debug and release can coexist
            applicationIdSuffix = ".debug"
            resValue("string", "launcher_label", "Debug")
            versionNameSuffix = "-debug" // 🔖 Adds "-debug" suffix to version name for clarity
        }
    }
}

// -----------------------------------------------------------------------------
// APP DEPENDENCIES
// -----------------------------------------------------------------------------
dependencies {
    // Project modules
    implementation(project(":domain"))
    // compose ui
    implementation(libs.bundles.compose.ui)
    implementation(libs.bundles.toolkit)
    // misc
    implementation(libs.androidx.graphics.shapes) // Graphics utilities
    implementation(libs.nav2.compose) // migrate to nav3
    implementation(libs.mp3agic)  // Temporary MP3 library (to be moved later)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.google.fonts)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.koin)
    implementation(libs.chrisbanes.haze)
    implementation(libs.lottie.compose)
    implementation(libs.bundles.coil)
    // tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}