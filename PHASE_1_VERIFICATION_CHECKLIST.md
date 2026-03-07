# ✅ PHASE 1 - END-TO-END VERIFICATION CHECKLIST

**Purpose:** Verify that Phase 1 implementation is complete and working  
**Date:** March 4, 2026

---

## 🔍 Pre-Flight Checks

### Code Changes
- [x] Profile summary call added to OnboardingViewModel
- [x] AppHomeViewModel properly configured
- [x] AppHomeRepository with ETag caching
- [x] ContentHomeApi with models ready

### Build Verification
- [ ] Project compiles without errors
- [ ] No missing imports
- [ ] All dependencies resolved

**Command to run:**
```bash
./gradlew build
```

---

## 📱 Runtime Testing

### Test 1: Authentication → Onboarding Flow
```
Steps:
1. [ ] Launch app
2. [ ] See login screen
3. [ ] Complete signup/login
4. [ ] Navigate to onboarding (username, DOB, etc.)
5. [ ] Go through all 7 steps
6. [ ] Accept terms
7. [ ] Should see: OnboardingComplete state
```

### Test 2: Profile Loading
```
After onboarding complete:
1. [ ] Check Logcat for "Profile loaded successfully"
2. [ ] Verify no crash on profile summary call
3. [ ] Check that ProfileSummaryResponse is populated
4. [ ] Confirm user data is available for use
```

### Test 3: Home Screen Navigation
```
1. [ ] After onboarding, navigate to Home
2. [ ] Should see loading indicator briefly
3. [ ] App Home Bundle should load from API
4. [ ] Festival name should display
5. [ ] Hero carousel should show (if available)
6. [ ] Announcements section should display
7. [ ] Upcoming events should display
8. [ ] Quick action buttons should appear
```

### Test 4: ETag Caching
```
1. [ ] Load home screen (first time) - full API response
2. [ ] Navigate away from home
3. [ ] Return to home - should use ETag header
4. [ ] Check network tab for 304 Not Modified response
5. [ ] Verify cached data displays (should be instant)
```

### Test 5: Error Handling
```
1. [ ] Turn off internet
2. [ ] Try to load home - should show error UI
3. [ ] Tap retry button
4. [ ] Turn internet back on
5. [ ] Content should load
6. [ ] Error message should disappear
```

---

## 🎯 Acceptance Criteria

All of the following must pass:

### Onboarding
- [ ] All 7 steps work properly
- [ ] Terms acceptance works
- [ ] API responses are handled correctly
- [ ] Profile summary is called after terms

### Profile Loading  
- [ ] getProfileSummary() is called
- [ ] ProfileSummaryResponse is received
- [ ] No crashes or exceptions
- [ ] User can see profile data

### Home Screen
- [ ] AppHomeBundle API is called
- [ ] Content displays without errors
- [ ] All sections render (festival header, carousel, announcements, events)
- [ ] Quick actions are clickable

### Caching
- [ ] ETag headers are sent
- [ ] 304 responses are handled
- [ ] Cached data is used on retry
- [ ] Cache is cleared on logout

### Error Handling
- [ ] 401 errors show "Session expired"
- [ ] 404 errors show "Festival not found"
- [ ] 500 errors show "Server error"
- [ ] Retry button works
- [ ] Graceful fallback to cached data

---

## 📊 Test Coverage

| Scenario | Status | Notes |
|----------|--------|-------|
| Happy path (complete flow) | [ ] | User → Onboarding → Home |
| Profile load failure | [ ] | Should continue gracefully |
| Home content load failure | [ ] | Should show error with retry |
| No internet | [ ] | Should show offline error |
| ETag 304 response | [ ] | Should use cached data |
| Session expired (401) | [ ] | Should redirect to login |

---

## 🐛 Known Issues to Watch

1. **Profile Summary Timeout** - If API is slow, set 30s timeout
   - File: Add interceptor in NetworkModule.kt

2. **ETag Header Case** - Some servers expect "If-None-Match"
   - Verify: ContentRepository.kt line 330

3. **Profile Data Not Persisted** - Currently in memory only
   - Future: Save to DataStore/SharedPreferences

4. **Home Content vs Home Bundle** - Two endpoints doing same thing
   - Decision: Use app-home-bundle for now (more complete)

---

## ✨ Success Indicators

When Phase 1 is complete, you should see:

```
✅ Login works
✅ Onboarding 7 steps complete
✅ Profile summary loads (maybe with slight delay)
✅ Home screen shows content
✅ Users see festival name, banner, announcements, events
✅ Navigation between screens works
✅ Error handling works properly
✅ Can retry on failure
✅ ETag caching reduces data usage
```

---

## 🚨 Failure Modes to Check

| Issue | How to Check | Expected Behavior |
|-------|-------------|-------------------|
| Profile null | Logcat | Should log error, not crash |
| API timeout | Wait 30+ seconds | Should show error, not hang |
| No internet | Airplane mode | Should show error with retry |
| Invalid token | Logout first | Should go to login |
| Festival not found | Wrong slug | Should show 404 error |

---

## 📝 Test Log Template

```
Test Date: _______________
Tester: ___________________
Device: Android __, API Level: __

RESULTS:
- Onboarding: PASS / FAIL
  Issues: ________________
- Profile Loading: PASS / FAIL
  Issues: ________________
- Home Display: PASS / FAIL
  Issues: ________________
- Caching: PASS / FAIL
  Issues: ________________
- Error Handling: PASS / FAIL
  Issues: ________________

Overall: PASS / FAIL

Notes: ___________________
_________________________
_________________________
```

---

## 🎉 Phase 1 Complete When

- [x] Code changes applied
- [ ] Project builds successfully
- [ ] All 5 runtime tests pass
- [ ] All acceptance criteria met
- [ ] No known issues remaining
- [ ] Ready for Phase 2

---

**Phase 1 Status:** Ready for Testing ✅  
**Estimated Test Time:** 15-20 minutes  
**Next Phase:** Phase 2 (Profile Management - 2 hours)

**Questions?** Check:
- PHASE_1_IMPLEMENTATION_REPORT.md (what was done)
- MISSING_API_IMPLEMENTATIONS.md (code templates)
- ONBOARDING_API_ANALYSIS.md (detailed specs)
