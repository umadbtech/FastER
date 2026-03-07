# 401 "Missing Authorization Header" - Android Fix Summary

**Issue**: Edge Functions returning 401 for public festivals  
**Root Cause**: Backend requires Authorization header unconditionally  
**Solution**: Backend needs fixing (not Android app)  
**Status**: ✅ Android app code is already correct

---

## 📱 Android App (Kotlin) - Status: READY ✅

Your Android app is **already correctly implemented**:

### AuthorizationInterceptor.kt
```kotlin
// ✅ CORRECT: Only adds Authorization header if token exists
if (!token.isNullOrBlank()) {
    request = request.newBuilder()
        .header("Authorization", "Bearer $token")
        .build()
}
// If no token, header is NOT added (which is correct!)
```

### SupabaseHeadersInterceptor.kt
```kotlin
// ✅ CORRECT: Always adds apikey
.header("apikey", apiKey)

// ✅ CORRECT: Conditionally adds Authorization
val token = getAccessToken()
if (!token.isNullOrBlank()) {
    requestBuilder.header("Authorization", "Bearer $token")
}
```

**No changes needed on Android side** - it correctly sends Authorization only when token exists.

---

## 🔧 Backend (Supabase Edge Functions) - Status: NEEDS FIX ❌→✅

Your Supabase Edge Functions need to be updated to handle missing Authorization headers.

### What Needs to Change on Backend

**Current (Broken)**:
```typescript
// ❌ WRONG: Fails if Authorization header is missing
const { data: { user }, error } = await supabase.auth.getUser();
if (!user) {
  return 401; // "Missing authorization header"
}
```

**Fixed (Correct)**:
```typescript
// ✅ CORRECT: Check header exists BEFORE calling auth
const authHeader = req.headers.get('Authorization');
const token = authHeader?.startsWith('Bearer ') ? authHeader.slice(7) : null;

let userId: string | null = null;
if (token) {
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) return 401;
  userId = user.id;
}

// Enforce visibility
if (!userId && festival.visibility !== 'public') {
  return 404; // Not visible
}
```

---

## 📋 Backend Edge Functions to Update

Update these Supabase Edge Functions:

| Function | Endpoint | Status |
|----------|----------|--------|
| app-home-bundle | `/functions/v1/app-home-bundle` | ❌ Needs Fix |
| content-home | `/functions/v1/content-home` | ❌ Needs Fix |
| festival-header | `/functions/v1/festival-header` | ❌ Needs Fix |
| content-lineup | `/functions/v1/content-lineup` | ❌ Needs Fix |
| content-map | `/functions/v1/content-map` | ❌ Needs Fix |

### Implementation Pattern (Same for All Functions)

```typescript
// 1. Extract Authorization header
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
      JSON.stringify({ code: 401, message: 'Invalid token' }),
      { status: 401 }
    );
  }
  userId = user.id;
}

// 3. Query festival
const { data: festival } = await supabase
  .from('festivals')
  .select('id, visibility, owner_id')
  .eq('slug', festivalSlug)
  .single();

// 4. Enforce visibility
if (!userId && festival.visibility !== 'public') {
  return new Response(
    JSON.stringify({ code: 404, message: 'Festival not found' }),
    { status: 404 }
  );
}

// 5. Return data (200)
return new Response(JSON.stringify(response), { status: 200 });
```

---

## 🗂️ Database Schema Required

Ensure your Supabase `festivals` table has:

```sql
CREATE TABLE festivals (
  id UUID PRIMARY KEY,
  slug TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  visibility TEXT DEFAULT 'public' CHECK (visibility IN ('public', 'private', 'hidden')),
  owner_id UUID REFERENCES auth.users(id),
  -- ... other fields
);
```

---

## 🚀 What You Need to Do

### Option 1: Supabase Dashboard (Easiest)
1. Go to Supabase Dashboard
2. Navigate to Functions
3. For each function listed above:
   - Edit the function
   - Update to check for Authorization header **before** calling `auth.getUser()`
   - Deploy

### Option 2: Supabase CLI
```bash
# Deploy updated Edge Functions
supabase functions deploy app-home-bundle
supabase functions deploy content-home
supabase functions deploy festival-header
supabase functions deploy content-lineup
supabase functions deploy content-map
```

---

## ✅ After Backend Fix

Once you update the Edge Functions:

### Android App Will Automatically Work ✅
- ✅ No authentication required for public festivals
- ✅ Optional login for private festivals
- ✅ App already sends Authorization only when logged in
- ✅ All Edge Functions will return 200 instead of 401

### Test Scenarios That Will Work

**Test 1: Public Festival Without Login** ✅
```
App sends: GET /app-home-bundle?festival_slug=floydfest-26
(no Authorization header)

Backend returns: 200 with festival data
App shows: Festival home screen ✅
```

**Test 2: Public Festival With Login** ✅
```
App sends: GET /app-home-bundle?festival_slug=floydfest-26
Authorization: Bearer <token>

Backend returns: 200 with festival data
App shows: Festival home screen ✅
```

**Test 3: Private Festival Without Login** ✅
```
App sends: GET /app-home-bundle?festival_slug=private-fest
(no Authorization header)

Backend returns: 404 Festival not found
App shows: Error message ✅
```

---

## 📞 Quick Reference

**Problem**: 401 error when accessing public festivals without login  
**Cause**: Backend Edge Functions  
**Solution**: Update Edge Functions to check for Authorization header presence  
**Android Status**: Already correct ✅  
**Effort**: 30 minutes (update 5 Edge Functions)  

---

## 📝 Summary

✅ **Android App (Kotlin)**: No changes needed - already correct  
❌ **Backend (Supabase)**: Needs updates to 5 Edge Functions  

The pattern is simple:
1. Extract Authorization header
2. Validate token **only if header exists**
3. Treat missing header as anonymous user
4. Enforce visibility rules
5. Return 404 for access denied (not 401)

Once backend is fixed, your Android app will automatically work for public festivals without login! 🎉

