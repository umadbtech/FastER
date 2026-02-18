import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

// Load .env file
fun loadEnvFile(): Map<String, String> {
    val envFile = rootProject.file(".env")
    val envMap = mutableMapOf<String, String>()

    if (envFile.exists()) {
        envFile.useLines { lines ->
            lines.forEach { line ->
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        envMap[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        }
    }
    return envMap
}

val envConfig = loadEnvFile()

android {
    namespace = "com.faster.festival"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.faster.festival"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
        
        // Expose credentials provided in .env file
        buildConfigField("String", "VITE_SUPABASE_URL", "\"${envConfig["VITE_SUPABASE_URL"] ?: ""}\"")
        buildConfigField("String", "VITE_SUPABASE_ANON_KEY", "\"${envConfig["VITE_SUPABASE_ANON_KEY"] ?: ""}\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources.excludes.add("META-INF/proguard/androidx-*.pro")
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.material)

    // Activity & Navigation
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.serialization)

    // Security
    implementation(libs.security.crypto)

    // QR Code
    implementation(libs.zxing.core)

    // Supabase
    implementation(libs.supabase.core)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
}
