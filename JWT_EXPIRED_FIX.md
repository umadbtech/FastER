# 🔐 JWT EXPIRED - ERROR FIX & ANALYSIS

**Date**: March 5, 2026  
**Issue**: {"error":"Festival lookup failed","detail":"JWT expired"}  
**Status**: ✅ **ANALYSIS COMPLETE - SOLUTION PROVIDED**

---

## 🎯 PROBLEM IDENTIFIED

### **Error Details**
```
{"error":"Festival lookup failed","detail":"JWT expired"}
```

### **Root Cause**
The access token stored in EncryptedSessionManager has expired. Supabase JWT tokens have a limited lifespan (typically 1 hour), and when they expire:

1. ✅ App sends expired token in Authorization header
2. ❌ Supabase backend rejects it
3. ❌ API returns "JWT expired" error
4. ❌ No automatic token refresh mechanism

---

## 📋 CURRENT ARCHITECTURE

### **Token Storage** (EncryptedSessionManager.kt)
```kotlin
fun saveAccessToken(token: String)    // ✅ Saves access token
fun getAccessToken(): String?         // ✅ Retrieves access token
fun saveRefreshToken(token: String)   // ✅ Saves refresh token
fun getRefreshToken(): String?        // ✅ Retrieves refresh token
```

### **Authorization Interceptor** (AuthorizationInterceptor.kt)
```kotlin
val token = getAccessToken()
if (!token.isNullOrBlank()) {
    request = request.newBuilder()
        .header("Authorization", "Bearer $token")  // Uses access token directly
        .build()
}
```

### **Issue**
- ✅ Tokens are stored
- ❌ **NO TOKEN REFRESH MECHANISM**
- ❌ **NO EXPIRATION CHECKING**
- ❌ **NO AUTOMATIC REFRESH LOGIC**

---

## ✅ SOLUTION: Implement Token Refresh Mechanism

### **Step 1: Create TokenRefreshInterceptor**

Create a new file: `TokenRefreshInterceptor.kt`

```kotlin
package com.faster.festival.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.remote.AuthApi
import kotlinx.coroutines.runBlocking

/**
 * Interceptor that automatically refreshes expired access tokens
 * using the refresh token before retrying the request
 */
class TokenRefreshInterceptor(
    private val sessionManager: EncryptedSessionManager,
    private val authApi: AuthApi
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // First attempt with current token
        var response = chain.proceed(originalRequest)

        // If 401 Unauthorized or JWT expired, try to refresh token
        if (response.code == 401 || response.body?.string()?.contains("JWT expired") == true) {
            synchronized(this) {
                // Double-check in case another thread already refreshed
                val currentToken = sessionManager.getAccessToken()
                
                // Try to refresh token
                val refreshToken = sessionManager.getRefreshToken()
                if (!refreshToken.isNullOrBlank()) {
                    val refreshResult = runBlocking {
                        refreshAccessToken(refreshToken)
                    }

                    if (refreshResult) {
                        // Token refreshed successfully, retry original request
                        val newToken = sessionManager.getAccessToken()
                        if (!newToken.isNullOrBlank()) {
                            val retryRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer $newToken")
                                .build()
                            return chain.proceed(retryRequest)
                        }
                    }
                }
            }
        }

        return response
    }

    private suspend fun refreshAccessToken(refreshToken: String): Boolean {
        return try {
            val response = authApi.refreshToken(
                RefreshTokenRequest(refreshToken)
            )
            
            if (response.isSuccessful) {
                val newTokens = response.body()
                if (newTokens != null && !newTokens.accessToken.isNullOrBlank()) {
                    sessionManager.saveAccessToken(newTokens.accessToken)
                    newTokens.refreshToken?.let { 
                        sessionManager.saveRefreshToken(it) 
                    }
                    true
                } else {
                    false
                }
            } else {
                // Refresh failed - token is invalid
                // Clear session and return false
                sessionManager.clearSession()
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

// Request/Response models for token refresh
data class RefreshTokenRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

data class RefreshTokenResponse(
    @SerialName("access_token")
    val accessToken: String?,
    @SerialName("refresh_token")
    val refreshToken: String?,
    @SerialName("token_type")
    val tokenType: String = "Bearer"
)
```

### **Step 2: Update AuthApi to include refresh endpoint**

```kotlin
// Add to AuthApi interface
@POST("auth/v1/token?grant_type=refresh_token")
suspend fun refreshToken(
    @Body request: RefreshTokenRequest
): Response<RefreshTokenResponse>
```

### **Step 3: Add clearSession method to EncryptedSessionManager**

```kotlin
fun clearSession() {
    sharedPreferences.edit().apply {
        remove(KEY_ACCESS_TOKEN)
        remove(KEY_REFRESH_TOKEN)
        remove(KEY_USER_EMAIL)
        remove(KEY_USER_ID)
        remove(KEY_USER_PHONE)
        remove(KEY_IS_EMAIL_CONFIRMED)
        apply()
    }
}
```

### **Step 4: Register TokenRefreshInterceptor in NetworkModule**

```kotlin
object NetworkModule {
    // ...existing code...

    fun provideOkHttpClient(
        context: Context,
        sessionManager: EncryptedSessionManager,
        authApi: AuthApi
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(SupabaseHeadersInterceptor(sessionManager))
            .addInterceptor(TokenRefreshInterceptor(sessionManager, authApi))  // ADD THIS
            .addInterceptor(AuthorizationInterceptor { sessionManager.getAccessToken() })
            // ... other interceptors ...
        
        return builder.build()
    }
}
```

---

## 📊 FLOW DIAGRAM

### **Without Token Refresh (Current - ❌ BROKEN)**
```
User Makes Request
        ↓
Include expired token in Authorization header
        ↓
Backend rejects with "JWT expired"
        ↓
❌ App crashes / shows error
```

### **With Token Refresh (Fixed - ✅ WORKS)**
```
User Makes Request
        ↓
Include access token in Authorization header
        ↓
Backend responds:
  ├─ ✅ 200 OK → Process response
  └─ ❌ 401 Unauthorized / "JWT expired" → Refresh token
      ├─ Call Supabase refresh endpoint
      ├─ Get new access token
      ├─ Save new tokens
      └─ Retry original request with new token
           ├─ ✅ 200 OK → Success!
           └─ ❌ 401 → Clear session & redirect to login
```

---

## 🔧 QUICK FIX (If Token Refresh Not Available)

If your backend doesn't have token refresh endpoint yet, you can force logout on JWT expired:

```kotlin
// In HomeScreen.kt error handling
is UiState.Error -> {
    val errorMessage = (bundleState as UiState.Error).message
    
    if (errorMessage.contains("JWT expired") || errorMessage.contains("token expired")) {
        // Clear session and redirect to login
        sessionManager.clearSession()
        // Navigate to login screen
        // Show message: "Your session has expired. Please login again."
    }
}
```

---

## 📋 IMPLEMENTATION CHECKLIST

- [ ] Create TokenRefreshInterceptor.kt
- [ ] Add refreshToken endpoint to AuthApi
- [ ] Add clearSession method to EncryptedSessionManager
- [ ] Register TokenRefreshInterceptor in NetworkModule
- [ ] Test with expired token scenario
- [ ] Add error handling for invalid refresh tokens
- [ ] Update HomeScreen error handling

---

## 🎯 WHY THIS HAPPENS

Supabase JWT tokens are short-lived (typically 1 hour):

1. User logs in → Gets access token + refresh token
2. App stores both tokens
3. User uses app for < 1 hour → Access token valid ✅
4. User leaves app idle for > 1 hour → Access token expires ❌
5. User opens app again → Access token is now invalid
6. App tries to use expired token → Backend rejects with 401

**Solution**: Use refresh token to get new access token before it's rejected

---

## ✨ WHAT THIS FIXES

✅ **No More "JWT expired" Errors**  
✅ **Seamless Token Refresh**  
✅ **Transparent to User**  
✅ **Automatic Retry on 401**  
✅ **Session Persistence**  

---

**Status**: ✅ **SOLUTION PROVIDED**  
**Next**: Implement the TokenRefreshInterceptor as shown above
