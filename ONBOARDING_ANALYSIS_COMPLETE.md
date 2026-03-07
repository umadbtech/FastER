# ✅ ONBOARDING FLOW - COMPLETE ANALYSIS & UNIT TESTS

## Project Overview

### 📱 Onboarding Architecture

```
OnboardingScreen (UI Layer)
    ↓
OnboardingViewModel (State Management)
    ↓
OnboardingRepository (Data Layer)
    ↓
OnboardingApiService (Network Layer)
    ↓
Supabase Edge Functions (Backend)
```

---

## 📋 Onboarding Steps (7 Total)

### **Step 1: Username** ✅
- Screen: UsernameScreen
- Input: username (string)
- Validation: Required, must be valid
- Save: SaveUsernameRequest

### **Step 2: Date of Birth** ✅
- Screen: DateOfBirthScreen
- Input: dob (YYYY-MM-DD format)
- Validation: Required, valid date format
- Save: SaveDemographicsRequest

### **Step 3: Race & Ethnicity** ✅
- Screen: RaceEthnicityScreen
- Input: selectedRaceEthnicity (List<String>), raceEthnicityText (String)
- Validation: Multiple selection
- Save: SaveDemographicsRequest

### **Step 4: Gender Identity** ✅
- Screen: GenderIdentityScreen
- Input: selectedGenderIdentity (String), genderIdentityText (String)
- Validation: Single selection
- Save: SaveDemographicsRequest

### **Step 5: Emergency Contact** ✅
- Screen: EmergencyContactScreen
- Input: name, phone (E.164 format), relationship
- Validation: Required, valid phone format
- Save: SaveEmergencyContactRequest

### **Step 6: Wristband** ✅
- Screen: WristbandScreen
- Input: wristbandCode (String)
- Validation: Code format
- Save: SaveDemographicsRequest

### **Step 7: Terms Acceptance** ✅
- Screen: TermsAcceptanceScreen
- Input: termsAccepted (Boolean)
- Validation: Must be true to proceed
- Save: SaveDemographicsRequest

---

## 🏗️ Key Components

### **OnboardingStepCoordinator**
- Manages step ordering based on `missing` fields from backend
- Always includes TERMS_ACCEPTANCE as final step
- Builds ordered list dynamically

### **OnboardingViewModel**
- Manages UI state (Loading, Idle, Error, Success, Complete)
- Maintains form state for all fields
- Handles API calls and navigation
- Observes festival_id changes

### **OnboardingRepository**
- Bridges ViewModel and API service
- Handles API responses
- Returns Results for error handling

### **OnboardingApiService**
- Retrofit interface for Supabase Edge Functions
- Endpoints:
  - `ensure_festival_onboarding` (RPC) - Initialize
  - `save-username` (POST) - Save username
  - `save-demographics` (POST) - Save demographics
  - `save-emergency-contact` (POST) - Save emergency contact

---

## 📊 State Management

### **OnboardingFormState**
```kotlin
data class OnboardingFormState(
    val dateOfBirth: String = "",
    val selectedRaceEthnicity: List<String> = emptyList(),
    val selectedGenderIdentity: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelationship: String = "",
    val wristbandCode: String = "",
    val username: String = "",
    val termsAccepted: Boolean = false,
    val orderedSteps: List<OnboardingStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val missing: List<String> = emptyList()
)
```

### **OnboardingUiState**
```kotlin
sealed interface OnboardingUiState {
    object Loading
    object Idle
    data class Error(val message: String)
    data class Success(val message: String)
    object OnboardingComplete
}
```

---

## 🔄 Flow Logic

### **Initialization**
1. User navigates to OnboardingScreen
2. `initializeOnboarding()` is called
3. Calls `ensure_festival_onboarding` RPC
4. Retrieves `festival_id` and missing fields
5. Builds ordered steps via `OnboardingStepCoordinator`
6. Displays first step

### **Step Navigation**
1. User fills in current step form
2. User taps "Continue" or "Next"
3. Step is saved via API call
4. Response contains updated `missing` fields
5. UI updates with next step
6. Pager animates to next page

### **Completion**
1. Final step (TERMS_ACCEPTANCE) is completed
2. All required fields are saved
3. API returns success with no more missing fields
4. OnboardingUiState.OnboardingComplete is triggered
5. `onOnboardingComplete()` callback is invoked
6. Navigation to home screen

---

## ✅ Unit Test Coverage

### **Tests Included**

1. **Step Ordering Tests**
   - ✅ Single field missing
   - ✅ Multiple fields missing
   - ✅ Empty missing list (default steps)
   - ✅ TERMS_ACCEPTANCE always last

2. **ViewModel State Tests**
   - ✅ Initial state
   - ✅ Update form fields
   - ✅ Handle API responses
   - ✅ Handle errors

3. **Validation Tests**
   - ✅ Username validation
   - ✅ DOB format validation
   - ✅ Phone number format (E.164)
   - ✅ Terms acceptance required

4. **API Integration Tests**
   - ✅ Ensure onboarding call
   - ✅ Save username request
   - ✅ Save demographics request
   - ✅ Save emergency contact request

---

## 🚀 Production Ready

✅ All 7 steps implemented
✅ State management complete
✅ API integration verified
✅ Error handling included
✅ Unit tests comprehensive
✅ Navigation logic tested

---

## 📝 Notes

- TERMS_ACCEPTANCE is **always** the final step for legal compliance
- Missing fields are determined by backend response
- Step ordering is fixed but extensible
- All validation errors are shown to user
- API responses update missing fields dynamically

