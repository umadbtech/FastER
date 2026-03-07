# 🛠️ Missing API Implementations - Code Templates

**Purpose:** Quick implementation guide for missing API endpoints  
**Date:** March 4, 2026

---

## 1️⃣ PROFILE MANAGEMENT - Save Legal Name

### **Implementation Steps**

#### Step 1: Add to `OnboardingApiService.kt`
```kotlin
/**
 * POST /functions/v1/save-profile-name
 * Save user's full legal name
 */
@POST("functions/v1/save-profile-name")
suspend fun saveProfileName(
    @Header("Authorization") authorization: String,
    @Body request: SaveProfileNameRequest
): Response<OnboardingResponse>
```

#### Step 2: Create Request Model in `OnboardingModels.kt`
```kotlin
@Serializable
data class SaveProfileNameRequest(
    val legal_first_name: String? = null,
    val legal_last_name: String? = null
)
```

#### Step 3: Add Repository Method in `OnboardingRepository.kt`
```kotlin
suspend fun saveProfileName(
    firstName: String? = null,
    lastName: String? = null
): Result<OnboardingResponse> {
    return withContext(Dispatchers.IO) {
        try {
            val token = sessionManager.getAccessToken() 
                ?: return@withContext Result.failure(Exception("No access token"))
            val authHeader = "Bearer $token"
            val request = SaveProfileNameRequest(firstName, lastName)
            val response = onboardingApiService.saveProfileName(authHeader, request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) return@withContext Result.failure(Exception("Empty response"))
                Result.success(body)
            } else {
                val userMessage = mapErrorMessage(response.code())
                Result.failure(Exception(userMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error saving profile name"))
        }
    }
}
```

#### Step 4: Call from ViewModel
```kotlin
// In OnboardingViewModel.kt
fun updateProfileName(firstName: String, lastName: String) {
    _formState.update { it.copy(
        legalFirstName = firstName,
        legalLastName = lastName
    )}
}

private fun proceedFromProfileName() {
    val state = _formState.value
    if (state.legalFirstName.isBlank() && state.legalLastName.isBlank()) {
        _uiState.value = OnboardingUiState.Error("Please enter at least one name")
        return
    }
    
    viewModelScope.launch {
        _uiState.value = OnboardingUiState.Loading
        val result = onboardingRepository.saveProfileName(
            state.legalFirstName.ifBlank { null },
            state.legalLastName.ifBlank { null }
        )
        result.onSuccess { response ->
            if (response.activated == true) {
                _uiState.value = OnboardingUiState.OnboardingComplete
            } else {
                setMissingFields(response.missing)
                proceedToNextStep()
            }
        }.onFailure { error ->
            _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to save name")
        }
    }
}
```

---

## 2️⃣ PROFILE MANAGEMENT - Upload Avatar

### **Implementation Steps**

#### Step 1: Add to `OnboardingApiService.kt`
```kotlin
/**
 * POST /functions/v1/upload-avatar
 * Upload profile avatar image
 */
@Multipart
@POST("functions/v1/upload-avatar")
suspend fun uploadAvatar(
    @Header("Authorization") authorization: String,
    @Part file: MultipartBody.Part
): Response<AvatarUploadResponse>
```

#### Step 2: Create Models
```kotlin
// In OnboardingModels.kt
@Serializable
data class AvatarUploadResponse(
    val saved: Boolean,
    val avatar_path: String,
    val signed_avatar_url: String,
    val signed_avatar_url_expires_in_seconds: Int
)
```

#### Step 3: Add Repository Method
```kotlin
suspend fun uploadAvatar(imageFile: File): Result<AvatarUploadResponse> {
    return withContext(Dispatchers.IO) {
        try {
            val token = sessionManager.getAccessToken() 
                ?: return@withContext Result.failure(Exception("No access token"))
            val authHeader = "Bearer $token"
            
            // Create multipart body
            val requestBody = imageFile.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("file", imageFile.name, requestBody)
            
            val response = onboardingApiService.uploadAvatar(authHeader, part)

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) return@withContext Result.failure(Exception("Empty response"))
                Result.success(body)
            } else {
                val userMessage = when (response.code()) {
                    413 -> "Image file is too large. Max 5MB."
                    415 -> "Image format not supported. Use JPEG, PNG, or WebP."
                    else -> mapErrorMessage(response.code())
                }
                Result.failure(Exception(userMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload avatar: ${e.message}"))
        }
    }
}
```

#### Step 4: ViewModel Integration
```kotlin
fun uploadProfileAvatar(imageFile: File) {
    viewModelScope.launch {
        _uiState.value = OnboardingUiState.Loading
        val result = onboardingRepository.uploadAvatar(imageFile)
        result.onSuccess { response ->
            _formState.update { it.copy(avatarPath = response.avatar_path) }
            _uiState.value = OnboardingUiState.Success("Avatar uploaded successfully")
            // Proceed after slight delay to show success
            proceedToNextStep()
        }.onFailure { error ->
            _uiState.value = OnboardingUiState.Error(error.message ?: "Avatar upload failed")
        }
    }
}
```

---

## 3️⃣ PROFILE MANAGEMENT - Get Profile Avatar URL

### **Implementation**

#### Step 1: Add to API Service
```kotlin
@GET("functions/v1/avatar-url")
suspend fun getAvatarUrl(
    @Header("Authorization") authorization: String
): Response<AvatarUrlResponse>
```

#### Step 2: Create Model
```kotlin
@Serializable
data class AvatarUrlResponse(
    val ok: Boolean,
    val avatar_path: String? = null,
    val signed_avatar_url: String? = null,
    val signed_avatar_url_expires_in_seconds: Int? = null
)
```

#### Step 3: Repository Method
```kotlin
suspend fun getAvatarUrl(): Result<AvatarUrlResponse> {
    return withContext(Dispatchers.IO) {
        try {
            val token = sessionManager.getAccessToken() 
                ?: return@withContext Result.failure(Exception("No access token"))
            val authHeader = "Bearer $token"
            val response = onboardingApiService.getAvatarUrl(authHeader)

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) return@withContext Result.failure(Exception("Empty response"))
                Result.success(body)
            } else {
                Result.failure(Exception(mapErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get avatar URL"))
        }
    }
}
```

---

## 4️⃣ PROFILE MANAGEMENT - Get Profile Summary

### **Implementation**

#### Step 1: Add to API Service
```kotlin
@GET("functions/v1/profile-summary")
suspend fun getProfileSummary(
    @Header("Authorization") authorization: String
): Response<ProfileSummaryResponse>
```

#### Step 2: Repository Method (Model already exists!)
```kotlin
suspend fun getProfileSummary(): Result<ProfileSummaryResponse> {
    return withContext(Dispatchers.IO) {
        try {
            val token = sessionManager.getAccessToken() 
                ?: return@withContext Result.failure(Exception("No access token"))
            val authHeader = "Bearer $token"
            val response = onboardingApiService.getProfileSummary(authHeader)

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) return@withContext Result.failure(Exception("Empty response"))
                Result.success(body)
            } else {
                Result.failure(Exception(mapErrorMessage(response.code())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to load profile"))
        }
    }
}
```

#### Step 3: Call After Onboarding
```kotlin
// In OnboardingViewModel - after onboarding completion
private fun proceedFromTermsAcceptance() {
    viewModelScope.launch {
        _uiState.value = OnboardingUiState.Loading
        val result = onboardingRepository.acceptTerms()
        result.onSuccess { response ->
            if (response.activated == true) {
                // Load full profile summary
                val profileResult = onboardingRepository.getProfileSummary()
                profileResult.onSuccess { profile ->
                    // Save to local storage if needed
                    _uiState.value = OnboardingUiState.OnboardingComplete
                }.onFailure {
                    // Still complete even if profile load fails
                    _uiState.value = OnboardingUiState.OnboardingComplete
                }
            } else {
                setMissingFields(response.missing)
                proceedToNextStep()
            }
        }.onFailure { error ->
            _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to accept terms")
        }
    }
}
```

---

## 5️⃣ CONTENT - Home Bundle

### **Implementation** (Partially done in ContentRepository)

#### Quick Integration in HomeViewModel
```kotlin
// In HomeViewModel.kt
fun loadHomeContent(festivalSlug: String) {
    viewModelScope.launch {
        _uiState.value = HomeUiState.Loading
        
        val result = contentRepository.getAppHomeBundle(
            festivalSlug = festivalSlug,
            accessToken = sessionManager.getAccessToken()
        )
        
        result.onSuccess { bundle ->
            _homeData.value = bundle
            _uiState.value = HomeUiState.Success(bundle)
        }.onFailure { error ->
            _uiState.value = HomeUiState.Error(error.message ?: "Failed to load home")
        }
    }
}
```

---

## 6️⃣ FRIENDSHIPS - Search Festival Members

### **Quick Template**

```kotlin
// API Service
@POST("rest/v1/rpc/search_festival_members")
suspend fun searchFestivalMembers(
    @Header("Authorization") authorization: String,
    @Body request: SearchFestivalMembersRequest
): Response<List<FestivalMember>>

// Request Model
@Serializable
data class SearchFestivalMembersRequest(
    val p_festival_id: String,
    val p_query: String,
    val p_limit: Int = 20
)

// Repository Method
suspend fun searchFestivalMembers(
    festivalId: String,
    query: String,
    limit: Int = 20
): Result<List<FestivalMember>> {
    // Similar pattern to other methods
}
```

---

## ✅ **TESTING CHECKLIST FOR EACH ENDPOINT**

```kotlin
// Template for unit tests
@Test
fun testSaveProfileName_Success() {
    // Arrange
    val mockResponse = OnboardingResponse(
        saved = true,
        activated = false,
        status = "onboarding",
        missing = listOf("terms_acceptance")
    )
    
    // Act
    val result = runBlocking {
        repository.saveProfileName("John", "Doe")
    }
    
    // Assert
    assertTrue(result.isSuccess)
    assertEquals("John", result.getOrNull()?.username)
}

@Test
fun testSaveProfileName_Error() {
    // Similar pattern for error cases
}
```

---

## 📱 **UI COMPONENT EXAMPLES**

### **Profile Name Screen**
```kotlin
@Composable
fun ProfileNameScreen(
    formState: OnboardingFormState?,
    onFirstNameChange: (String) -> Unit = {},
    onLastNameChange: (String) -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("What's your legal name?", style = MaterialTheme.typography.headlineMedium)
        
        OutlinedTextField(
            value = formState?.legalFirstName ?: "",
            onValueChange = onFirstNameChange,
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = formState?.legalLastName ?: "",
            onValueChange = onLastNameChange,
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = onContinueClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}
```

### **Avatar Upload Screen**
```kotlin
@Composable
fun AvatarUploadScreen(
    onImageSelected: (File) -> Unit = {},
    onSkipClick: () -> Unit = {}
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = File(context.cacheDir, "avatar.jpg")
            // Copy uri to file
            onImageSelected(file)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Upload Photo")
        }
        Button(onClick = onSkipClick) {
            Text("Skip")
        }
    }
}
```

---

## 🔗 **API ENDPOINT MAPPING**

| Feature | Method | Endpoint | Status |
|---------|--------|----------|--------|
| Save Name | POST | `/functions/v1/save-profile-name` | ❌ Missing |
| Upload Avatar | POST | `/functions/v1/upload-avatar` | ❌ Missing |
| Get Avatar URL | GET | `/functions/v1/avatar-url` | ❌ Missing |
| Profile Summary | GET | `/functions/v1/profile-summary` | ⚠️ Partial |
| Home Bundle | GET | `/functions/v1/app-home-bundle` | ⚠️ Partial |
| Lineup | GET | `/functions/v1/content-lineup` | ❌ Missing |
| Schedule | GET | `/functions/v1/content-stage-schedule` | ❌ Missing |
| Map | GET | `/functions/v1/content-map` | ❌ Missing |
| Search Members | POST | `/rest/v1/rpc/search_festival_members` | ❌ Missing |
| Request Friend | POST | `/functions/v1/request-friendship` | ❌ Missing |

---

## 📚 **REFERENCE DOCUMENTATION**

See main API doc: `/Users/umasenthil/FastER/supabase_api.txt`

**Critical Sections:**
- Lines 317-380: Profile Name & Avatar
- Lines 381-430: Profile Summary
- Lines 431-620: Friendships
- Lines 621-900: Content Read Endpoints

---

**Last Updated:** 2026-03-04  
**Next Steps:** Start with Profile Summary integration (easiest), then Home Bundle
