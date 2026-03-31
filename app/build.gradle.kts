plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.xpenseledger.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xpenseledger.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

    // Required for 16 KB page-size compatibility (Android 15 / API 35+).
    // Uncompressed .so files allow the OS to mmap them directly at their on-disk alignment.
    packaging {
        jniLibs {
            useLegacyPackaging = false   // false = store uncompressed (default on AGP 8+, made explicit)
        }
    }
}

kotlin {
    // Use the same JVM version for all Kotlin tasks (including kapt)
    jvmToolchain(17)
}

ksp {
    // Disable Room schema verification (avoids loading SQLite JDBC on host)
    arg("room.verifySchema", "false")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Room / Hilt
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.compose.material:material-icons-extended")

    // Security
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")

    // SQLCipher removed: its libsqlcipher.so is not 16 KB page-aligned (Android 15 / Play Store mandate).
    // The database is protected by Android's per-app filesystem sandbox + full-disk encryption (FBE)
    // which is mandatory on all Android 10+ devices. App-level access is further guarded by the PIN lock.
    // This is the same security model used by Google Pay, PhonePe, and most production finance apps.

    // Serialization for encrypted backup export/import
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // --- Recommended test dependencies ---
    // Coroutines test utilities for deterministic coroutine testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    // Mocking library for Kotlin
    testImplementation("io.mockk:mockk:1.13.5")
    // Robolectric to run Android framework-dependent unit tests on the JVM (optional)
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Hilt testing helpers for androidTest
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    // KSP compiler for androidTest to generate Hilt code in tests
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.52")
    // AndroidX Test core (useful in androidTest)
    androidTestImplementation("androidx.test:core:1.5.0")
    // Make AndroidX Test core available to JVM unit tests (Robolectric)
    testImplementation("androidx.test:core:1.5.0")
}

// Task to run all tests. Note: connectedDebugAndroidTest requires a device/emulator to be available.
tasks.register("runAllTests") {
    group = "verification"
    description = "Run unit tests and connected instrumented tests (requires device/emulator)"
    dependsOn("testDebugUnitTest", "connectedDebugAndroidTest")
}