# 🔀 WRISTBAND vs TERMS ACCEPTANCE - DETAILED COMPARISON

**Date**: March 5, 2026  
**Purpose**: Understand the architecture difference between optional and required steps

---

## 📊 SIDE-BY-SIDE COMPARISON

### **WRISTBAND STEP (Step 6 - OPTIONAL)**

#### **ViewModel Logic** (OnboardingViewModel.kt:504-530)

```kotlin
private fun proceedFromWristband() {
    val wristbandCode = _formState.value.wristbandCode

    if (wristbandCode.isEmpty()) {
        // Skip wristband - it's optional ✅
        proceedToNextStep()
        return
    }

    viewModelScope.launch {
        _uiState.value = OnboardingUiState.Loading
        val result = onboardingRepository.saveWristband(wristbandCode)
        result.onSuccess { response ->
            // Clear error
            _formState.update { it.copy(wristbandError = null) }

            // Check if onboarding is complete
            if (response.activated == true) {
                _uiState.value = OnboardingUiState.OnboardingComplete
            } else {
                setMissingFields(response.missing)
                proceedToNextStep()
                _uiState.value = OnboardingUiState.Idle
            }
        }
    }
}
```

#### **UI** (WristbandScreen.kt)

```kotlin
// Button 1: Pair Wristband
Button(
    onClick = onPairClick,  // Start countdown
    containerColor = MaterialTheme.colorScheme.primaryContainer
) {
    Text("Pair Wristband")
}

// Button 2: Skip Pairing ⭐ KEY
TextButton(
    onClick = onSkipClick,  // Skip step (no code = empty)
) {
    Text("Skip Pairing the Wristband")
}
```

#### **Features**
- ✅ Has "Skip" button
- ✅ No validation error on empty
- ✅ No checkbox requirements
- ✅ Auto-advances if code empty
- ✅ Optional device pairing

---

### **TERMS ACCEPTANCE STEP (Step 7 - REQUIRED)**

#### **ViewModel Logic** (OnboardingViewModel.kt:538-555)

```kotlin
private fun proceedFromTermsAcceptance() {
    val current = _formState.value
    if (!current.termsAccepted) {
        _uiState.value = OnboardingUiState.Error("You must accept the terms to proceed")
        return  // ❌ Block if not accepted
    }

    viewModelScope.launch {
        _uiState.value = OnboardingUiState.Loading
        val result = onboardingRepository.acceptTerms()
        result.onSuccess { response ->
            // Check if activation is complete
            if (response.activated == true) {
                _uiState.value = OnboardingUiState.OnboardingComplete
            } else {
                setMissingFields(response.missing)
                proceedToNextStep()
                _uiState.value = OnboardingUiState.Idle
            }
        }
    }
}
```

#### **UI** (TermsAcceptanceScreen.kt)

```kotlin
// Checkbox 1: Terms and Conditions ⭐ REQUIRED
TermsCheckboxItem(
    label = "I accept the Terms and Conditions",
    isChecked = termsAndConditionsChecked,
    onCheckedChange = { isChecked ->
        if (isScrolledToBottom) {
            termsAndConditionsChecked = isChecked
            val bothChecked = isChecked && privacyPolicyChecked  // BOTH must be true!
            onTermsAcceptanceChange(bothChecked)
        }
    },
    enabled = isScrolledToBottom
)

// Checkbox 2: Privacy Policy ⭐ REQUIRED
TermsCheckboxItem(
    label = "I accept the Privacy Policy",
    isChecked = privacyPolicyChecked,
    onCheckedChange = { isChecked ->
        if (isScrolledToBottom) {
            privacyPolicyChecked = isChecked
            val bothChecked = termsAndConditionsChecked && isChecked  // BOTH must be true!
            onTermsAcceptanceChange(bothChecked)
        }
    },
    enabled = isScrolledToBottom
)

// Info text
Text(
    text = if (isScrolledToBottom) 
        "Both boxes must be checked to continue." 
    else 
        "You must scroll down to read the full terms before accepting."
)
```

#### **Features**
- ❌ No "Skip" button
- ❌ Validation error if NOT accepted
- ✅ DUAL checkbox requirement
- ✅ Scroll-to-bottom requirement
- ❌ Cannot auto-advance (requires user action)
- ❌ Legally required

---

## 🔄 BEHAVIOR FLOW COMPARISON

### **WRISTBAND: User Decision Tree**

```
User reaches Wristband step
    ├─ Decision A: "Pair Wristband"
    │   ├─ Yes → Start countdown
    │   ├─ Timer counts down (10s)
    │   ├─ BLE pairs device
    │   ├─ Code captured: "ABC123"
    │   ├─ updateWristbandCode("ABC123")
    │   ├─ proceedFromCurrentStep()
    │   └─ Code NOT empty → API call → Next step
    │
    └─ Decision B: "Skip Pairing the Wristband"
        ├─ Yes → proceedFromCurrentStep()
        ├─ Code IS empty → NO API call → Next step directly ✅
        └─ Result: Wristband step SKIPPED
```

### **TERMS: User Decision Tree**

```
User reaches Terms step
    ├─ Must scroll to bottom first
    │   ├─ Checkboxes disabled until scrolled
    │   └─ Once scrolled → Checkboxes enabled
    │
    ├─ Check: "Terms and Conditions"
    │   ├─ Yes → termsAndConditionsChecked = true
    │   └─ Check if BOTH are now true
    │
    ├─ Check: "Privacy Policy"
    │   ├─ Yes → privacyPolicyChecked = true
    │   └─ Check if BOTH are now true
    │
    ├─ BOTH boxes checked?
    │   ├─ Yes → formState.termsAccepted = true
    │   │   └─ Submit button enabled ✅
    │   │
    │   └─ No → formState.termsAccepted = false
    │       └─ Submit button disabled ❌
    │
    └─ Click Submit
        ├─ if (!termsAccepted) → Error "You must accept..."
        │   └─ Do NOT proceed (blocked) ❌
        │
        └─ if (termsAccepted) → API call acceptTerms()
            └─ Next step (Onboarding Complete) ✅
```

---

## 💾 STATE MANAGEMENT

### **WRISTBAND State**

```kotlin
data class OnboardingFormState(
    val wristbandCode: String = "",          // User-entered code
    val wristbandError: String? = null,      // Never set to "required"
    // ...
)

// When user clicks skip:
// wristbandCode = "" (empty)
// wristbandError = null (no error)
// → proceedToNextStep() called directly
```

### **TERMS State**

```kotlin
data class OnboardingFormState(
    val termsAccepted: Boolean = false,      // Must be TRUE to proceed
    // ...
)

// When user clicks submit:
// if (!termsAccepted) {
//     Error shown: "You must accept..."
//     Do NOT proceed ❌
// }
// if (termsAccepted) {
//     API call: acceptTerms()
//     Proceed ✅
// }
```

---

## 📋 VALIDATION RULES

### **WRISTBAND Validation**

| Scenario | Code Value | Validation | Result |
|----------|-----------|-----------|--------|
| User provides code | "ABC123" | ✅ Valid | API call → Proceed |
| User skips | "" | ⏭️ No error | Auto-skip (no API) |
| Empty after input | "" | ⏭️ No error | Auto-skip (no API) |

**Key**: No validation error on empty - automatic skip

### **TERMS Validation**

| Scenario | Both Checked | Validation | Result |
|----------|------------|-----------|--------|
| Both checked | true | ✅ Valid | API call → Proceed |
| One checked | false | ❌ Error | "You must accept..." → Blocked |
| None checked | false | ❌ Error | "You must accept..." → Blocked |

**Key**: Validation REQUIRED - must block if unchecked

---

## 🧪 TEST IMPLICATIONS

### **Test #3: Wristband**

**What to Test**: Empty code should skip, NOT error

```kotlin
@Test
fun `test proceedFromWristband skips with empty code`() = runTest {
    viewModel.updateWristbandCode("") // Empty
    viewModel.proceedFromCurrentStep()
    advanceUntilIdle()
    
    // ✅ Should have NO error
    assertNull(viewModel.formState.value.wristbandError)
    
    // ✅ Should have advanced
    assertEquals(1, viewModel.formState.value.currentStepIndex)
}
```

### **Test #1: Terms Acceptance**

**What to Test**: Terms must be accepted to proceed

```kotlin
@Test
fun `test completion requires TERMS_ACCEPTANCE with true value`() = runTest {
    coEvery { mockRepository.acceptTerms() } returns Result.success(...)
    
    viewModel.updateTermsAcceptance(true) // Both checked
    viewModel.proceedFromCurrentStep()
    advanceUntilIdle()
    
    // ✅ Should be accepted
    assertTrue(viewModel.formState.value.termsAccepted)
    
    // ✅ Should be complete
    assertEquals(OnboardingUiState.OnboardingComplete, viewModel.uiState.value)
}
```

---

## 🎯 ARCHITECTURAL PATTERN

### **Optional Steps** (Like Wristband)

```
Pattern:
1. Check if required data is empty
2. If empty → Skip step (no validation error)
3. If NOT empty → Validate and proceed

Benefit:
- Flexible onboarding
- Don't block on missing devices
- Can be done later
```

### **Required Steps** (Like Terms)

```
Pattern:
1. Validate that required data is set
2. If NOT set → Show error and block
3. If set → Proceed with API call

Benefit:
- Ensures compliance
- Legally required
- Cannot be skipped
```

---

## 📌 SUMMARY TABLE

| Aspect | WRISTBAND (Optional) | TERMS (Required) |
|--------|----------------------|------------------|
| Skip allowed | ✅ Yes (empty = skip) | ❌ No |
| Validation error | ❌ No | ✅ Yes (if unchecked) |
| API call on skip | ❌ No | N/A |
| Button style | Skip button | Submit button |
| UI constraints | None | Scroll-to-bottom |
| Checkbox count | 0 | 2 (both required) |
| Auto-advance | ✅ Yes (if empty) | ❌ No |
| Block on fail | ❌ No | ✅ Yes |

---

## 🎓 LESSONS FOR ARCHITECTURE

**1. Not all steps are equal**
- Wristband: Flexible device, can skip
- Terms: Legal requirement, cannot skip

**2. Different validation approaches**
- Wristband: No error on empty (auto-skip)
- Terms: Error on unchecked (must block)

**3. Test must match reality**
- Wristband: Test that empty code skips
- Terms: Test that unchecked blocks

**4. UI can differ per step**
- Wristband: Simple device pairing
- Terms: Complex dual checkbox + scroll

---

## ✅ CONCLUSION

Understanding the difference between WRISTBAND (optional) and TERMS (required) is critical for:
1. ✅ Correct ViewModel implementation
2. ✅ Proper test design
3. ✅ User experience
4. ✅ Legal compliance

**The 3 test fixes reflected this understanding**:
- Test #1: Terms need acceptTerms() API (not saveDemographics)
- Test #2: Complete flow must simulate all steps
- Test #3: Wristband must allow skip on empty (no error)

---

**Date**: March 5, 2026  
**Status**: ✅ Complete Analysis
