plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.buglist"
    // Libraries from late 2026 require compileSdk 37. targetSdk stays 36:
    // that is the Play submission bar, and it keeps newer runtime behavior opt-in.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.buglist"
        minSdk = 26
        targetSdk = 36
        // Test update with the clearer paid history; installable over build 58.
        versionCode = 59
        versionName = "2.6.0"
        buildConfigField("String", "NTFY_TOPIC_URL", "\"https://ntfy.sh/BugListLogs\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // SQLCipher page size for Android 15+
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("buglist-release.jks")
            storePassword = "buglist123"
            keyAlias = "buglist"
            keyPassword = "buglist123"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
    }

    // Room schema export location for version-controlled migrations
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // JUnit Jupiter pulled in transitively by mockk-android — exclude duplicate license files
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/NOTICE.md"
        }
    }

}

// AGP 9 compiles Kotlin itself. jvmTarget lives on the Kotlin compiler, not android.kotlinOptions.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    // Compose BOM — all compose versions managed here
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle / ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt DI — KSP only, no KAPT
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Room — KSP only. room-ktx was merged into room-runtime in 2.7.
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // SQLCipher (Zetetic new artifact — supports 16KB pages)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)

    // Security
    implementation(libs.androidx.biometric)
    implementation(libs.tink.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.argon2kt)

    // Charts
    implementation(libs.vico.compose)

    // Drag-to-reorder for Crew list
    implementation(libs.reorderable)

    // In-person pairing QR. The mailbox only ever carries ciphertext.
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Ktor (Auto-Update via GitHub Releases)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)

    // Instrumented Tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    kspAndroidTest(libs.hilt.compiler)
}
