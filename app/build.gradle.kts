import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
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
// Simplify access to avoid nested string interpolation issues when Kotlin script compiles
val viteSupabaseUrl: String = envConfig["VITE_SUPABASE_URL"] ?: ""
val viteSupabaseAnonKey: String = envConfig["VITE_SUPABASE_ANON_KEY"] ?: ""
val googleMapsApiKey: String = envConfig["GOOGLE_MAPS_API_KEY"] ?: ""

// Project 2 — SOS trusted-device signing endpoints (pinch-ingest, pinch-alert-status,
// sos-register-device, sos-verify-attestation). See Pinch_SOS_Frontend_Implementation_Guide.md.
val project2SosUrl: String = envConfig["PROJECT2_SOS_URL"] ?: ""
val project2SosAnonKey: String = envConfig["PROJECT2_SOS_ANON_KEY"] ?: ""
val sosAllowTestAttestation: String = envConfig["SOS_ALLOW_TEST_ATTESTATION"] ?: "false"

// Detect whether the build should use the real Supabase client libraries
val useRealSupabase: Boolean = project.hasProperty("useRealSupabase") && project.property("useRealSupabase") == "true"

android {
    namespace = "com.faster.festival"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.faster.festival"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
        
        // Expose credentials provided in .env file
        buildConfigField("String", "VITE_SUPABASE_URL", "\"$viteSupabaseUrl\"")
        buildConfigField("String", "VITE_SUPABASE_ANON_KEY", "\"$viteSupabaseAnonKey\"")
        buildConfigField("String", "PROJECT2_SOS_URL", "\"$project2SosUrl\"")
        buildConfigField("String", "PROJECT2_SOS_ANON_KEY", "\"$project2SosAnonKey\"")
        buildConfigField(
            "boolean",
            "SOS_ALLOW_TEST_ATTESTATION",
            sosAllowTestAttestation.equals("true", ignoreCase = true).toString()
        )
        // NOTE: there is intentionally no test/mock-location build flag. SOS
        // location is real-GPS-only in every build (see SosLocationProvider).

        // Google Maps API key for map rendering
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsApiKey
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources.excludes.add("META-INF/proguard/androidx-*.pro")
        // Bouncy Castle (bcprov-jdk18on) and jspecify both ship an
        // OSGI manifest at the same MR-JAR path; AGP refuses to merge.
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        resources.excludes.add("META-INF/versions/*/OSGI-INF/MANIFEST.MF")
    }

    // If using the real Supabase client, exclude the local stub sources which would otherwise cause duplicate symbols.
    if (useRealSupabase) {
        sourceSets["main"].java.srcDirs.forEach { _ ->
            // Exclude the package folder where the stubs live. Pattern is relative to the source dir(s).
            sourceSets["main"].java.exclude("**/io/github/jan_tennert/supabase/**")
        }
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

    // Phone number parsing/validation
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.28")

    // ─── SOS trusted-device signing ─────────────────────────────────────────
    // Bouncy Castle for Ed25519 (Android Keystore added Ed25519 only on API 33+;
    // minSdk = 24). bcprov-jdk18on provides Ed25519PrivateKeyParameters / Ed25519Signer.
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    // Timber for the SOS setup / canonical / polling flow logs.
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Image Loading
    implementation(libs.coil)
    implementation(libs.coil.gif)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room (SQLite) for wristband + SOS history persistence
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager — telemetry batch upload (TelemetryUploadWorker). Coroutine
    // worker variant keeps the worker body suspending so the HTTP / Room
    // calls don't tie up a thread while waiting on network.
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Google Maps for Compose
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ─── Nordic nRF Mesh — FastER wristband BLE Mesh node ────────────────
    // See Mobile-Vendor-Model-Dev.md §9 + wristband/data/ble/*.kt for the
    // wrapper layer. Phase A defaults to FakeMeshManager; flip
    // WristbandModule.useFakeMesh = false to use the Nordic-backed impl
    // (requires NordicMeshManager.kt + BleMeshGatt.kt to be added).
    // Nordic Mesh < 3.4.0 was published as .jar which the Android Gradle
    // plugin can't accept directly; 3.4.0+ ships as proper .aar.
    implementation("no.nordicsemi.android:mesh:3.4.0")
    // ble:2.11+ was compiled with Kotlin 2.2 metadata which conflicts with the
    // project's Kotlin 2.0.21. 2.7.5 is the last release with Kotlin-2.0-
    // compatible metadata. We deliberately skip `ble-ktx` — BleMeshGatt uses
    // the Java-style BleManager API directly, no coroutine extensions needed.
    implementation("no.nordicsemi.android:ble:2.7.5")
    implementation("no.nordicsemi.android.support.v18:scanner:1.6.0")

    // Add the real Supabase client artifacts only when requested.
    if (useRealSupabase) {
        // Use version catalog entries instead of hard-coded coordinates.
        implementation(libs.supabase.kt.android)
        implementation(libs.supabase.kt.realtime)
    }

    // === TEST DEPENDENCIES ===
    // JUnit 4
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.1.5")

    // Kotlin Test
    testImplementation(kotlin("test"))

    // Coroutines Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")

    // MockK (Kotlin Mocking)
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("io.mockk:mockk-agent:1.13.5")
}

    /*

     Optional Supabase client libraries

     This project includes small local stubs under `app/src/main/java/io/github/jan_tennert/supabase/*`
     so the app can compile and run without the real Supabase artifacts (useful during development
     or when the upstream artifacts are not available on JitPack/MavenCentral).

     If you want to enable the real Supabase Kotlin client libraries, set the Gradle project
     property `useRealSupabase=true` (for example: `./gradlew assembleDebug -PuseRealSupabase=true`) and
     ensure the coordinates below match the published artifacts you want to use. These coordinates
     and versions may need to be adjusted to the correct groupId/artifactId/version available on
     Maven Central or your repository.

     Notes:
     - The project currently uses local stubs (see `io.github.jan_tennert.supabase.*` in source).
     - If you enable the real library, make sure the coordinates are correct and the stubs are excluded.
    */
