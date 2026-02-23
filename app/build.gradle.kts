// Import Gradle DSL helpers for Android and Kotlin configuration
import com.android.build.api.dsl.ApplicationDefaultConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// -----------------------------
// Plugins Section
// -----------------------------
plugins {
    alias(libs.plugins.android.application) // Android Application plugin → marks this module as the main app entry point
    alias(libs.plugins.kotlin.android) // Kotlin Android plugin → enables Kotlin support for Android development
    alias(libs.plugins.kotlin.compose) // Kotlin Compose plugin → adds Jetpack Compose compiler integration
    alias(libs.plugins.google.services) // Google Services plugin → integrates Firebase/Google services (e.g., Analytics, Auth)
    alias(libs.plugins.crashanlytics)// Crashlytics plugin → enables Firebase Crashlytics for crash reporting
}

// -----------------------------
// Kotlin Compiler Options
// -----------------------------
// Configure Kotlin compiler with JVM target and advanced language features
kotlin {
    compilerOptions {
        // Target JVM bytecode version (modern Java 17 features)
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

// -----------------------------
// Secrets Management
// -----------------------------
// Secrets injected into BuildConfig at runtime (from GitHub Actions env variables)
private val secrets = arrayOf("ADS_APP_ID", "PLAY_CONSOLE_APP_RSA_KEY")

/**
 * Helper function to add a string BuildConfig field.
 */
private fun ApplicationDefaultConfig.buildConfigField(name: String, value: String) =
    buildConfigField("String", name, "\"" + value + "\"")

// -----------------------------
// Android Configuration
// -----------------------------
android {
    buildFeatures { compose = true; buildConfig = true } // Enable Compose and BuildConfig generation
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }  // Exclude redundant license files from packaging
    namespace = "com.zs.audiofy" // Unique namespace for the app
    compileSdk = 36 // Compile against Android SDK level 36 → latest APIs available

    // -----------------------------
    // Compose Compiler Configuration
    // -----------------------------
    composeCompiler {
        // Stability configuration for Compose compiler
        // TODO - I guess disable these in release builds.reportsDestination =
        //     layout.buildDirectory.dir("compose_compiler")
        // metricsDestination = layout.buildDirectory.dir("compose_compiler")
        stabilityConfigurationFiles = listOf(
            rootProject.layout.projectDirectory.file("stability_config.conf")
        )
    }

    // -----------------------------
    // Java Compatibility Options
    // -----------------------------
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // -----------------------------
    // Default Config
    // -----------------------------
    defaultConfig {
        applicationId = "com.googol.android.apps.oneplayer" // Unique app ID
        minSdk = 28                                         // Minimum supported Android version
        targetSdk = 36                                      // Target SDK
        versionCode = 24                                    // Internal version code
        versionName = "1.5.8-beta"                          // User-facing version name
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Inject secrets into BuildConfig (from environment variables)
        for (secret in secrets) {
            buildConfigField(secret, System.getenv(secret) ?: "")
        }
    }

    // -----------------------------
    // Build Types
    // -----------------------------
    buildTypes {
        // Release build configuration
        release {
            isMinifyEnabled = true       // Enable code shrinking/obfuscation
            isShrinkResources = true     // Remove unused resources
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("debug") // 👈 temporary debug signing
        }

        // Debug build configuration
        debug {
            applicationIdSuffix = ".debug"   // Allows installing debug + release side by side
            resValue("string", "launcher_label", "Debug") // Custom launcher label
            versionNameSuffix = "-debug"     // Append suffix to version name
        }
    }
    dynamicFeatures += setOf(":feature:codex")
}

// -----------------------------
// Dependencies
// -----------------------------
// Core libraries and Compose ecosystem
dependencies {
    implementation(libs.androidx.koin)                // Dependency injection with Koin
    implementation(libs.bundles.compose)              // Jetpack Compose bundle
    implementation(libs.androidx.startup)             // App startup initialization
    implementation(libs.androidx.splashscreen)        // Splash screen API
    implementation(libs.accompanist.permissions)      // Permissions handling in Compose
    implementation(libs.google.fonts)                 // Google Fonts integration
    implementation(libs.chrisbanes.haze)              // Visual effects library
    implementation(libs.nav2.compose)                 // Navigation for Compose
    implementation(libs.lottie.compose)               // Lottie animations in Compose
    implementation(libs.toolkit.theme)                // Custom theme toolkit
    implementation(libs.toolkit.foundation)           // Foundation toolkit
    implementation(libs.toolkit.preferences)          // Preferences toolkit
    implementation(libs.coil.compose)                 // Image loading in Compose
    implementation(libs.coil.video)                   // Video thumbnails with Coil
    implementation(libs.androidx.constraint.layout)   // ConstraintLayout support
    implementation(libs.bundles.play.services)  // Google Play Services bundle
    implementation(libs.mp3agic)  // Temporary MP3 library (to be moved later)
    implementation(libs.androidx.graphics.shapes) // Graphics utilities

    // Project modules
    api(project(":core"))                             // Expose core module APIs
    implementation(project(":feature:widget"))        // Widget feature module
}