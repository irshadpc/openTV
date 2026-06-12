import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ---------------------------------------------------------------------------
// Optional release signing.
// Create a file named `keystore.properties` in the project root (NOT committed
// to source control) with these keys to produce a signed release build:
//
//   storeFile=/absolute/path/to/release.keystore
//   storePassword=********
//   keyAlias=opentv
//   keyPassword=********
//
// If the file is absent, the release build simply remains unsigned and you can
// sign it later with `apksigner`.
// ---------------------------------------------------------------------------
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "com.opentv.worldcup"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.opentv.worldcup"
        // Android TV 9 (API 28) and above, as required.
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            // Shrink and obfuscate to keep the APK small and startup fast.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Attach the signing config only when a keystore is configured.
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core AndroidX / Kotlin.
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Material components for the dark theme and settings dialog.
    implementation("com.google.android.material:material:1.12.0")

    // Leanback: makes the app a first-class Android TV / Google TV citizen.
    implementation("androidx.leanback:leanback:1.0.0")

    // WebKit support library: modern WebView APIs (dark mode, safe browsing)
    // back-ported to older Android TV versions.
    implementation("androidx.webkit:webkit:1.11.0")

    // Lifecycle helpers used by the connectivity monitor.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")

    // Modern splash screen (with graceful fallback on Android 9–11).
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ConstraintLayout for the overlay UI (spinner, error screen).
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
