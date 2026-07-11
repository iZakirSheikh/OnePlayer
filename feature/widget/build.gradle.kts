import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// -----------------------------------------------------------------------------
// PLUGINS
// -----------------------------------------------------------------------------
plugins {
    alias(libs.plugins.android.library) // Android library plugin
    alias(libs.plugins.kotlin.android) // Kotlin Android plugin
    alias(libs.plugins.kotlin.compose) // Jetpack Compose plugin
}

// -----------------------------------------------------------------------------
// KOTLIN COMPILER OPTIONS
// -----------------------------------------------------------------------------
// Configure Kotlin compiler settings for this module
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// -----------------------------------------------------------------------------
// ANDROID CONFIGURATION
// -----------------------------------------------------------------------------
// Core Android build settings for this library module
android {
    // Unique namespace for generated R class and manifest
    namespace = "com.zs.feature.widget"
    compileSdk = 37 // Compile against the latest Android SDK version
    buildFeatures { compose = true } // Enable Jetpack Compose support

    // Java compatibility settings (ensures consistent bytecode level)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Default configuration for all build variants
    defaultConfig {
        minSdk = 28  // Minimum supported Android version
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    // Build type definitions
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
    flavorDimensions += "edition"
    productFlavors {
        create("standard") { dimension = "edition" }
        create("community") { dimension = "edition" }
        create("plus") { dimension = "edition" }
        create("gold") { dimension = "edition" }
    }
}

// -----------------------------------------------------------------------------
// DEPENDENCIES
// -----------------------------------------------------------------------------
// External libraries and internal modules required by this feature
dependencies {
    implementation(libs.glance.appwidget) // Jetpack Glance library for building app widgets
    implementation(libs.glance.material3) // Material 3 components for Glance widgets
    implementation(project(":core")) // Internal core module dependency
}
