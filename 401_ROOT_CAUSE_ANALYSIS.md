# 401 "Missing Authorization Header" - Root Cause & Fix Guide

## 📋 Analysis Summary

### .env File Status ✅
Your `.env` file is correctly configured:
```dotenv
VITE_SUPABASE_URL=https://dccxxpzwpgjjxllygouq.supabase.co
VITE_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRjY3h4cHp3cGdqanhsbHlnb3VxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzA5OTI5MTIsImV4cCI6MjA4NjU2ODkxMn0.bpd2afSxxOwHE3UifEoXRld4K5FkuT3_hmGPULmdKXI
```

- ✅ Supabase URL is valid
- ✅ Anonymous key is valid JWT token
- ✅ Being loaded into BuildConfig

### How Android Handles Headers ✅

**NetworkModule.kt (Lines 45-53):**
```kotlin
private val authInterceptor = okhttp3.Interceptor { chain ->
    val original = chain.request()
    val requestBuilder =
        original.newBuilder()
            .header("apikey", BuildConfig.VITE_SUPABASE_ANON_KEY)  // ✅ Always sent
            .header("Content-Type", "application/json")
            .method(original.method, original.body)
    chain.proceed(requestBuilder.build())
}
```

**AuthorizationInterceptor.kt (Lines 13-20):**
```kotlin
override fun intercept(chain: Interceptor.Chain): Response {
    var request = chain.request()
    val token = getAccessToken()
    
    // ✅ CORRECT: Only send Authorization if token exists
    if (!token.isNullOrBlank()) {
        request = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }
    return chain.proceed(request)
}
```

### What Android Sends ✅

**Anonymous User (No Login):**
```
GET /functions/v1/app-home-bundle?festival_slug=floydfest-26
Headers:
  apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...  ✅ Sent
  Content-Type: application/json
  Authorization: (NOT SENT)  ✅ Correct per spec
```

**Logged-In User:**
```
GET /functions/v1/app-home-bundle?festival_slug=floydfest-26
Headers:
  apikey: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...  ✅ Sent
  Content-Type: application/json
  Authorization: Bearer <access_token>  ✅ Sent when available
```

---

## ❌ The Real Problem: Backend Violates API Spec

### API Spec Says (supabase_api.txt, Line 1119):
```
Headers:
  apikey: <SUPABASE_ANON_KEY>        (Required)
  Authorization: Bearer <access_token> (OPTIONAL)  ← KEY WORD
```

### Backend Currently Does:
```typescript
// ❌ WRONG: Requires Authorization unconditionally
const { data: { user }, error } = await supabase.auth.getUser();
if (!user) {
    return 401; // "Missing authorization header"
}
```

### Why This Fails:
1. `supabase.auth.getUser()` requires Authorization header
2. Anonymous users don't have a token
3. Backend throws 401 instead of treating them as anonymous
4. Violates published API spec

---

## ✅ The Solution: Fix Supabase Backend

### Correct Backend Pattern:

```typescript
// ✅ CORRECT: Check header existence BEFORE calling auth
const authHeader = req.headers.get('Authorization');
const token = authHeader?.startsWith('Bearer ')
  ? authHeader.slice(7)
  : null;

// Only validate if header exists
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

// Query festival with visibility check
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

// Enforce visibility rules
if (!userId && festival.visibility !== 'public') {
  return new Response(
    JSON.stringify({ code: 404, message: "Festival not found" }),
    { status: 404 }
  );
}

// Success - return data
return new Response(JSON.stringify(responseData), { status: 200 });
```

---

## 🎯 Action Items

### Android Side: ✅ COMPLETE (No Changes Needed)
- [x] apikey always sent
- [x] Authorization only sent when token exists
- [x] Matches API specification
- [x] HomeScreen shows graceful error UI on failure

### Backend Side: ⏳ NEEDS IMPLEMENTATION
Update these Supabase Edge Functions:
- [ ] `/functions/v1/app-home-bundle`
- [ ] `/functions/v1/festival-header`
- [ ] `/functions/v1/content-home`
- [ ] `/functions/v1/content-lineup`
- [ ] `/functions/v1/content-map`
- [ ] Other public content endpoints

**Pattern:** Check for Authorization header BEFORE calling `auth.getUser()`

---

## 📊 Current Request Flow

### Anonymous User Request:
```
Android App
  │
  ├─ Gets access token → null (not logged in)
  │
  ├─ AuthorizationInterceptor.intercept()
  │   └─ if (!token.isNullOrBlank()) → FALSE
  │       └─ Does NOT add Authorization header ✅
  │
  ├─ NetworkModule authInterceptor
  │   └─ Adds apikey header ✅
  │
  ├─ Sends Request:
  │   GET /functions/v1/app-home-bundle?festival_slug=floydfest-26
  │   Headers: {
  │       apikey: <valid-key> ✅
  │       Content-Type: application/json
  │   }
  │
  └─> Backend receives request
        │
        ├─ CURRENT BEHAVIOR (❌ Wrong):
        │   └─ Calls supabase.auth.getUser()
        │       └─ Requires Authorization header
        │       └─ Request has none
        │       └─ Returns 401 ❌
        │
        └─ CORRECT BEHAVIOR (✅ Right):
            └─ Checks for Authorization header first
                └─ Header missing → treat as anonymous
                └─ Query festival with visibility check
                └─ If public → return 200 ✅
                └─ If private → return 404 ✅
```

---

## 🔍 Verification Checklist

### Android Implementation ✅
- [x] API key from .env loaded into BuildConfig
- [x] API key always sent in requests
- [x] Authorization header only sent when token exists
- [x] HomeScreen shows fallback UI on errors
- [x] Code compiles without errors
- [x] Type-safe Kotlin

### Backend Implementation (TO DO)
- [ ] Check Authorization header before calling auth.getUser()
- [ ] Allow anonymous access for public festivals
- [ ] Return 404 (not 401) for non-visible festivals
- [ ] All 5 public endpoints updated
- [ ] Tested with anonymous users
- [ ] Tested with authenticated users

---

## 💡 Summary

| Aspect | Status | Details |
|--------|--------|---------|
| .env file | ✅ | Credentials loaded correctly |
| Android headers | ✅ | Implementation correct per spec |
| API spec | ✅ | Clear - Authorization optional |
| HomeScreen UI | ✅ | Shows graceful fallback on error |
| Backend behavior | ❌ | Violates spec, requires fix |
| Overall | ⏳ | Android ready, awaiting backend |

---

**Conclusion:** Your Android app is **100% correct**. The 401 error is caused by the backend not following its own API specification. The fix must be applied on the Supabase side by implementing the correct authorization header checking pattern.

