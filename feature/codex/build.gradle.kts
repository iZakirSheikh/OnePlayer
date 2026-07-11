import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// -----------------------------
// Plugins Section
// -----------------------------
plugins {
    alias(libs.plugins.android.dynamic.feature) // Apply Android Dynamic Feature plugin → enables modular feature delivery
    alias(libs.plugins.kotlin.android) // Apply Kotlin Android plugin → adds Kotlin support for Android development
}

// -----------------------------
// Kotlin Compiler Options
// -----------------------------
kotlin {
    compilerOptions {
        // Configure Kotlin compiler to target JVM 17 → ensures compatibility with modern Java features
        jvmTarget = JvmTarget.JVM_17
    }
}

// -----------------------------
// Android Configuration
// -----------------------------
android {
    namespace = "com.zs.feature.codex"
    compileSdk = 37  // Compile against Android SDK level 36 → latest APIs available

    defaultConfig {
        minSdk = 28  // Minimum supported Android version (28 → Android 9 Pie)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // -----------------------------
    // Build Types
    // -----------------------------
    buildTypes {
        release {
            // Disable code shrinking/obfuscation for release build (can be enabled later for optimization)
            isMinifyEnabled = false
            // Apply custom ProGuard rules for release builds
            proguardFiles("proguard-rules.pro")
        }
    }

    // -----------------------------
    // Java Compatibility Options
    // -----------------------------
    // Ensure both source and target compatibility with Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // -------------------------------------------------------------------------
    flavorDimensions += "edition"
    productFlavors {
        create("standard") { dimension = "edition" }
        create("community") { dimension = "edition" }
        create("plus") { dimension = "edition" }
        create("gold") { dimension = "edition" }
    }
}

// -----------------------------
// Dependencies
// -----------------------------
dependencies {
    implementation(project(":app"))
    implementation(libs.nextlib)  // Media3 decoders + extensions
}
