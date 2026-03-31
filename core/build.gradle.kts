// Import Kotlin Gradle DSL helpers for JVM target and Kotlin version configuration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    compileSdk = 36

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
    implementation(libs.bundles.analytics)  // Analytics bundle → tracking and reporting
}