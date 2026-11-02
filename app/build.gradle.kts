// Dateipfad: app/build.gradle.kts
// Build-Konfiguration für das Modul "app" (Jetpack Compose, Material 3, SDK-Versionen).

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dienstplan.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dienstplan.app"
        minSdk = 26 // Android 8.0+ (unterstützt moderne Datums-/Zeitberechnung nativ)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signiert die Release-APK automatisch mit dem Standard-Debug-Key,
            // damit du sie direkt aus GitHub Actions auf dem Handy installieren kannst.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Jetpack Compose & Material Design 3 (Material You)
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
