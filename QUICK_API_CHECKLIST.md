# 📊 QUICK REFERENCE - Onboarding API Checklist

**Status:** ✅ Onboarding Core Complete | ⚠️ Profile & Content Missing  
**Date:** March 4, 2026

---

## 🟢 IMPLEMENTED (12 Endpoints)

### Authentication (5/5) ✅
- [x] Signup with Email
- [x] Login with Email
- [x] Verify OTP
- [x] Password Recovery
- [x] Logout

### Onboarding Steps (7/7) ✅
- [x] Username
- [x] Date of Birth
- [x] Race/Ethnicity
- [x] Gender Identity
- [x] Emergency Contact
- [x] Wristband
- [x] Terms & Conditions

### Infrastructure (2/2) ✅
- [x] Ensure Festival Onboarding (RPC)
- [x] Session Management

---

## 🔴 MISSING - CRITICAL (Phase 1)

### Profile Management (0/4) ❌
| Feature | Endpoint | Priority | Location |
|---------|----------|----------|----------|
| Save Legal Name | `POST /functions/v1/save-profile-name` | HIGH | After Onboarding |
| Upload Avatar | `POST /functions/v1/upload-avatar` | HIGH | Profile Screen |
| Get Avatar | `GET /functions/v1/avatar-url` | HIGH | Profile Display |
| Profile Summary | `GET /functions/v1/profile-summary` | CRITICAL | Post-Onboarding |

**Why Critical:** Need to load user profile after onboarding completion

### Content for Home (0/2) ❌
| Feature | Endpoint | Priority |
|---------|----------|----------|
| Home Bundle | `GET /functions/v1/app-home-bundle` | CRITICAL |
| Home Content | `GET /functions/v1/content-home` | CRITICAL |

**Why Critical:** Home screen cannot display content without these

---

## 🟡 MISSING - HIGH PRIORITY (Phase 2)

### Content Endpoints (0/3) ❌
- [ ] Get Lineup (Artists)
- [ ] Get Stage Schedule
- [ ] Get Map/Venues

### Friendships (0/5) ❌
- [ ] Search Festival Members
- [ ] Request Friendship
- [ ] Respond to Friendship
- [ ] List Friendships
- [ ] Manage Blocks

---

## 🟠 MISSING - MEDIUM PRIORITY (Phase 3)

### Experience & Offline (0/3) ❌
- [ ] Experience Categories
- [ ] Experience Locations
- [ ] Offline Bundle

---

## ⚡ QUICK START - IMMEDIATE ACTIONS

### **TODAY: Add Profile Summary** (5 min)
```kotlin
// 1. Add to OnboardingApiService.kt
@GET("functions/v1/profile-summary")
suspend fun getProfileSummary(
    @Header("Authorization") authorization: String
): Response<ProfileSummaryResponse>

// 2. Add to OnboardingRepository.kt
suspend fun getProfileSummary(): Result<ProfileSummaryResponse> {
    // Use pattern from existing methods
}

// 3. Call in OnboardingViewModel after terms accepted
val profile = onboardingRepository.getProfileSummary()
```

### **THIS WEEK: Home Bundle** (30 min)
```kotlin
// Already mostly implemented in ContentRepository
// Just need to integrate into HomeViewModel
fun loadHomeBundle(festivalSlug: String) {
    val result = contentRepository.getAppHomeBundle(festivalSlug)
    // Update UI state
}
```

### **NEXT WEEK: Legal Name Field** (45 min)
```kotlin
// Follow template in MISSING_API_IMPLEMENTATIONS.md
// Add optional profile name screen
```

---

## 📋 CURRENT FLOW vs SPEC

### Current (Onboarding Only) ✅
```
1. Signup/Login
2. Username
3-7. Demographics + Emergency Contact + Wristband + Terms
```

### Expected (Full User Journey) ❌
```
1. Signup/Login
2-8. Onboarding (7 steps)
9. Load Profile Summary
10. Display Home with Content
11. Show Lineup, Map, Schedule
12. Allow Friendships
```

**GAPS:** Steps 9-12 not implemented

---

## 🎯 SUCCESS CRITERIA

### Onboarding Complete? ✅
- [x] All 7 steps functional
- [x] Terms acceptance works
- [x] Activation logic correct
- [x] Error handling in place

### Post-Onboarding? ❌
- [ ] Profile loads correctly
- [ ] Avatar displays
- [ ] Home content shows
- [ ] Navigation works

---

## 📱 FILES TO MODIFY

### Priority 1 (This Sprint)
1. `OnboardingApiService.kt` - Add profile-summary endpoint
2. `OnboardingRepository.kt` - Add getProfileSummary()
3. `OnboardingViewModel.kt` - Call getProfileSummary() on completion

### Priority 2 (Next Sprint)
1. `ContentRepository.kt` - Integrate app-home-bundle
2. Create `HomeViewModel.kt` - Use content endpoints
3. Create `ProfileScreen.kt` - Display profile data

### Priority 3 (Future)
1. Add remaining content endpoints
2. Implement friendship features
3. Add experience/offline support

---

## ✅ VALIDATION

- [x] Current onboarding: 100% working
- [x] All 7 steps present and functional
- [x] API calls matched to Supabase spec
- [x] Error handling implemented
- [x] Activation logic correct
- [ ] Post-onboarding flow: 0% implemented
- [ ] Profile management: 0% implemented
- [ ] Content loading: 0% implemented
- [ ] Friendships: 0% implemented

---

## 📞 SUPPORT

**Documentation Files:**
1. `ONBOARDING_API_ANALYSIS.md` - Full breakdown
2. `MISSING_API_IMPLEMENTATIONS.md` - Code templates
3. `supabase_api.txt` - Original API spec

**Need Help?**
- Check template for each endpoint in `MISSING_API_IMPLEMENTATIONS.md`
- Follow existing pattern from `saveUsername()` method
- Test with `OnboardingViewModel` integration

---

## 🚀 ESTIMATED TIMELINE

| Task | Effort | Timeline |
|------|--------|----------|
| Add Profile Summary | 5 min | Today |
| Add Home Bundle integration | 30 min | This week |
| Add Legal Name field | 45 min | Next week |
| Add Avatar upload | 1 hour | Week 2 |
| Add remaining content | 3 hours | Week 3 |
| Add friendships | 4 hours | Week 4 |
| **TOTAL** | **~9.5 hours** | **1 Month MVP** |

---

## 🎓 LEARNING RESOURCES

**Pattern to Follow (Already In Code):**
```
SaveUsername endpoint:
- OnboardingApiService.kt: saveUsername()
- OnboardingRepository.kt: saveUsername()
- OnboardingViewModel.kt: proceedFromUsername()
- OnboardingScreen.kt: UsernameScreen()

→ Use this exact pattern for all new endpoints
```

**Key Files to Study:**
1. `OnboardingRepository.kt` - API error handling
2. `OnboardingViewModel.kt` - State management
3. `ContentRepository.kt` - Already has structure

---

**Status:** Ready for implementation  
**Confidence:** High - all patterns established  
**Risk Level:** Low - following existing patterns

Next Action: Add profile-summary endpoint (highest value, lowest effort)
