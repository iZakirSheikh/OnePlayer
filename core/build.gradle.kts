// Import Kotlin Gradle DSL helpers for JVM target and Kotlin version configuration
import com.android.build.api.dsl.VariantDimension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// -----------------------------------------------------------------------------
// SECRETS
// -----------------------------------------------------------------------------
// 🔐 Keys or IDs injected into BuildConfig at runtime.
private val secrets = arrayOf("ADS_APP_ID", "PLAY_CONSOLE_APP_RSA_KEY")

/**
 * Helper function to add a string BuildConfig field.
 */
private fun VariantDimension.buildConfigField(name: String, value: String) =
    buildConfigField("String", name, "\"" + value + "\"")

// -----------------------------
// Plugins Section
// -----------------------------
plugins {
    alias(libs.plugins.android.library) // Android Library plugin → marks this module as a reusable library
    alias(libs.plugins.kotlin.android) // Kotlin Android plugin → enables Kotlin support for Android development
    alias(libs.plugins.ksp) // KSP (Kotlin Symbol Processing) → lightweight annotation processing for code generation (e.g., Room, custom processors)
}

// -----------------------------
// Kotlin Compiler Options
// -----------------------------
// Configure Kotlin compiler with JVM target and advanced language features
kotlin {
    compilerOptions {
        // Target JVM bytecode version (modern Java 17 features)
        jvmTarget = JvmTarget.JVM_17

        // Add experimental/advanced compiler flags for cutting-edge features
        freeCompilerArgs.addAll(
            "-Xopt-in=kotlin.RequiresOptIn", // Opt-in to APIs marked with @RequiresOptIn
            "-Xwhen-guards",                 // Enable experimental when-guards for safer branching
            "-Xnon-local-break-continue",    // Allow non-local break/continue in inline functions
            "-Xcontext-sensitive-resolution",// Smarter overload resolution based on context
            "-Xcontext-parameters"           // Enable experimental context parameters
        )
    }
}

// -----------------------------
// Android Configuration
// -----------------------------
android {
    // Unique namespace for this core library module
    namespace = "com.zs.core"
    // Compile against Android SDK level 36 → latest APIs available
    compileSdk = 37
    buildFeatures { buildConfig = true; resValues = true }

    // -----------------------------
    // Java Compatibility Options
    // -----------------------------
    // Ensure both source and target compatibility with Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 28 // Minimum supported Android version (28 → Android 9 Pie)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro") // Consumer ProGuard rules → applied when this library is consumed by other apps/modules
        // --------------------------------------------------------------------
        // BUILD CONFIG: SECRETS
        // --------------------------------------------------------------------
        // Inject secrets from environment variables.
        // Missing values default to empty strings to avoid build failures.
        for (secret in secrets)
            buildConfigField(secret, System.getenv(secret) ?: "")

        // 📌 Edition constants (used for comparison in code)
        buildConfigField("FLAVOR_COMMUNITY", "community")
        buildConfigField("FLAVOR_STANDARD", "standard")
        buildConfigField("FLAVOR_PLUS", "plus")
        buildConfigField("FLAVOR_GOLD", "gold")
    }

    // -----------------------------
    // Build Types
    // -----------------------------
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // -------------------------------------------------------------------------
    // PRODUCT FLAVORS
    // -------------------------------------------------------------------------
    flavorDimensions += "edition"
    productFlavors {
        // STANDARD → Default monetized edition.
        // PLUS + Ad SDK
        create("standard") { dimension = "edition" }

        // COMMUNITY → FOSS/open‑source build.
        // Minimal free edition with no ads, no telemetry, and no purchases.
        create("community") { dimension = "edition" }

        // PLUS → Privacy-friendly edition:
        // No Ad SDK, but telemetry and in‑app purchases.
        create("plus") { dimension = "edition" }

        // PREMIUM → Full unlock build.
        // Based on Community, but with all features enabled.
        create("gold") { dimension = "edition" }
    }
    // -------------------------------------------------------------------------
    // SOURCE SETS CONFIGURATION
    // -------------------------------------------------------------------------
    sourceSets {
        // Community flavor → uses stubbed (no-op) implementations for all shared libs
        getByName("community") {
            kotlin.directories += "src/shared/analytics/stub/java"
            kotlin.directories += "src/shared/market/stub/java"
        }

        // Premium flavor → also wired to stub implementations (restricted feature set)
        getByName("plus") {
            kotlin.directories += "src/shared/analytics/actual/java"
            kotlin.directories += "src/shared/market/actual/java"
        }

        // Standard flavor → full/actual implementations of analytics, billing, and ads
        getByName("standard") {
            kotlin.directories += "src/shared/analytics/actual/java"
            kotlin.directories += "src/shared/market/actual/java"
        }

        // Plus flavor → only requires actual billing implementation (no analytics/ads)
        getByName("gold") {
            kotlin.directories += "src/shared/analytics/stub/java"
            kotlin.directories += "src/shared/market/actual/java"
        }
    }

}

// -----------------------------
// Dependencies
// -----------------------------
// Core AndroidX utilities
dependencies {
    implementation(libs.androidx.core.ktx)          // Kotlin extensions for Android core APIs
    implementation(libs.androidx.activity.compose)  // Compose integration with Activity lifecycle
    // Room database (bundled dependencies + compiler via KSP)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    // Media3 → modern media playback APIs
    implementation(libs.media3.session)             // Media session management
    implementation(libs.media3.exoplayer)           // ExoPlayer for playback
    implementation(libs.media3.ui) // for using SubtitleView
    implementation(libs.play.billing.client) // Play Billing → in-app purchases
    implementation(libs.coil.core)  // Coil → lightweight image loading library
    implementation(libs.androidx.palette) // AndroidX Palette → extract prominent colors from images
    // standard
    "standardImplementation"(libs.bundles.analytics)
    "standardImplementation"(libs.bundles.play.services)
    // plus
    "plusImplementation"(libs.bundles.analytics)
    "plusImplementation"(libs.bundles.play.services)
    // gold
    "goldImplementation"(libs.bundles.play.services)
}