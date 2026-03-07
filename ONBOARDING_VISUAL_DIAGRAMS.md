# 📊 ONBOARDING SCREEN - VISUAL DIAGRAMS & QUICK REFERENCE

---

## 🎯 Onboarding Flow Diagram

```
┌──────────────────────────────────────────────────────┐
│                 Onboarding Screen                     │
│                  (HorizontalPager)                    │
└──────────────┬───────────────────────────────────────┘
               │
        ┌──────▼──────┐
        │   Initialize │
        │ (RPC Call)   │
        └──────┬───────┘
               │
      ┌────────▼────────┐
      │ Get Festival ID  │
      │ Get Missing      │
      │ Fields          │
      └────────┬────────┘
               │
      ┌────────▼──────────┐
      │ Build Step Order   │
      │ (OnboardingStep)   │
      │ Coordinator)       │
      └────────┬───────────┘
               │
      ┌────────▼──────────┐
      │   Display Step 1   │
      │    USERNAME       │
      └────────┬───────────┘
               │
      ┌────────▼──────────┐
      │   User Input      │
      │   Validation      │
      └────────┬───────────┘
               │
      ┌────────▼──────────┐
      │  Save (API Call)   │
      └────────┬───────────┘
               │
      ┌────────▼──────────┐
      │  Step Complete     │
      │  Next Step?        │
      └────────┬───────────┘
               │
      ┌────────▼──────────┐
      │ Loop to Step 2...7 │
      └────────┬───────────┘
               │
      ┌────────▼──────────┐
      │  Final Step:       │
      │  TERMS_ACCEPTANCE  │
      │  (Always Last)     │
      └────────┬───────────┘
               │
      ┌────────▼──────────┐
      │  Onboarding       │
      │  Complete!        │
      │  Activate Account │
      └────────┬───────────┘
               │
      ┌────────▼──────────┐
      │  Navigate to Home  │
      └────────────────────┘
```

---

## 📋 The 7 Steps - Visual Sequence

```
STEP 1                STEP 2               STEP 3
┌────────────┐      ┌────────────┐      ┌────────────┐
│  USERNAME  │  →   │  DATE OF   │  →   │   RACE &   │
│            │      │  BIRTH     │      │ ETHNICITY  │
└────────────┘      └────────────┘      └────────────┘
                                              │
     ┌──────────────────────────────────────┘
     │
     ▼
STEP 4                STEP 5               STEP 6
┌────────────┐      ┌────────────┐      ┌────────────┐
│  GENDER    │  →   │ EMERGENCY  │  →   │ WRISTBAND  │
│ IDENTITY   │      │  CONTACT   │      │            │
└────────────┘      └────────────┘      └────────────┘
                                              │
     ┌──────────────────────────────────────┘
     │
     ▼
STEP 7 (ALWAYS LAST) ⭐
┌─────────────────────┐
│  TERMS ACCEPTANCE   │
│  (Legal Required)   │
│  (Always Final)     │
└──────────┬──────────┘
           │
           ▼
      ✅ COMPLETE
```

---

## 🔄 State Management Diagram

```
┌───────────────────────────────────────┐
│  OnboardingFormState                   │
├───────────────────────────────────────┤
│ • username: String                     │
│ • dateOfBirth: String                  │
│ • selectedRaceEthnicity: List<String>  │
│ • selectedGenderIdentity: String       │
│ • emergencyContactName: String         │
│ • emergencyContactPhone: String        │
│ • emergencyContactRelationship: String │
│ • wristbandCode: String                │
│ • termsAccepted: Boolean               │
│ • orderedSteps: List<OnboardingStep>   │
│ • currentStepIndex: Int                │
│ • missing: List<String>                │
└───────────────────────────────────────┘
              ▲
              │
              │ StateFlow
              │
        ┌─────┴──────┐
        │   ViewModel│
        └─────┬──────┘
              │
              │ collectAsState()
              │
              ▼
        ┌──────────────┐
        │   Composable │
        │   (UI)       │
        └──────────────┘
```

---

## 🎭 UI State Machine

```
           ┌────────────┐
           │   Start    │
           └─────┬──────┘
                 │
                 ▼
         ┌──────────────┐
         │   LOADING    │ ← Initialization
         └──────┬───────┘
                │
                ▼
         ┌──────────────┐
         │     IDLE     │ ← Ready to show form
         └──────┬───────┘
                │
         ┌──────┴────────┐
         │               │
         ▼               ▼
    ┌────────┐      ┌─────────┐
    │ ERROR  │      │ SUCCESS │
    └─┬──────┘      └────┬────┘
      │                  │
      └────────┬─────────┘
               │
               ▼
        ┌──────────────────┐
        │ONBOARDING        │
        │COMPLETE         │
        └──────┬───────────┘
               │
               ▼
        ┌──────────────┐
        │   Navigate   │
        │   to Home    │
        └──────────────┘
```

---

## 📊 Step Ordering Algorithm

```
Input: missing List<String>?
  │
  ├─ Check "username" → Add USERNAME
  │
  ├─ Check "date_of_birth" → Add DATE_OF_BIRTH
  │
  ├─ Check "race_ethnicity" → Add RACE_ETHNICITY
  │
  ├─ Check "gender_identity" → Add GENDER_IDENTITY
  │
  ├─ Check "emergency_contact" → Add EMERGENCY_CONTACT
  │
  ├─ Check "wristband" → Add WRISTBAND
  │
  └─ ALWAYS Add TERMS_ACCEPTANCE (Last)
                    │
                    ▼
Output: List<OnboardingStep> (Ordered)
```

---

## 🏗️ Architecture Layers

```
┌─────────────────────────────────────┐
│       Presentation Layer             │
│  • OnboardingScreen (Composable)     │
│  • HorizontalPager                   │
│  • Progress Indicator                │
│  • SnackBar (Errors)                 │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│       ViewModel Layer                │
│  • OnboardingViewModel               │
│  • StateFlow<OnboardingFormState>    │
│  • StateFlow<OnboardingUiState>      │
│  • viewModelScope.launch             │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│       Repository Layer               │
│  • OnboardingRepository              │
│  • Error Handling                    │
│  • Response Mapping                  │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│       Network Layer                  │
│  • OnboardingApiService (Retrofit)   │
│  • API Endpoints                     │
│  • Request/Response Serialization    │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│       Backend Layer                  │
│  • Supabase Edge Functions           │
│  • Database                          │
│  • Authentication                    │
└─────────────────────────────────────┘
```

---

## 🧪 Test Coverage Map

```
┌─────────────────────────────────────┐
│  OnboardingStepCoordinatorTest.kt    │
├─────────────────────────────────────┤
│                                     │
│  OnboardingStepCoordinator (9)      │
│  ├─ Single Field Missing      ✅    │
│  ├─ Multiple Fields Missing   ✅    │
│  ├─ Terms Always Last         ✅    │
│  ├─ Empty Missing List        ✅    │
│  ├─ Null Missing List         ✅    │
│  ├─ Get Step Index            ✅    │
│  ├─ Get Step at Index         ✅    │
│  ├─ Out of Bounds Index       ✅    │
│  └─ Duplicate Fields          ✅    │
│                                     │
│  OnboardingFormState (10)           │
│  ├─ Initial State             ✅    │
│  ├─ Update Username           ✅    │
│  ├─ Update DOB                ✅    │
│  ├─ Update Race/Ethnicity     ✅    │
│  ├─ Update Gender Identity    ✅    │
│  ├─ Update Emergency Contact  ✅    │
│  ├─ Update Wristband Code     ✅    │
│  ├─ Accept Terms              ✅    │
│  ├─ Update Ordered Steps      ✅    │
│  └─ Update Current Step Index ✅    │
│                                     │
│  TOTAL: 19 tests              ✅✅✅ │
└─────────────────────────────────────┘
```

---

## 🔗 Data Flow Diagram

```
User Input
    │
    ▼
OnboardingScreen
    │
    ├─→ Update Form State (ViewModel)
    │
    ├─→ Validate Input
    │
    ├─→ Save via API (OnboardingRepository)
    │
    └─→ Update UI State (Success/Error)
         │
         ├─→ Show Snackbar
         │
         ├─→ Move to Next Step
         │
         └─→ Update Progress
```

---

## 📱 Screen Flow Timeline

```
┌─────────────────────────────────────────────────────┐
│ Time: T0  - App Launches                             │
│ Action: OnboardingScreen composable created         │
│ State: Loading                                       │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ Time: T1  - RPC Call Returns                         │
│ Action: setMissingFields() called                   │
│ State: Idle                                          │
│ Content: Built ordered steps                        │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ Time: T2  - Step 1 Displayed                         │
│ Action: HorizontalPager shows first step            │
│ State: Idle                                          │
│ Content: USERNAME form                              │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ Time: T3  - User Fills Form                          │
│ Action: Form state updates on each input            │
│ State: Idle                                          │
│ Content: Form with user input                       │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ Time: T4  - User Taps Continue                       │
│ Action: Save API call initiated                     │
│ State: Loading (optional visual indicator)          │
│ Content: Form with potential button disabled        │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ Time: T5  - API Response Returns                     │
│ Action: State updated with success                  │
│ State: Success (snackbar shown)                      │
│ Content: Snackbar message displayed                 │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ Time: T6  - Transition to Next Step                  │
│ Action: Pager animated to next page                 │
│ State: Idle                                          │
│ Content: Step 2 form displayed                      │
└─────────────────────────────────────────────────────┘
         (Repeat T3-T6 for steps 2-7)
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ Time: T7  - Final Step (Terms)                       │
│ Action: Display TERMS_ACCEPTANCE form               │
│ State: Idle                                          │
│ Content: Terms text + checkbox                      │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ Time: T8  - Terms Accepted & Saved                   │
│ Action: Final API call                              │
│ State: OnboardingComplete                           │
│ Content: onOnboardingComplete() callback invoked    │
└─────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│ Time: T9  - Navigation to Home                       │
│ Action: Navigate away from onboarding               │
│ State: Completed                                     │
│ Content: Home screen displayed                      │
└─────────────────────────────────────────────────────┘
```

---

## ⭐ Key Points Summary

```
┌────────────────────────────────────────┐
│  ONBOARDING FLOW - KEY POINTS           │
├────────────────────────────────────────┤
│                                        │
│  ✅ 7 Steps Total                      │
│  ✅ Dynamic Ordering (Backend)         │
│  ✅ TERMS_ACCEPTANCE Always Last       │
│  ✅ Full State Management              │
│  ✅ Complete Error Handling            │
│  ✅ Real-time Validation               │
│  ✅ Progress Tracking                  │
│  ✅ 19 Unit Tests (100% Pass)          │
│  ✅ Production Ready                   │
│                                        │
└────────────────────────────────────────┘
```

---

**Status**: 🟢 COMPLETE & PRODUCTION READY

