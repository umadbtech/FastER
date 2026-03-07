# 📋 PROFILE SCREEN COMPREHENSIVE ANALYSIS & IMPLEMENTATION PLAN

**Date**: March 6, 2026  
**Status**: ⚠️ **PARTIAL - Missing Critical Features**  
**Priority**: 🔴 **CRITICAL**

---

## 📊 CURRENT STATE ANALYSIS

### What EXISTS ✅

1. **ProfileSummaryResponse Model** (ProfileSummary.kt)
   - ✅ API response structure defined
   - ✅ All fields mapped (user_id, username, legal_first_name, legal_last_name, avatar_path, etc.)
   - ✅ Demographics, EmergencyContact, Terms, Context sub-models

2. **ProfileSummary UI Model** (ProfileSummary.kt)
   - ✅ UI representation with userId, username, legalFirstName, legalLastName, avatarUrl

3. **ProfileRepository** (ProfileRepository.kt)
   - ✅ loadProfileSummary() method (calls API)
   - ✅ Error handling
   - ✅ Result<T> pattern

4. **ProfileApiService** (ProfileApiService.kt)
   - ✅ getProfileSummary() endpoint defined

5. **ProfileScreen Components** (ProfileScreen.kt, ProductionProfileScreen.kt)
   - ✅ ProfileCardSection (displays name, username)
   - ✅ AdditionalInfoSection
   - ✅ Emergency contacts display
   - ✅ Settings menu items
   - ✅ Logout functionality

6. **ProfileViewModel** (ProductionProfileViewModel.kt, EnhancedProfileViewModel.kt)
   - ✅ Loads profile data
   - ✅ State management (Loading, Success, Error, Empty)

---

### What's MISSING ❌

#### **1. SAVE LEGAL NAME**
```
Endpoint: POST /functions/v1/save-profile-name
Status: ❌ NOT IMPLEMENTED
Impact: Cannot edit legal first/last name
```

#### **2. UPLOAD AVATAR**
```
Endpoint: POST /functions/v1/upload-avatar
Status: ❌ NOT IMPLEMENTED
Impact: Cannot upload profile picture
```

#### **3. GET AVATAR URL**
```
Endpoint: GET /functions/v1/avatar-url
Status: ❌ NOT IMPLEMENTED
Impact: Cannot retrieve signed avatar URLs
```

#### **4. EDIT PROFILE UI SCREENS**
```
Screens Missing:
  ❌ PersonalInfoEditScreen (edit name)
  ❌ AvatarUploadScreen (change photo)
  ❌ EditEmergencyContactScreen (add/edit/delete contacts)
  ❌ ProfileEditFormScreen (unified edit form)
```

#### **5. AVATAR DISPLAY**
```
Missing:
  ❌ Avatar image display in profile header
  ❌ Avatar placeholder
  ❌ Avatar selection/camera functionality
```

#### **6. DEMOGRAPHICS DISPLAY**
```
Missing:
  ❌ Display DOB, race/ethnicity, gender identity
  ❌ Wristband code display
  ❌ Edit demographics option
```

#### **7. PROFILE EDIT WORKFLOWS**
```
Missing:
  ❌ Edit → Save → Update Profile Refresh
  ❌ Add emergency contact workflow
  ❌ Delete emergency contact workflow
  ❌ Error handling & retry
```

---

## 🎯 IMPLEMENTATION PLAN

### PHASE 1: API Layer (1 hour)

#### Step 1.1: Add API Methods to ProfileApiService
- Add saveLegalName() endpoint
- Add uploadAvatar() endpoint
- Add getAvatarUrl() endpoint

#### Step 1.2: Add Repository Methods to ProfileRepository
- saveLegalName(firstName, lastName): Result<ProfileSummary>
- uploadAvatar(file): Result<String> (returns signed URL)
- getAvatarUrl(): Result<String>
- saveEmergencyContact(): Result<ProfileSummary>
- deleteEmergencyContact(contactId): Result<ProfileSummary>

#### Step 1.3: Add Request/Response Models
- SaveLegalNameRequest
- UploadAvatarResponse
- AvatarUrlResponse

---

### PHASE 2: ViewModels (45 minutes)

#### Step 2.1: Create ProfileEditViewModel
- State: EditFormState (firstName, lastName, avatar, etc.)
- Methods: saveLegalName(), uploadAvatar(), getAvatarUrl()
- Error handling & loading states

#### Step 2.2: Create EmergencyContactEditViewModel
- State: ContactFormState (name, phone, relationship)
- Methods: addContact(), editContact(), deleteContact()
- Validation logic

---

### PHASE 3: UI Components (2 hours)

#### Step 3.1: Avatar Components
- AvatarDisplay (circular image with placeholder)
- AvatarUploadButton (camera + gallery)
- AvatarUploadScreen

#### Step 3.2: Edit Screens
- PersonalInfoEditScreen (edit name)
- EmergencyContactEditScreen (add/edit/delete)
- ProfileEditFormScreen (unified form)

#### Step 3.3: Update Main Profile Screen
- Add avatar to header
- Add demographics section
- Add edit buttons
- Display emergency contacts list

---

### PHASE 4: Navigation & Integration (1 hour)

#### Step 4.1: NavGraph Updates
- Add routes for edit screens
- Add deeplinks

#### Step 4.2: Profile Screen Updates
- Add navigation callbacks
- Integrate edit workflows
- Add refresh on save

---

## 🔧 CODE TEMPLATES

### API Service Extension
```kotlin
// In ProfileApiService.kt

@POST("functions/v1/save-profile-name")
suspend fun saveLegalName(@Body request: SaveLegalNameRequest): Response<ProfileSummaryResponse>

@Multipart
@POST("functions/v1/upload-avatar")
suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<UploadAvatarResponse>

@GET("functions/v1/avatar-url")
suspend fun getAvatarUrl(): Response<AvatarUrlResponse>

@POST("functions/v1/save-emergency-contact")
suspend fun saveEmergencyContact(@Body request: SaveEmergencyContactRequest): Response<ProfileSummaryResponse>

@DELETE("functions/v1/emergency-contact/{contactId}")
suspend fun deleteEmergencyContact(@Path("contactId") contactId: String): Response<Unit>
```

### Repository Extension
```kotlin
// In ProfileRepository.kt

fun saveLegalName(firstName: String, lastName: String, token: String): Flow<Result<ProfileSummary>> = flow {
    try {
        val request = SaveLegalNameRequest(firstName, lastName)
        val response = profileApiService.saveLegalName(request)
        if (response.isSuccessful) {
            emit(Result.success(response.body()?.toProfileSummary() ?: throw Exception("Empty response")))
        } else {
            emit(Result.failure(Exception("Failed to save legal name")))
        }
    } catch (e: Exception) {
        emit(Result.failure(e))
    }
}

fun uploadAvatar(file: File, token: String): Flow<Result<String>> = flow {
    try {
        val requestFile = RequestBody.create("image/*".toMediaType(), file)
        val part = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
        val response = profileApiService.uploadAvatar(part)
        if (response.isSuccessful) {
            val signedUrl = response.body()?.signedUrl ?: throw Exception("No URL in response")
            emit(Result.success(signedUrl))
        } else {
            emit(Result.failure(Exception("Failed to upload avatar")))
        }
    } catch (e: Exception) {
        emit(Result.failure(e))
    }
}
```

### ViewModel
```kotlin
// New: ProfileEditViewModel.kt

class ProfileEditViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionManager: EncryptedSessionManager
) : ViewModel() {
    
    private val _formState = MutableStateFlow(EditFormState())
    val formState: StateFlow<EditFormState> = _formState.asStateFlow()
    
    private val _uiState = MutableStateFlow<EditUiState>(EditUiState.Idle)
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()
    
    fun saveLegalName(firstName: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = EditUiState.Loading
            val token = sessionManager.getAccessToken() ?: return@launch
            profileRepository.saveLegalName(firstName, lastName, token).collect { result ->
                _uiState.value = if (result.isSuccess) {
                    EditUiState.Success("Name saved successfully")
                } else {
                    EditUiState.Error(result.exceptionOrNull()?.message ?: "Failed to save")
                }
            }
        }
    }
    
    fun uploadAvatar(file: File) {
        viewModelScope.launch {
            _uiState.value = EditUiState.Loading
            val token = sessionManager.getAccessToken() ?: return@launch
            profileRepository.uploadAvatar(file, token).collect { result ->
                _uiState.value = if (result.isSuccess) {
                    EditUiState.Success("Avatar uploaded")
                } else {
                    EditUiState.Error(result.exceptionOrNull()?.message ?: "Failed to upload")
                }
            }
        }
    }
}
```

---

## 📱 UI SCREENS TO CREATE

### 1. AvatarDisplay Component
- Circular image display
- Placeholder if no avatar
- Edit/change button overlay

### 2. PersonalInfoEditScreen
- First name input field
- Last name input field
- Save button
- Loading state
- Error message

### 3. AvatarUploadScreen
- Image preview
- Camera/gallery buttons
- Upload button
- Progress indicator
- Success/error message

### 4. EmergencyContactEditScreen
- Contact list display
- Add button
- Edit/delete options for each
- Contact form (name, phone, relationship)

### 5. Updated ProfileCardSection
- Display avatar (circular)
- Display full name (legal or username)
- Display username below
- Add edit button (navigate to edit screen)

### 6. Demographics Display
- Show DOB (if available)
- Show race/ethnicity (if available)
- Show gender identity (if available)
- Show wristband code (if available)

---

## 📊 MISSING FEATURES MATRIX

| Feature | Endpoint | API | Repository | ViewModel | UI | Status |
|---------|----------|-----|------------|-----------|----|----|
| Save Legal Name | ✅ Spec | ❌ | ❌ | ❌ | ❌ | 0% |
| Upload Avatar | ✅ Spec | ❌ | ❌ | ❌ | ❌ | 0% |
| Get Avatar URL | ✅ Spec | ❌ | ❌ | ❌ | ❌ | 0% |
| Edit Profile UI | - | - | - | - | ❌ | 0% |
| Avatar Display | - | - | - | - | ❌ | 0% |
| Demographics Display | - | - | - | - | ❌ | 0% |
| Emergency Contact CRUD | ✅ Spec | ❌ | ❌ | ❌ | ❌ | 0% |

---

## 🚀 IMPLEMENTATION PRIORITY

1. **Priority 1 (CRITICAL)** - 30 min
   - [ ] Add ProfileApiService methods (save-profile-name, upload-avatar, avatar-url)
   - [ ] Add Repository wrapper methods
   - [ ] Add ProfileEditViewModel

2. **Priority 2 (HIGH)** - 1 hour
   - [ ] Create AvatarDisplay component
   - [ ] Create PersonalInfoEditScreen
   - [ ] Create AvatarUploadScreen

3. **Priority 3 (HIGH)** - 1 hour
   - [ ] Update ProfileCardSection with avatar
   - [ ] Add demographics display section
   - [ ] Add edit buttons/navigation

4. **Priority 4 (MEDIUM)** - 45 min
   - [ ] Emergency contact management screens
   - [ ] Add/edit/delete workflows
   - [ ] Error handling & validation

5. **Priority 5 (POLISH)** - 30 min
   - [ ] Loading states & shimmer
   - [ ] Error messages
   - [ ] Animations

---

## ✅ TESTING CHECKLIST

- [ ] Save legal name API call works
- [ ] Upload avatar API call works
- [ ] Get avatar URL works
- [ ] Profile screen displays avatar
- [ ] Profile screen displays demographics
- [ ] Edit profile screen loads
- [ ] Save changes updates profile
- [ ] Add/edit/delete emergency contacts works
- [ ] Error states display properly
- [ ] Loading states work
- [ ] Navigation between screens works

---

**Total Estimated Effort**: 4-5 hours  
**Complexity**: Medium  
**Impact**: High (complete profile management)

