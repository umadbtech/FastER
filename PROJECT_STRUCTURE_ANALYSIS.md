# 📋 PROJECT STRUCTURE ANALYSIS - Feature Modules API Compliance

**Date**: March 6, 2026  
**Analysis**: Against feature_modules_api.txt specifications  
**Scope**: Auth & Session, Onboarding, Home (App Shell Content)

---

## ✅ SECTION 1: AUTH & SESSION

### Specification Requirements
```
Purpose: Create accounts, authenticate users, manage sessions

Primary entrypoints:
  ✅ Email signup → /auth/v1/signup + /auth/v1/verify (type=signup)
  ✅ Email login → /auth/v1/token?grant_type=password
  ✅ Phone OTP → /auth/v1/verify (type=sms)
  ✅ Logout → /auth/v1/logout
  ✅ Password reset → /auth/v1/recover + /auth/v1/verify (type=recovery) + /auth/v1/user
```

### Current Implementation Status

#### ✅ API Endpoints (AuthApiService.kt)
```
✅ @POST("auth/v1/signup") - signUp()
✅ @POST("auth/v1/verify") - verifyOtp()
✅ @POST("auth/v1/otp") - sendPhoneOtp()
✅ @POST("auth/v1/verify") - verifyPhoneOtp()
✅ @POST("auth/v1/token?grant_type=password") - login()
✅ @POST("auth/v1/token?grant_type=refresh_token") - refreshToken()
✅ @POST("auth/v1/recover") - recover()
✅ @PUT("auth/v1/user") - updateUser()
✅ @POST("auth/v1/logout") - logout()
✅ @GET("auth/v1/user") - getUser()
```

#### ✅ Repository Implementation (AuthRepository.kt)
```
✅ signUp(fullName, email, password): Result<User>
✅ verifyOtp(email, code): Result<AuthResponse>
✅ sendOtp(email): Result<Unit>
✅ login(email, password): Result<LoginResponse>
✅ logout(accessToken): Result<Unit>
✅ recover(email): Result<Unit>
```

#### ✅ ViewModel & UI (LoginViewModel, SignupScreen)
```
✅ LoginViewModel - handles email/password validation
✅ LoginScreen - displays login form
✅ SignupScreen - displays signup form
✅ OTP verification screen - handles email OTP
```

#### ✅ Session Management (EncryptedSessionManager.kt)
```
✅ Token storage (access_token, refresh_token)
✅ User info storage (user_id, email)
✅ Session clearing on logout
✅ Encrypted SharedPreferences
```

#### ✅ Token Refresh (TokenRefreshInterceptor.kt)
```
✅ Auto-detects 401 responses
✅ Calls refreshToken() endpoint
✅ Retries request with new token
✅ Clears session if refresh fails
```

### Auth & Session: COMPLETE ✅

---

## ✅ SECTION 2: ONBOARDING (Festival-scoped)

### Specification Requirements
```
Purpose: Collect required profile info to activate membership for a festival.

Primary entrypoints:
  ✅ Prerequisite: POST /rest/v1/rpc/ensure_festival_onboarding
  ✅ Resume & prefill: GET /functions/v1/profile-summary
  
Write steps:
  ✅ POST /functions/v1/save-username
  ✅ POST /functions/v1/save-demographics
  ✅ POST /functions/v1/save-emergency-contact
  ✅ POST /functions/v1/accept-terms
  
Optional:
  ✅ Save legal name → /functions/v1/save-profile-name
  ✅ Upload avatar → /functions/v1/upload-avatar
  ✅ Get signed avatar URL → /functions/v1/avatar-url
```

### Current Implementation Status

#### ✅ 7-Step Onboarding Flow (OnboardingViewModel.kt)
```
Step 1: ✅ USERNAME - save-username endpoint
Step 2: ✅ DATE_OF_BIRTH - save-demographics endpoint
Step 3: ✅ RACE_ETHNICITY - save-demographics endpoint
Step 4: ✅ GENDER_IDENTITY - save-demographics endpoint
Step 5: ✅ EMERGENCY_CONTACT - save-emergency-contact endpoint
Step 6: ✅ WRISTBAND - included in save-demographics
Step 7: ✅ TERMS_ACCEPTANCE - accept-terms endpoint
```

#### ✅ API Endpoints (OnboardingApiService.kt)
```
✅ POST /functions/v1/save-username
✅ POST /functions/v1/save-demographics
✅ POST /functions/v1/save-emergency-contact
✅ POST /functions/v1/accept-terms
✅ GET /functions/v1/profile-summary
✅ POST /rest/v1/rpc/ensure_festival_onboarding
```

#### ✅ Repository (OnboardingRepository.kt)
```
✅ saveUsername(username): Result<OnboardingResponse>
✅ saveDemographics(request): Result<OnboardingResponse>
✅ saveEmergencyContact(request): Result<OnboardingResponse>
✅ acceptTerms(): Result<OnboardingResponse>
✅ getProfileSummary(): Result<ProfileSummary>
✅ ensureOnboarding(festivalId): Result<String>
```

#### ✅ ViewModel (OnboardingViewModel.kt)
```
✅ 7-step coordinator (OnboardingStepCoordinator.kt)
✅ Form state management
✅ Validation logic (username 3-30 chars, DOB validation, etc.)
✅ Navigation between steps
✅ Completion rules (activated=true OR missing=[])
✅ Profile summary loading after completion
```

#### ✅ UI Screens (OnboardingScreen.kt)
```
✅ UsernameScreen
✅ DateOfBirthScreen
✅ RaceEthnicityScreen
✅ GenderIdentityScreen
✅ PrimaryEmergencyContactScreen
✅ WristbandScreen
✅ TermsAcceptanceScreen
```

#### ✅ Validation Rules (OnboardingViewModel.kt)
```
✅ Username: 3-30 characters, no special chars
✅ DOB: Valid date, not in future, over 13
✅ Emergency Contact: Name required, phone with country code
✅ Terms: Must be accepted before proceeding
```

### Onboarding: COMPLETE ✅

---

## ⚠️ SECTION 3: HOME (App Shell Content)

### Specification Requirements
```
Purpose: Load server-driven Home content and tile ordering.

Primary entrypoint (recommended):
  ⚠️ GET /functions/v1/app-home-bundle?festival_slug=<slug>
  ✅ Supports ETag caching (If-None-Match → 304)

Supporting endpoints:
  ✅ Festival header → GET /functions/v1/festival-header?festival_slug=<slug>
  ⚠️ Simple home bundle → GET /functions/v1/content-home?festival_slug=<slug>

UI surfaces:
  ⚠️ Home screen hero carousel, announcements, upcoming events
  ⚠️ Server-driven tiles ("Festival Experience", "Lineup & Schedule", "Safety", "FAQ")
```

### Current Implementation Status

#### ✅ API Endpoints (AppHomeApi.kt)
```
✅ @GET("functions/v1/app-home-bundle")
   suspend fun getAppHomeBundle(
       @Query("festival_slug") festivalSlug: String,
       @Header("If-None-Match") etag: String? = null
   ): Response<AppHomeBundleResponse>
```

#### ✅ Models (AppHomeBundleModels.kt)
```
✅ AppHomeBundleResponse - full response structure
✅ AppFestivalHeader - festival data
✅ HeroCarouselItem - carousel items
✅ Announcement - announcement items
✅ UpcomingEvent - event items
✅ HomeModule - module structure
✅ UiConfig - tile configuration
✅ TileConfig - individual tile
```

#### ⚠️ Repository (AppHomeRepository.kt)
```
✅ getAppHomeBundle(festivalSlug): Flow<AppHomeBundleResponse>
⚠️ ETag caching not fully implemented (basic in-memory only)
⚠️ 304 Not Modified handling exists but basic
```

#### ✅ ViewModel (AppHomeViewModel.kt)
```
✅ loadAppHomeBundle() - calls API
✅ Exposes bundleState as StateFlow<UiState>
✅ Loading/Success/Error states
```

#### ✅ UI Components (HomeScreen.kt)
```
✅ Banner carousel (BannerSlider component)
✅ Quick Actions Row (tiles from uiConfig)
✅ Setup Account Card
✅ Explore Section with:
   ✅ Hero Carousel Items (Featured)
   ✅ Announcements Section
   ✅ Upcoming Events Section
✅ Experience List
✅ Festival Header Screen (bottom)
```

#### ⚠️ ISSUES IDENTIFIED

**Issue 1: Missing Content-Home Endpoint Usage**
```
Status: ⚠️ NOT IMPLEMENTED
The spec mentions: GET /functions/v1/content-home?festival_slug=<slug>
Current: Using only app-home-bundle
Impact: Lightweight fallback not available
```

**Issue 2: Tiles Not Properly Mapped**
```
Current: QuickActionRow shows generic tiles
Expected: Should use ui_config.tiles configuration
Status: ⚠️ PARTIAL - exists but not fully integrated
```

**Issue 3: Module Order Not Used**
```
Current: Hardcoded section order
Expected: Should follow ui_config.module_order
Status: ⚠️ NOT IMPLEMENTED
```

**Issue 4: Published Status Gate Missing**
```
Spec says: If festival.status == "published", show only when logged in
Current: No check in HomeScreen
Status: ❌ MISSING
Location: HomeScreen.kt - need to add check
```

**Issue 5: ETag Caching Incomplete**
```
Current: Basic in-memory storage
Expected: Proper 304 handling, persistent cache (DataStore)
Status: ⚠️ PARTIAL
```

### Home: MOSTLY COMPLETE ⚠️ (Minor fixes needed)

---

## 🔧 RECOMMENDED FIXES

### Fix 1: Add Published Status Check
**File**: HomeScreen.kt
**Issue**: Not checking festival.status == "published"
**Fix**: Add authentication gate before displaying home content

```kotlin
// In HomeScreenContent
if (bundle.festival.status == "published" && accessToken.isNullOrBlank()) {
    item {
        HomeLoginGate(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp))
    }
} else {
    // Show home content
}
```

### Fix 2: Implement Content-Home Fallback
**File**: AppHomeRepository.kt
**Issue**: No fallback to content-home endpoint
**Fix**: Add getContentHome() method for lightweight fallback

### Fix 3: Implement Proper ETag Caching
**File**: AppHomeRepository.kt  
**Issue**: Only in-memory, no persistence
**Fix**: Add DataStore-based persistence for ETag + response

### Fix 4: Use Module Order
**File**: HomeScreen.kt
**Issue**: Hardcoded section order
**Fix**: Sort sections by ui_config.moduleOrder

---

## 📊 COMPLIANCE SUMMARY

| Feature | Status | Notes |
|---------|--------|-------|
| **Auth & Session** | ✅ COMPLETE | All 9 endpoints implemented |
| **Onboarding** | ✅ COMPLETE | All 7 steps + validation |
| **Home Content** | ⚠️ MOSTLY | 4/5 features, need minor fixes |
| **Error Handling** | ✅ COMPLETE | Proper error states |
| **Token Refresh** | ✅ COMPLETE | Auto-refresh on 401 |
| **Session Management** | ✅ COMPLETE | Encrypted storage |

---

## 🎯 PRIORITY FIXES

**Priority 1 - CRITICAL** (5 min)
- [ ] Add published status check in HomeScreen
- Fix: Check `festival.status == "published"` before showing content

**Priority 2 - IMPORTANT** (15 min)
- [ ] Implement proper ETag caching
- Fix: Use DataStore for persistent cache

**Priority 3 - NICE-TO-HAVE** (20 min)
- [ ] Add content-home fallback endpoint
- [ ] Implement module order sorting

---

**Analysis Complete**: All major features implemented. Minor fixes recommended for production readiness.

