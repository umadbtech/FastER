# Adding Test Dependencies to build.gradle.kts

Add the following to your `app/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing dependencies ...

    // === Testing Dependencies ===
    
    // JUnit 4
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.1.5")
    
    // Kotlin Test
    testImplementation("kotlin-test:kotlin-test:1.9.0")
    
    // Coroutines Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    
    // MockK (Kotlin Mocking)
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("io.mockk:mockk-agent:1.13.5")
}

android {
    // ... existing config ...
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = false
            isReturnDefaultValues = true
        }
    }
}
```

---

## Sync Dependencies

After adding the dependencies to `build.gradle.kts`:

```bash
./gradlew clean build
# or
./gradlew syncDebugSources
```

---

## Verify Dependency Installation

```bash
./gradlew app:dependencies | grep -E "junit|mockk|coroutines-test|kotlin-test"
```

---

## Expected Output After Sync

```
+--- junit:junit:4.13.2
+--- androidx.test.ext:junit:1.1.5
+--- kotlin-test:kotlin-test:1.9.0
+--- org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1
+--- io.mockk:mockk:1.13.5
+--- io.mockk:mockk-agent:1.13.5
```

---

##  If you get "Unresolved reference" errors in IDE

1. **Invalidate IDE cache:**
   - Android Studio → File → Invalidate Caches / Restart → Invalidate and Restart

2. **Rebuild project:**
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

3. **Sync Gradle files:**
   - Android Studio → File → Sync Now

---

## Complete build.gradle.kts Example (App Module)

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.faster.festival"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.faster.festival"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = false
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    
    // Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    
    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    
    // === TESTING DEPENDENCIES ===
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("kotlin-test:kotlin-test:1.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("io.mockk:mockk-agent:1.13.5")
}
```

---

## Next Steps

1. Add the dependencies to `app/build.gradle.kts`
2. Click "Sync Now" in Android Studio
3. Run: `./gradlew testDebugUnitTest`
4. All tests should now compile and run!
