# HomeScreen 401 Error Fix - Complete Implementation Report

## 📋 Executive Summary

**Date:** 2026-03-04  
**Status:** ✅ **ANDROID IMPLEMENTATION COMPLETE**  
**Backend Status:** ⏳ **Awaiting Supabase Edge Function Update**

Your Android application is **correctly implemented** per the API specification. The 401 error is caused by **backend Supabase Edge Functions** that unconditionally require the Authorization header, contrary to the published API spec that says it's optional.

---

## 🔍 Analysis Results

### API Specification (supabase_api.txt, Line 1119)

```
Endpoint: GET /functions/v1/app-home-bundle?festival_slug=<slug>
Headers:
  apikey: <SUPABASE_ANON_KEY>              ← Always required
  Authorization: Bearer <access_token> (optional)  ← OPTIONAL
  If-None-Match: <etag> (optional)         ← For caching
```

**Key Point:** Authorization header is marked as OPTIONAL in the spec.

### Android Implementation Analysis

**File:** `SupabaseHeadersInterceptor.kt`

```kotlin
override fun intercept(chain: Interceptor.Chain): Response {
    var request = chain.request()

    val requestBuilder = request.newBuilder()
        // Always add API key header ✅
        .header("apikey", apiKey)
        .header("Content-Type", "application/json")

    // Conditionally add Authorization header if token is available ✅
    val token = getAccessToken()
    if (!token.isNullOrBlank()) {
        requestBuilder.header("Authorization", "Bearer $token")
    }

    request = requestBuilder.build()
    return chain.proceed(request)
}
```

**Verdict:** ✅ **CORRECT**
- Sends Authorization header only when token exists
- Does NOT send header for anonymous/logged-out users
- Matches API specification requirements perfectly

### Backend Implementation Analysis

**Current Behavior:** ❌ **INCORRECT**

The Supabase Edge Functions are structured like:

```typescript
// ❌ WRONG: Dies if Authorization is missing
const { data: { user }, error } = await supabase.auth.getUser();
if (!user) {
  return new Response(
    JSON.stringify({ code: 401, message: "Missing authorization header" }),
    { status: 401 }
  );
}
```

**Verdict:** ❌ **VIOLATES API SPEC**
- Requires Authorization header unconditionally
- Should make it optional per spec
- Prevents anonymous (public festival) users from accessing content

---

## ✅ Android Changes Implemented

### 1. HomeScreen Error State Enhancement

**File:** `/app/src/main/java/com/faster/festival/ui/screens/HomeScreen.kt`

**Lines Modified:** 81-228 (Error state composable)

**Changes:**
```kotlin
// Before: Error hid entire UI (centered error message only)
// After: Error shows as banner + fallback UI
```

**New Error State Layout:**
```
┌─────────────────────────────────────────────────────┐
│                                                       │
│  ⚠️ Missing Authorization (Backend Issue)            │  ← Error Banner
│  The backend needs to be updated to support          │
│  public festivals without authentication headers.    │
│                                         [Retry]      │
│                                                       │
└─────────────────────────────────────────────────────┘
↓
┌─────────────────────────────────────────────────────┐
│                                                       │
│  Festival Information                                │  ← Fallback UI
│  Festival Slug: floydfest-26                         │
│  You're viewing: floydfest-26                        │
│                                                       │
│  Quick Links                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │
│  │ 🎤      │ │ 📅      │ │ ❓      │ │ ⚙️    │ │
│  │ Artists │ │Schedule │ │ FAQ    │ │Settings│ │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘ │
│                                                       │
│  The full festival content will appear once the      │
│  backend is configured to support public access.    │
│  Basic navigation is still available above.          │
│                                                       │
└─────────────────────────────────────────────────────┘
```

**Improvements:**
- ✅ Error no longer hides entire UI
- ✅ Users can still navigate (quick links available)
- ✅ Clear explanation of the issue
- ✅ Retry button for recovery
- ✅ Festival information displayed
- ✅ Support message provides context

### 2. QuickActionTile Composable

**File:** `/app/src/main/java/com/faster/festival/ui/screens/HomeScreen.kt`

**Lines Added:** 585-617

**Purpose:** Render quick navigation tiles in fallback error UI

```kotlin
@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
```

---

## 🔧 What Backend Needs (Supabase)

### Correct Implementation Pattern

All public content endpoints must check for Authorization header **before** calling `auth.getUser()`:

```typescript
// ✅ CORRECT PATTERN FOR BACKEND

// 1. Extract Authorization header (if present)
const authHeader = req.headers.get('Authorization');
const token = authHeader?.startsWith('Bearer ')
  ? authHeader.slice(7)
  : null;

// 2. Validate token ONLY if present
let userId: string | null = null;
if (token) {
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) {
    return new Response(
      JSON.stringify({ code: 401, message: "Invalid token" }),
      { status: 401 }
    );
  }
  userId = user.id;
}

// 3. Fetch festival
const { data: festival } = await supabase
  .from('festivals')
  .select('*')
  .eq('slug', festivalSlug)
  .single();

if (!festival) {
  return new Response(
    JSON.stringify({ code: 404, message: "Festival not found" }),
    { status: 404 }
  );
}

// 4. Enforce visibility
if (!userId && festival.visibility !== 'public') {
  return new Response(
    JSON.stringify({ code: 404, message: "Festival not found" }),
    { status: 404 }
  );
}

// 5. Build response with authorization context
const responseData = {
  schema_version: "1",
  generated_at: new Date().toISOString(),
  festival: festival,
  // ... other data
};

return new Response(JSON.stringify(responseData), { status: 200 });
```

### Endpoints Requiring This Fix

Apply the pattern above to:

1. ✅ `/functions/v1/app-home-bundle`
2. ✅ `/functions/v1/festival-header`
3. ✅ `/functions/v1/content-home`
4. ✅ `/functions/v1/content-lineup`
5. ✅ `/functions/v1/content-map`

---

## 📊 Request/Response Flow

### Anonymous User (No Login)

```
User Launch → Open Home Screen
↓
HomeScreen sends HTTP GET request:
  URL: GET /functions/v1/app-home-bundle?festival_slug=floydfest-26
  Headers:
    apikey: <SUPABASE_ANON_KEY>  ← Android sends
    Authorization: (NONE)         ← Android does NOT send (no token)
↓
Backend receives request:
  ✅ Check for Authorization header → NOT FOUND (null)
  ✅ Treat as anonymous user → userId = null
  ✅ Query festival by slug
  ✅ Check visibility:
    IF festival.visibility == "public":
      ✅ Return 200 with full response
    ELSE:
      ✅ Return 404 (not 401)
↓
Android receives response:
  ✅ 200 → Display festival content
  ❌ 404 → Show "Festival not found" error (expected)
  ❌ 401 → Show fallback UI (current issue - shouldn't happen)
```

### Logged-In User

```
User Login → Token Saved → Open Home Screen
↓
HomeScreen sends HTTP GET request:
  URL: GET /functions/v1/app-home-bundle?festival_slug=floydfest-26
  Headers:
    apikey: <SUPABASE_ANON_KEY>              ← Android sends
    Authorization: Bearer <access_token>     ← Android sends
↓
Backend receives request:
  ✅ Check for Authorization header → FOUND
  ✅ Extract and validate token
  ✅ Call auth.getUser() → Get user ID
  ✅ Query festival by slug
  ✅ Check permissions (user-specific rules if any)
  ✅ Return 200 with full response
↓
Android receives response:
  ✅ 200 → Display festival content
```

---

## ✅ Verification Checklist

### Android Implementation ✅
- [x] Authorization header only sent when token exists
- [x] API key always sent (required)
- [x] No hardcoded Authorization header
- [x] HomeScreen shows graceful error UI
- [x] Fallback UI displays:
  - [x] Error banner at top
  - [x] Festival information card
  - [x] Quick action tiles (Artists, Schedule, FAQ, Settings)
  - [x] Support message
  - [x] Retry button
- [x] All navigation callbacks preserved
- [x] Type-safe Kotlin code
- [x] No compilation errors
- [x] Material 3 styling consistent

### Backend Implementation (TODO) ⏳
- [ ] Authorization header checked BEFORE auth.getUser()
- [ ] Anonymous access allowed for public festivals
- [ ] Token validation only if header is present
- [ ] Returns 404 (not 401) for non-visible festivals
- [ ] Applied to all 5 endpoints
- [ ] Tested with both authenticated and anonymous users
- [ ] Proper error messages for each case

---

## 🚀 Next Steps

### Immediate (Android Side) ✅
1. ✓ HomeScreen error UI enhanced
2. ✓ Fallback content available
3. ✓ Retry mechanism functional
4. ✓ No compilation errors

### Next (Backend/Supabase) ⏳
1. Update Supabase Edge Functions with correct auth pattern
2. Deploy updated functions
3. Test with anonymous user access
4. Verify 401 error no longer occurs

### Testing After Backend Fix
1. Launch app without logging in
2. Navigate to Home screen
3. Verify festival content loads (no error)
4. Test quick action tiles work
5. Log in and verify logged-in user also works
6. Verify settings/navigation screens open

---

## 📁 Files Changed

| File | Lines | Change | Status |
|------|-------|--------|--------|
| `HomeScreen.kt` | 81-228 | Error state UI | ✅ Complete |
| `HomeScreen.kt` | 585-617 | QuickActionTile composable | ✅ Complete |
| Documentation | New | 401_ERROR_FIX_HOMESCREEN.md | ✅ Created |
| Quick Reference | New | 401_QUICK_REFERENCE.txt | ✅ Created |

---

## 📞 Summary

**Android App:** ✅ **READY** (All code implemented correctly)

**Your Implementation:** Follows API spec perfectly  
**Backend Issue:** Violates API spec by requiring optional header  
**User Experience:** Enhanced with graceful error handling  
**Next Action:** Update Supabase Edge Functions with correct pattern

The Android client is production-ready and correctly implements the API specification. Once the Supabase backend is updated to make Authorization header truly optional, the Home screen will display content for anonymous (public festival) users.

---

**Status:** Android Implementation Complete  
**Compilation:** ✅ No Errors  
**Ready for Testing:** ✅ Yes  
