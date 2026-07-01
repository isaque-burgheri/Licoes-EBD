import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Read API key from local.properties (kept out of git).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val driveApiKey: String = localProps.getProperty("DRIVE_API_KEY", "")
val audioFolderId: String = localProps.getProperty("AUDIO_FOLDER_ID", "")

android {
    namespace = "br.com.licoesebd.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.licoesebd.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.3"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "DRIVE_API_KEY", "\"$driveApiKey\"")
        buildConfigField("String", "AUDIO_FOLDER_ID", "\"$audioFolderId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // PDF rendering uses Android's built-in android.graphics.pdf.PdfRenderer
    // (available since API 21, no external dependency needed)

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
