# 📋 Android Kotlin Onboarding Flow - API Implementation Checklist

**Analysis Date:** March 4, 2026  
**Status:** Comprehensive Review Complete  
**Project:** FastER Festival Mobile App

---

## ✅ **CURRENTLY IMPLEMENTED (7/14 API Endpoints)**

### **Authentication Layer** ✅
- ✅ `POST /auth/v1/signup` - Email signup
- ✅ `POST /auth/v1/token?grant_type=password` - Email login
- ✅ `POST /auth/v1/verify` - Verify OTP (email/phone)
- ✅ `POST /auth/v1/recover` - Password recovery request
- ✅ `POST /auth/v1/logout` - User signout

### **Onboarding Flow - Step Endpoints** ✅
1. ✅ **Step 1: Username** - `POST /functions/v1/save-username`
2. ✅ **Step 2-4: Demographics** - `POST /functions/v1/save-demographics`
   - Date of Birth
   - Race/Ethnicity
   - Gender Identity
3. ✅ **Step 5: Emergency Contact** - `POST /functions/v1/save-emergency-contact`
4. ✅ **Step 6: Wristband** - Handled within save-demographics
5. ✅ **Step 7: Terms & Conditions** - `POST /functions/v1/accept-terms`

### **Infrastructure Endpoints** ✅
- ✅ `POST /rest/v1/rpc/ensure_festival_onboarding` - Initialize onboarding state

---

## ❌ **MISSING API INTEGRATIONS (7/14 Endpoints)**

### **1. PROFILE MANAGEMENT ENDPOINTS**

#### **❌ MISSING: Save Full Legal Name**
```
POST /functions/v1/save-profile-name
Headers:
  Authorization: Bearer <access_token>
  apikey: <SUPABASE_ANON_KEY>
Body:
{
  "legal_first_name": "John",
  "legal_last_name": "Doe"
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Users cannot save legal names separately
**Recommendation:** Add optional legal name fields to demographics screen OR create dedicated profile name screen

---

#### **❌ MISSING: Upload Profile Avatar**
```
POST /functions/v1/upload-avatar
Headers:
  Authorization: Bearer <access_token>
  apikey: <SUPABASE_ANON_KEY>
Body: multipart/form-data
  file: image file (required, max 5MB)
  allowed types: image/jpeg, image/png, image/webp
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Users cannot upload profile pictures during onboarding
**Recommendation:** Add optional avatar upload screen after demographics

---

#### **❌ MISSING: Get Profile Avatar URL**
```
GET /functions/v1/avatar-url
Headers:
  Authorization: Bearer <access_token>
  apikey: <SUPABASE_ANON_KEY>
Response:
{
  "ok": true,
  "avatar_path": "avatars/<user_id>/...",
  "signed_avatar_url": "https://...",
  "signed_avatar_url_expires_in_seconds": 60
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Cannot retrieve user avatar for profile display
**Recommendation:** Implement in profile loading post-onboarding

---

#### **❌ MISSING: Profile Summary (Get All Profile Data)**
```
GET /functions/v1/profile-summary
Headers:
  Authorization: Bearer <access_token>
  apikey: <SUPABASE_ANON_KEY>
Response:
{
  "ok": true,
  "profile": { ... },
  "demographics": { ... },
  "emergency_contacts": [ ... ],
  "signed_avatar_url": "...",
  "terms": { "accepted_at": "...", ... },
  "context": { "festival_id": "..." }
}
```
**Status in Code:** PARTIALLY IMPLEMENTED
  - Model exists: `ProfileSummaryResponse.kt`
  - API interface: NOT IN ONBOARDING_API_SERVICE
  - Usage: NOT CALLED
**Impact:** Cannot load all profile data in single call post-onboarding
**Recommendation:** Add to OnboardingApiService, call after onboarding completion

---

### **2. FRIENDSHIP & DISCOVERY ENDPOINTS**

#### **❌ MISSING: Ensure Festival Membership (Pre-step)**
```
POST /rest/v1/rpc/ensure_festival_onboarding
(Already implemented, but used only at start)
```
**Status in Code:** ✅ IMPLEMENTED

#### **❌ MISSING: Search Festival Members**
```
POST /rest/v1/rpc/search_festival_members
Body:
{
  "p_festival_id": "<festival_uuid>",
  "p_query": "john",
  "p_limit": 20
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Users cannot discover other festival attendees
**Recommendation:** Add as post-onboarding feature (not critical for onboarding)

---

#### **❌ MISSING: Request Friendship**
```
POST /functions/v1/request-friendship
Body:
{
  "festival_id": "<festival_uuid>",
  "target_festival_member_id": "<festival_memberships.id>"
  // OR
  "addressee_user_id": "<auth_user_uuid>"
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Users cannot add friends
**Recommendation:** Post-onboarding feature

---

#### **❌ MISSING: Respond to Friendship Request**
```
POST /functions/v1/respond-friendship
Body:
{
  "friendship_id": "<friendship_uuid>",
  "action": "accept|reject|block|unblock"
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Cannot manage friendship requests
**Recommendation:** Post-onboarding feature

---

#### **❌ MISSING: Read Friendships**
```
GET /rest/v1/friendships
  ?select=*
  &festival_id=eq.<festival_uuid>
  &or=(requester_id.eq.<my_user_id>,addressee_id.eq.<my_user_id>)
  &order=created_at.desc
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Cannot list user's friendships
**Recommendation:** Post-onboarding feature

---

### **3. CONTENT READ ENDPOINTS (APP-FACING)**

#### **❌ MISSING: Home Content Bundle**
```
GET /functions/v1/content-home?festival_slug=<slug>
Headers:
  Authorization: Bearer <access_token> (optional)
Response:
{
  "ok": true,
  "festival": { ... },
  "announcements": [ ... ],
  "hero_carousel_items": [ ... ],
  "upcoming_events": [ ... ]
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Home screen cannot load festival content
**Recommendation:** Implement in HomeScreen/HomeViewModel

---

#### **❌ MISSING: Content Lineup (Artists)**
```
GET /functions/v1/content-lineup?festival_slug=<slug>
Response:
{
  "ok": true,
  "festival": { ... },
  "artists": [ ... ]
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Cannot display lineup/artists
**Recommendation:** Implement in LineupScreen

---

#### **❌ MISSING: Content Artist Detail**
```
GET /functions/v1/content-artist-detail?festival_slug=<slug>&artist_slug=<slug>
Response:
{
  "ok": true,
  "festival": { ... },
  "artist": { ... },
  "events": [ ... ]
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Cannot show artist detail pages
**Recommendation:** Implement in ArtistDetailScreen

---

#### **❌ MISSING: Content Stage Schedule**
```
GET /functions/v1/content-stage-schedule?festival_slug=<slug>
Response:
{
  "ok": true,
  "festival": { ... },
  "stages": [ ... ],
  "events": [ ... ]
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Cannot display schedule
**Recommendation:** Implement in ScheduleScreen

---

#### **❌ MISSING: Content Map**
```
GET /functions/v1/content-map?festival_slug=<slug>
Response:
{
  "ok": true,
  "festival": { ... },
  "venues": [ ... ]
}
```
**Status in Code:** NOT IMPLEMENTED
**Impact:** Cannot display festival map
**Recommendation:** Implement in MapScreen

---

#### **❌ MISSING: App Home Bundle**
```
GET /functions/v1/app-home-bundle?festival_slug=<slug>
Headers:
  If-None-Match: <etag> (optional)
Response:
{
  "schema_version": "1",
  "generated_at": "...",
  "festival": { ... },
  "modules": [ ... ],
  "ui_config": { ... }
}
Supports: 304 Not Modified + ETag caching
```
**Status in Code:** NOT IMPLEMENTED (partially in ContentRepository)
**Impact:** Cannot load home screen with server-driven UI config
**Recommendation:** Implement in HomeViewModel with ETag caching

---

### **4. EXPERIENCE & OFFLINE ENDPOINTS**

#### **✅ PARTIALLY IMPLEMENTED: Festival Experience Core**
- ❌ `GET /functions/v1/festival-experience-categories?festival_slug=<slug>`
- ❌ `GET /functions/v1/festival-experience-locations?festival_slug=<slug>&category=<key>`
- ❌ `GET /functions/v1/festival-experience-location?id=<location_id>`

**Status in Code:** Models exist, API interfaces created in ContentRepository, but NOT integrated into UI

---

#### **❌ MISSING: App Experience Bundle**
```
GET /functions/v1/app-experience-bundle?festival_slug=<slug>
Response includes: categories + critical_sets (medical, water, restrooms, stages)
```
**Status in Code:** NOT IMPLEMENTED

---

#### **❌ MISSING: Offline Bundle**
```
GET /functions/v1/offline-bundle?festival_slug=<slug>
Response: Full offline-capable bundle with header + categories + critical_sets
```
**Status in Code:** Partially in ContentRepository, not called

---

---

## 📊 **IMPLEMENTATION SUMMARY**

| Category | Implemented | Total | Status |
|----------|------------|-------|--------|
| **Auth & Signup** | 5 | 5 | ✅ COMPLETE |
| **Onboarding Steps** | 7 | 7 | ✅ COMPLETE |
| **Profile Management** | 0 | 4 | ❌ MISSING |
| **Friendships/Discovery** | 0 | 5 | ❌ MISSING |
| **Content Read (Frontend)** | 0 | 5 | ❌ MISSING |
| **Experience/Offline** | 0 | 3 | ❌ MISSING |
| **TOTAL** | **12** | **29** | **⚠️ 41% Complete** |

---

## 🚀 **PRIORITY IMPLEMENTATION ROADMAP**

### **PHASE 1: CRITICAL (Required for MVP)**
1. **Home Content** - `GET /functions/v1/content-home` + `GET /functions/v1/app-home-bundle`
2. **Profile Summary** - `GET /functions/v1/profile-summary` (load after onboarding)
3. **Lineup Content** - `GET /functions/v1/content-lineup` (for artists screen)

### **PHASE 2: HIGH PRIORITY (Next Sprint)**
1. **Save Profile Name** - `POST /functions/v1/save-profile-name`
2. **Avatar Upload** - `POST /functions/v1/upload-avatar`
3. **Avatar URL** - `GET /functions/v1/avatar-url`
4. **Stage Schedule** - `GET /functions/v1/content-stage-schedule`

### **PHASE 3: MEDIUM PRIORITY (Future Sprints)**
1. **Friendships** - All 5 friendship endpoints
2. **Experience/Offline** - Experience categories, locations, offline bundle
3. **Admin Content** - Write endpoints (if needed)

---

## 📝 **ONBOARDING FLOW CURRENT STATE**

### **Current 7-Step Flow** ✅
```
1. ✅ Login/Signup
2. ✅ Username Input
3. ✅ Date of Birth
4. ✅ Race/Ethnicity
5. ✅ Gender Identity
6. ✅ Emergency Contact
7. ✅ Wristband Pairing
8. ✅ Terms & Conditions
```

### **Recommendation: Extended Onboarding** (Optional)
```
9. ⚠️ Profile Name (Legal First/Last Name)
10. ⚠️ Profile Avatar (Photo Upload)
11. ⚠️ Profile Summary (Review & Confirm)
```

---

## 🔧 **TECHNICAL DEBT & ISSUES**

### **1. Wristband Code Validation**
- ❌ No wristband_code validation logic
- ⚠️ Currently accepts any non-empty string
- 📍 Line: `OnboardingViewModel.kt:502-530`

### **2. Emergency Contact Validation**
- ❌ Phone number E.164 format validation missing
- ❌ Relationship field not shown in UI but required in API
- 📍 Line: `OnboardingViewModel.kt:467-500`

### **3. Demographics Validation**
- ✅ DOB validation present
- ⚠️ Race/ethnicity multi-select not fully validated
- ⚠️ self_describe text field optional validation missing

### **4. API Error Handling**
- ✅ Basic error codes handled (400, 401, 409, 422)
- ⚠️ No retry logic for transient failures
- ⚠️ No timeout handling for slow networks

### **5. State Management**
- ✅ Proper coroutines usage
- ⚠️ No offline state caching
- ⚠️ No background sync for partial submissions

---

## 📋 **QUICK ACTION ITEMS**

### **Immediate (This Sprint)**
```kotlin
// 1. Add profile-summary endpoint to OnboardingApiService
@GET("functions/v1/profile-summary")
suspend fun getProfileSummary(
    @Header("Authorization") authorization: String
): Response<ProfileSummaryResponse>

// 2. Add repository method
suspend fun getProfileSummary(): Result<ProfileSummaryResponse>

// 3. Call after onboarding completion
// In OnboardingViewModel.proceedFromTermsAcceptance()
val profileResult = onboardingRepository.getProfileSummary()
```

### **Short Term (Next 2 Sprints)**
1. Implement home content endpoints in ContentRepository (already exists!)
2. Create HomeViewModel to use content endpoints
3. Add legal name fields to profile
4. Implement avatar upload/get endpoints

### **Medium Term (Sprint Planning)**
1. Friendship endpoints for social features
2. Experience/offline bundles for festival features

---

## ✅ **VALIDATION CHECKLIST**

Use this to verify each endpoint implementation:

```kotlin
// For each new endpoint, ensure:
[ ] API interface method in appropriate Service
[ ] Model classes (Request/Response) with proper @Serializable annotations
[ ] Repository suspend function returning Result<T>
[ ] Error handling for codes: 400, 401, 403, 404, 409, 422, 500
[ ] ViewModel integration with proper state updates
[ ] UI screen/composable binding
[ ] Loading state during API call
[ ] Error state with user-friendly message
[ ] Success state with data binding
[ ] Navigation to next screen on success
[ ] Retry logic for failures
```

---

## 📞 **SUPABASE API ENDPOINT REFERENCE**

**Base URL:** `https://<PROJECT_REF>.supabase.co`

**Always Include Headers:**
```kotlin
Authorization: Bearer <access_token>
apikey: <SUPABASE_ANON_KEY>
Content-Type: application/json
```

**Refer to:** `/Users/umasenthil/FastER/supabase_api.txt` (Lines 1-1518)

---

**Generated:** 2026-03-04  
**Next Review:** After Phase 1 Implementation  
**Maintainer:** FastER Development Team
