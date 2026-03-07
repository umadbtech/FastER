# 📋 COMPLETE MISSING API ENDPOINTS - MASTER LIST

**Analyzed Against:** `/Users/umasenthil/FastER/supabase_api.txt` (1518 lines)  
**Date:** March 4, 2026  
**Completeness:** 41% (12/29 endpoints implemented)

---

## MISSING ENDPOINTS BY CATEGORY

### 1. PROFILE MANAGEMENT (4 Missing)

#### 1.1 ❌ Save Full Legal Name
```
POST /functions/v1/save-profile-name
Feature: Allow users to save first and last name separately
Body: {
  "legal_first_name": "John",  // optional
  "legal_last_name": "Doe"     // optional
}
Response: {
  "saved": true,
  "legal_first_name": "John",
  "legal_last_name": "Doe",
  "activated": false,
  "status": "onboarding",
  "missing": [...]
}
Error Codes: 400, 401, 405, 500
Location in Spec: Line 328-365
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐ Easy (5-10 min)
File to Create: SaveProfileNameRequest model
Files to Modify: OnboardingApiService, OnboardingRepository
```

#### 1.2 ❌ Upload Profile Avatar
```
POST /functions/v1/upload-avatar
Feature: Upload user profile picture
Headers: Authorization, Content-Type: multipart/form-data
Body: {
  file: image/jpeg, image/png, image/webp (max 5MB)
}
Response: {
  "saved": true,
  "avatar_path": "avatars/<user_id>/avatar_...",
  "signed_avatar_url": "https://...",
  "signed_avatar_url_expires_in_seconds": 60
}
Error Codes: 400, 401, 413, 415, 500
Location in Spec: Line 366-395
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (30-45 min)
Note: Requires multipart/form-data handling
Files to Modify: OnboardingApiService, OnboardingRepository
```

#### 1.3 ❌ Get Profile Avatar URL
```
GET /functions/v1/avatar-url
Feature: Retrieve signed URL for user's avatar image
Headers: Authorization
Response: {
  "ok": true,
  "avatar_path": "avatars/<user_id>/...",
  "signed_avatar_url": "https://...",
  "signed_avatar_url_expires_in_seconds": 60
}
Also Returns (if no avatar): {
  "ok": true,
  "avatar_path": null,
  "signed_avatar_url": null,
  "signed_avatar_url_expires_in_seconds": null
}
Error Codes: 401, 405, 500
Location in Spec: Line 396-430
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐ Easy (10 min)
Files to Modify: OnboardingApiService, OnboardingRepository
```

#### 1.4 ⚠️ Get Profile Summary
```
GET /functions/v1/profile-summary
Feature: Load all profile data in single call
Headers: Authorization
Response: {
  "ok": true,
  "profile": {
    "user_id": "...",
    "username": "...",
    "legal_first_name": "...",
    "legal_last_name": "...",
    "avatar_path": "...",
    "is_minor": true/false,
    "updated_at": "..."
  },
  "demographics": { ...profile_demographics row... },
  "emergency_contacts": [ ...array... ],
  "signed_avatar_url": "...",
  "signed_avatar_url_expires_in_seconds": 60,
  "terms": {
    "required_version": "v1.0",
    "accepted_version": "v1.0",
    "accepted_at": "...",
    "complete": true/false
  },
  "context": {
    "festival_id": "..."
  }
}
Error Codes: 401, 405, 500
Location in Spec: Line 431-480
Current Status in Code: ⚠️ Model exists (ProfileSummaryResponse.kt)
                        API interface NOT added
                        Repository method NOT added
Difficulty: ⭐ Easy (5 min)
Priority: 🔴 CRITICAL - Must call after onboarding completion
Files to Modify: OnboardingApiService, OnboardingRepository
```

---

### 2. CONTENT - HOME SCREEN (2 Missing)

#### 2.1 ❌ Get Home Content
```
GET /functions/v1/content-home?festival_slug=<slug>
Feature: Load home screen content (announcements, carousel, events)
Headers: Authorization (optional), apikey
Query Params: festival_slug (required)
Response: {
  "ok": true,
  "festival": {
    "id": "...",
    "slug": "floydfest-26",
    "name": "FloydFest 26",
    "timezone": "America/New_York",
    "starts_at": "2026-07-22T16:00:00+00:00",
    "ends_at": "2026-07-27T03:00:00+00:00",
    "description": "...",
    "status": "published",
    "visibility": "public"
  },
  "announcements": [ ...array... ],
  "hero_carousel_items": [ ...array... ],
  "upcoming_events": [ ...array... ]
}
Error Codes: 400, 404, 500
Location in Spec: Line 481-530
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (30 min for integration)
Priority: 🔴 CRITICAL
Files to Modify: ContentRepository, HomeViewModel
```

#### 2.2 ⚠️ Get App Home Bundle
```
GET /functions/v1/app-home-bundle?festival_slug=<slug>
Feature: Complete home screen bundle with server-driven UI config
Headers: Authorization (optional), If-None-Match (optional)
Special: Supports ETag caching (304 Not Modified)
Response: {
  "schema_version": "1",
  "generated_at": "...",
  "festival": { ...header fields... },
  "modules": [
    {
      "key": "hero_carousel|announcements|upcoming_events|sponsors|perks|alerts",
      "enabled": true/false,
      "ttl_seconds": 30-180,
      "updated_at": "...",
      "data": []
    }
  ],
  "ui_config": {
    "tiles": [
      { "key": "festival_experience|lineup_schedule|event_safety|faq", 
        "enabled": true/false, 
        "order": 1 }
    ],
    "module_order": [ "hero_carousel", "announcements", "upcoming_events", ... ]
  }
}
Cache Behavior: 304 Not Modified when If-None-Match matches ETag
Error Codes: 400, 404, 500
Location in Spec: Line 531-640
Current Status in Code: ⚠️ Partially in ContentRepository (lines 328-375)
                        Models exist, API methods exist
                        NOT called from HomeViewModel
Difficulty: ⭐ Easy (30 min to integrate)
Priority: 🔴 CRITICAL - Main home screen endpoint
Files to Modify: HomeViewModel, HomeScreen
```

---

### 3. CONTENT - FEATURES (3 Missing)

#### 3.1 ❌ Get Lineup Content (Artists)
```
GET /functions/v1/content-lineup?festival_slug=<slug>
Feature: Load festival lineup with all artists
Response includes:
- festival metadata
- artists[] with: id, name, slug, image_url, genre[]
Error Codes: 400, 404, 500
Location in Spec: Line 641-675
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (45 min)
Priority: 🟠 HIGH
```

#### 3.2 ❌ Get Stage Schedule
```
GET /functions/v1/content-stage-schedule?festival_slug=<slug>
Feature: Load festival schedule with stages and events
Response includes:
- festival metadata
- stages[] array
- events[] array
Error Codes: 400, 404, 500
Location in Spec: Line 676-700
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (45 min)
Priority: 🟠 HIGH
```

#### 3.3 ❌ Get Map Content
```
GET /functions/v1/content-map?festival_slug=<slug>
Feature: Load festival map with venues and POIs
Response includes:
- festival metadata
- venues[] array
Error Codes: 400, 404, 500
Location in Spec: Line 701-725
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (45 min)
Priority: 🟠 HIGH
```

#### 3.4 ❌ Get Artist Detail
```
GET /functions/v1/content-artist-detail
  ?festival_slug=<slug>&artist_slug=<artist_slug>
Feature: Load detailed artist information
Response includes:
- artist metadata (bio, image, etc)
- artist events[] array
Error Codes: 400, 404, 500
Location in Spec: Line 726-760
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (45 min)
Priority: 🟠 HIGH
```

---

### 4. FRIENDSHIPS / DISCOVERY (5 Missing)

#### 4.1 ❌ Search Festival Members
```
POST /rest/v1/rpc/search_festival_members
Feature: Discover other festival attendees by name
Body: {
  "p_festival_id": "<uuid>",
  "p_query": "john",
  "p_limit": 20
}
Response: Array of festival members with username, friendship status
Error Codes: 400, 401, 404, 500
Location in Spec: Line 861-880
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (45 min)
Priority: 🟡 MEDIUM (post-launch feature)
```

#### 4.2 ❌ Request Friendship
```
POST /functions/v1/request-friendship
Feature: Send friend request to another user
Body: {
  "festival_id": "...",
  "target_festival_member_id": "..." 
  // OR "addressee_user_id": "..."
}
Response: { "saved": true, "friendship_id": "...", ... }
Error Codes: 400, 401, 404, 409 (already blocked)
Location in Spec: Line 881-920
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (45 min)
Priority: 🟡 MEDIUM
```

#### 4.3 ❌ Respond to Friendship Request
```
POST /functions/v1/respond-friendship
Feature: Accept/reject/block friend requests
Body: {
  "friendship_id": "...",
  "action": "accept|reject|block|unblock"
}
Response: Updated friendship record
Error Codes: 400, 401, 404, 409
Location in Spec: Line 921-960
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (45 min)
Priority: 🟡 MEDIUM
```

#### 4.4 ❌ List Friendships
```
GET /rest/v1/friendships
  ?select=*
  &festival_id=eq.<festival_uuid>
  &or=(requester_id.eq.<my_user_id>,addressee_id.eq.<my_user_id>)
  &order=created_at.desc
Feature: Load user's friend list
Response: Array of friendship records with status
Error Codes: 400, 401, 500
Location in Spec: Line 961-985
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐ Easy (20 min)
Priority: 🟡 MEDIUM
```

#### 4.5 ⚠️ Realtime Friendships Subscription
```
Realtime subscription to public.friendships
Feature: Listen for incoming friend requests
Trigger: INSERT on request, UPDATE on accept/reject/block
Error handling: Process only if requester_id==me || addressee_id==me
Location in Spec: Line 986-1000
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐⭐ Hard (1-2 hours)
Priority: 🟡 MEDIUM (can be async socket feature)
```

---

### 5. EXPERIENCE & OFFLINE (3 Missing)

#### 5.1 ❌ Get Experience Categories
```
GET /functions/v1/festival-experience-categories?festival_slug=<slug>
Feature: Load all experience categories (medical, water, etc)
Response: {
  "schema_version": "1",
  "festival_id": "...",
  "categories": [
    {
      "key": "medical|water|restrooms|stages|food|merch",
      "label": "Medical",
      "sort_order": 1,
      "icon": "medical",
      "preload_priority": 1
    }
  ]
}
Error Codes: 400, 404, 500
Location in Spec: Line 1001-1035
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐ Easy (20 min)
Priority: 🟡 MEDIUM
```

#### 5.2 ❌ Get Experience Locations by Category
```
GET /functions/v1/festival-experience-locations
  ?festival_slug=<slug>&category=<key>
Feature: Load all locations for a category
Response: Array of location records with coords, hours, phone, etc
Error Codes: 400, 404, 500
Location in Spec: Line 1036-1090
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐⭐ Medium (30 min)
Priority: 🟡 MEDIUM
```

#### 5.3 ❌ Get Single Experience Location Detail
```
GET /functions/v1/festival-experience-location?id=<location_id>
Feature: Load detailed info for single location
Response: Single location object with all fields
Error Codes: 400, 404, 500
Location in Spec: Line 1091-1140
Current Status in Code: NOT IMPLEMENTED
Difficulty: ⭐ Easy (15 min)
Priority: 🟡 MEDIUM
```

#### 5.4 ⚠️ Get App Experience Bundle
```
GET /functions/v1/app-experience-bundle?festival_slug=<slug>
Feature: Complete experience bundle with critical locations
Response includes:
- categories[]
- critical_sets (medical, water, restrooms, stages)
Error Codes: 400, 404, 500
Location in Spec: Line 1141-1200
Current Status in Code: ⚠️ Partially in ContentRepository
                        Models exist but NOT integrated
Difficulty: ⭐⭐ Medium (30 min to integrate)
Priority: 🟡 MEDIUM
```

#### 5.5 ❌ Get Offline Bundle
```
GET /functions/v1/offline-bundle?festival_slug=<slug>
Feature: Complete offline-capable bundle for app
Includes: Header, categories, critical_sets, cache hints
Response: {
  "schema_version": "1",
  "generated_at": "...",
  "festival": { ...header... },
  "categories": [ ... ],
  "critical_sets": { "medical": [...], "water": [...], ... },
  "cache_hints": { "ttl_seconds": 3600, ... }
}
Supports: ETag/304 caching
Error Codes: 400, 404, 500
Location in Spec: Line 1201-1280
Current Status in Code: ⚠️ Partially in ContentRepository
                        NOT integrated
Difficulty: ⭐⭐ Medium (30 min to integrate)
Priority: 🟡 MEDIUM (offline support is later feature)
```

---

## 📊 SUMMARY TABLE

| Category | Endpoint | Method | Missing | Priority |
|----------|----------|--------|---------|----------|
| **Profile** | Save Name | POST | ❌ | HIGH |
| | Upload Avatar | POST | ❌ | HIGH |
| | Get Avatar URL | GET | ❌ | HIGH |
| | Profile Summary | GET | ⚠️ | 🔴 CRITICAL |
| **Home** | Home Content | GET | ❌ | 🔴 CRITICAL |
| | Home Bundle | GET | ⚠️ | 🔴 CRITICAL |
| **Features** | Lineup | GET | ❌ | HIGH |
| | Schedule | GET | ❌ | HIGH |
| | Map | GET | ❌ | HIGH |
| | Artist Detail | GET | ❌ | HIGH |
| **Social** | Search Members | POST | ❌ | MEDIUM |
| | Request Friend | POST | ❌ | MEDIUM |
| | Respond Friend | POST | ❌ | MEDIUM |
| | List Friends | GET | ❌ | MEDIUM |
| | Realtime Subs | RTC | ❌ | MEDIUM |
| **Experience** | Categories | GET | ❌ | MEDIUM |
| | Locations | GET | ❌ | MEDIUM |
| | Location Detail | GET | ❌ | MEDIUM |
| | Bundle | GET | ⚠️ | MEDIUM |
| | Offline Bundle | GET | ⚠️ | MEDIUM |
| **TOTAL** | | | **17/22** | |

---

## 🎯 IMPLEMENTATION ORDER

### PHASE 1: UNBLOCK HOME SCREEN (3 endpoints, ~1 hour)
1. Profile Summary GET (5 min) - Model exists
2. Home Bundle GET integration (30 min) - Already in ContentRepository
3. Home Content GET (20 min) - New endpoint

### PHASE 2: COMPLETE PROFILE (4 endpoints, ~2 hours)
1. Save Legal Name POST (10 min)
2. Avatar Upload POST (30 min)
3. Avatar URL GET (10 min)
4. Profile UI Components (40 min)

### PHASE 3: FEATURE SCREENS (4 endpoints, ~3 hours)
1. Lineup GET (45 min)
2. Schedule GET (45 min)
3. Map GET (45 min)
4. Artist Detail GET (45 min)

### PHASE 4: SOCIAL FEATURES (5 endpoints, ~4 hours)
1-5: Friendship endpoints

### PHASE 5: EXPERIENCE & OFFLINE (3 endpoints, ~2 hours)
1-3: Experience endpoints + offline bundle

---

## 🚀 CRITICAL PATH

```
To launch MVP today:
  Profile Summary (5 min) ✅
  ↓
  Home Bundle (30 min) ✅  
  ↓
  Profile Screen (45 min) ✅
  ↓
  USERS CAN SEE HOME + PROFILE
  
Total: 80 minutes
```

---

**Analysis Complete**  
**Next Step:** Start with Phase 1 (Profile Summary + Home Bundle)  
**Estimated Total Time:** 15-20 hours to implement all 17 missing endpoints  

For code templates, see: `/Users/umasenthil/FastER/MISSING_API_IMPLEMENTATIONS.md`
