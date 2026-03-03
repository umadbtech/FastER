package com.faster.festival.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.model.SaveDemographicsRequest
import com.faster.festival.data.model.SaveEmergencyContactRequest
import com.faster.festival.data.repository.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * UI state for onboarding flow.
 */
sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState
    data object Idle : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
    data class Success(val message: String) : OnboardingUiState
    data object OnboardingComplete : OnboardingUiState
}

/**
 * Form state for the entire onboarding flow.
 */
data class OnboardingFormState(
    // Screen 1: Date of Birth
    val dateOfBirth: String = "", // YYYY-MM-DD
    val dobError: String? = null,

    // Screen 2: Race & Ethnicity
    val selectedRaceEthnicity: List<String> = emptyList(),
    val raceEthnicityText: String = "",

    // Screen 3: Gender Identity
    val selectedGenderIdentity: String = "",
    val genderIdentityText: String = "",

    // Screen 4: Primary Emergency Contact
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelationship: String = "",
    val emergencyContactError: String? = null,

    // Screen 5: Wristband
    val wristbandCode: String = "",
    val wristbandError: String? = null,

    // Username (dynamic screen if in missing)
    val username: String = "",
    val usernameError: String? = null,

    // Terms acceptance (dynamic screen if in missing)
    val termsAccepted: Boolean = false,

    // Ordered list of steps based on backend `missing` field
    val orderedSteps: List<OnboardingStep> = emptyList(),

    // Current step index in the ordered steps list
    val currentStepIndex: Int = 0,

    // Backend missing fields
    val missing: List<String> = emptyList()
)

/**
 * ViewModel for managing onboarding flow state and API interactions.
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val defaultFestivalId: String = "297d5837-a7b6-49a4-873b-4e3b17b60657"
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(OnboardingFormState())
    val formState: StateFlow<OnboardingFormState> = _formState.asStateFlow()

    // Mutable state for festival_id retrieved from RPC
    private val _festivalId = MutableStateFlow(defaultFestivalId)
    val festivalId: StateFlow<String> = _festivalId.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Initialize onboarding flow by calling ensure_festival_onboarding RPC
     * to retrieve the actual festival_id for this user's session.
     */
    fun initializeOnboarding() {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            val result = onboardingRepository.ensureOnboarding()
            result.onSuccess { retrievedFestivalId ->
                // Update festival_id from RPC response
                _festivalId.value = retrievedFestivalId

                // Set default missing fields to show the onboarding flow
                // In a full implementation, these would come from the backend response
                // ✅ NOW INCLUDES: username (step 1) and terms_acceptance (step 7) - ALL 7 STEPS
                val defaultMissing = listOf(
                    "username",
                    "date_of_birth",
                    "race_ethnicity",
                    "gender_identity",
                    "emergency_contact",
                    "wristband",
                    "terms_acceptance"
                )
                setMissingFields(defaultMissing)

                _uiState.value = OnboardingUiState.Idle
            }.onFailure { error ->
                // Log error but fallback to default festival_id and steps
                _festivalId.value = defaultFestivalId

                // Still set missing fields so the screen shows content
                // ✅ NOW INCLUDES: username (step 1) and terms_acceptance (step 7) - ALL 7 STEPS
                val defaultMissing = listOf(
                    "username",
                    "date_of_birth",
                    "race_ethnicity",
                    "gender_identity",
                    "emergency_contact",
                    "wristband",
                    "terms_acceptance"
                )
                setMissingFields(defaultMissing)

                _uiState.value = OnboardingUiState.Error("${error.message ?: "Failed to initialize onboarding"} - using default flow")
            }
        }
    }

    /**
     * Set missing fields and build ordered steps based on backend response.
     */
    fun setMissingFields(missing: List<String>?) {
        val orderedSteps = OnboardingStepCoordinator.buildOrderedSteps(missing)
        _formState.update { state ->
            state.copy(
                missing = missing ?: emptyList(),
                orderedSteps = orderedSteps,
                currentStepIndex = 0
            )
        }
    }

    /**
     * Get the total number of steps in the current flow.
     */
    fun getTotalSteps(): Int = _formState.value.orderedSteps.size

    /**
     * Get the current step based on currentStepIndex.
     */
    fun getCurrentStep(): OnboardingStep? {
        val state = _formState.value
        return OnboardingStepCoordinator.getStepAtIndex(state.orderedSteps, state.currentStepIndex)
    }

    /**
     * Get the step at a specific index.
     */
    fun getStepAtIndex(index: Int): OnboardingStep? {
        return OnboardingStepCoordinator.getStepAtIndex(_formState.value.orderedSteps, index)
    }

    /**
     * Move to the next step.
     */
    fun proceedToNextStep() {
        val currentState = _formState.value
        if (currentState.currentStepIndex < currentState.orderedSteps.size - 1) {
            _formState.update { it.copy(currentStepIndex = it.currentStepIndex + 1) }
        }
    }

    /**
     * Move to the previous step.
     */
    fun goBack() {
        val currentState = _formState.value
        if (currentState.currentStepIndex > 0) {
            _formState.update { it.copy(currentStepIndex = it.currentStepIndex - 1) }
        }
    }

    /**
     * Validate and proceed from current step based on step type.
     */
    fun proceedFromCurrentStep() {
        val currentStep = getCurrentStep() ?: return

        when (currentStep) {
            OnboardingStep.USERNAME -> proceedFromUsername()
            OnboardingStep.DATE_OF_BIRTH -> proceedFromDOB()
            OnboardingStep.RACE_ETHNICITY -> proceedFromRaceEthnicity()
            OnboardingStep.GENDER_IDENTITY -> proceedFromGenderIdentity()
            OnboardingStep.EMERGENCY_CONTACT -> proceedFromEmergencyContact()
            OnboardingStep.WRISTBAND -> proceedFromWristband()
            OnboardingStep.TERMS_ACCEPTANCE -> proceedFromTermsAcceptance()
        }
    }

    /**
     * Validate and proceed from username screen.
     */
    private fun proceedFromUsername() {
        val username = _formState.value.username
        if (!validateUsername(username)) {
            _formState.update { it.copy(usernameError = "Username must be 3-30 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            val result = onboardingRepository.saveUsername(username)
            result.onSuccess { response ->
                // Clear error and proceed to next step
                _formState.update { it.copy(usernameError = null) }
                proceedToNextStep()
                _uiState.value = OnboardingUiState.Idle
            }.onFailure { error ->
                _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to save username")
            }
        }
    }

    /**
     * Update username field.
     */
    fun updateUsername(username: String) {
        _formState.update { it.copy(username = username, usernameError = null) }
    }

    /**
     * Update date of birth and validate.
     */
    fun updateDateOfBirth(date: String) {
        val error = validateDOB(date)
        _formState.update { it.copy(dateOfBirth = date, dobError = error) }
    }

    /**
     * Proceed from Screen 1 to Screen 2.
     */
    fun proceedFromDOB() {
        val current = _formState.value
        val error = validateDOB(current.dateOfBirth)

        if (error != null) {
            _formState.update { it.copy(dobError = error) }
            return
        }

        proceedToNextStep()
    }

    /**
     * Toggle race/ethnicity selection.
     */
    fun toggleRaceEthnicity(option: String) {
        _formState.update { state ->
            val updated = state.selectedRaceEthnicity.toMutableList()
            if (updated.contains(option)) {
                updated.remove(option)
            } else {
                updated.add(option)
            }
            state.copy(selectedRaceEthnicity = updated)
        }
    }

    /**
     * Update race/ethnicity custom text.
     */
    fun updateRaceEthnicityText(text: String) {
        _formState.update { it.copy(raceEthnicityText = text) }
    }

    /**
     * Proceed from Screen 2 to Screen 3.
     */
    fun proceedFromRaceEthnicity() {
        // No validation required, but at least one should be selected ideally
        proceedToNextStep()
    }

    /**
     * Update gender identity selection.
     */
    fun updateGenderIdentity(option: String) {
        _formState.update { it.copy(selectedGenderIdentity = option) }
    }

    /**
     * Update gender identity custom text.
     */
    fun updateGenderIdentityText(text: String) {
        _formState.update { it.copy(genderIdentityText = text) }
    }

    /**
     * Proceed from Screen 3 to Screen 4.
     */
    fun proceedFromGenderIdentity() {
        proceedToNextStep()
    }

    /**
     * Update emergency contact name.
     */
    fun updateEmergencyContactName(name: String) {
        _formState.update { it.copy(emergencyContactName = name, emergencyContactError = null) }
    }

    /**
     * Update emergency contact phone.
     */
    fun updateEmergencyContactPhone(phone: String) {
        _formState.update { it.copy(emergencyContactPhone = phone, emergencyContactError = null) }
    }

    /**
     * Update emergency contact relationship.
     */
    fun updateEmergencyContactRelationship(relationship: String) {
        _formState.update { it.copy(emergencyContactRelationship = relationship) }
    }

    /**
     * Proceed from Screen 4 to Screen 5 (wristband).
     */
    fun proceedFromEmergencyContact() {
        val current = _formState.value
        val error = validateEmergencyContact(current.emergencyContactName, current.emergencyContactPhone)

        if (error != null) {
            _formState.update { it.copy(emergencyContactError = error) }
            return
        }

        // Save emergency contact
        saveEmergencyContactToBackend()
    }

    /**
     * Save emergency contact to backend and proceed to next screen.
     */
    private fun saveEmergencyContactToBackend() {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading

            val current = _formState.value
            val currentFestivalId = _festivalId.value  // Get latest festival_id

            try {
                // Create emergency contact request with festival_id from RPC response
                val request = SaveEmergencyContactRequest(
                    festival_id = currentFestivalId,
                    external_name = current.emergencyContactName,
                    external_phone_e164 = normalizePhoneNumber(current.emergencyContactPhone),
                    relationship = current.emergencyContactRelationship.ifBlank { null },
                    is_primary = true  // First emergency contact is primary
                )

                val result = onboardingRepository.saveEmergencyContact(request)

                result.onSuccess { response ->
                    proceedToNextStep()
                    _uiState.value = OnboardingUiState.Idle
                }.onFailure { error ->
                    _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to save emergency contact")
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Unknown error saving emergency contact")
            }
        }
    }

    /**
     * Update wristband code.
     */
    fun updateWristbandCode(code: String) {
        _formState.update { it.copy(wristbandCode = code, wristbandError = null) }
    }

    /**
     * Proceed from wristband screen.
     */
    private fun proceedFromWristband() {
        proceedToNextStep()
    }

    /**
     * Update terms acceptance.
     */
    fun updateTermsAcceptance(accepted: Boolean) {
        _formState.update { it.copy(termsAccepted = accepted) }
    }

    /**
     * Proceed from terms acceptance screen.
     */
    private fun proceedFromTermsAcceptance() {
        val current = _formState.value
        if (!current.termsAccepted) {
            _uiState.value = OnboardingUiState.Error("You must accept the terms to proceed")
            return
        }
        proceedToNextStep()
    }

    /**
     * Submit entire onboarding flow.
     *
     * When called from the last step (TERMS_ACCEPTANCE), this saves all onboarding data.
     * The API response will indicate what fields are still missing (if any).
     *
     * If the response has missing fields, rebuild the steps and continue to the next screen.
     * If no missing fields, the onboarding is complete.
     */
    fun submitOnboarding() {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading

            val current = _formState.value

            // Validate current step data
            when {
                current.termsAccepted && current.wristbandCode.isBlank() -> {
                    _uiState.value = OnboardingUiState.Error("Wristband code is required")
                    return@launch
                }
            }

            try {
                // Save demographics (final submission)
                val demographicsRequest = SaveDemographicsRequest(
                    dob = current.dateOfBirth.ifBlank { null },
                    race_ethnicity = current.selectedRaceEthnicity.ifEmpty { null },
                    race_ethnicity_text = current.raceEthnicityText.ifBlank { null },
                    gender_identity = current.selectedGenderIdentity.ifBlank { null },
                    gender_identity_text = current.genderIdentityText.ifBlank { null },
                    wristband_code = current.wristbandCode.ifBlank { null },
                    terms_acceptance = if (current.termsAccepted) true else null
                )

                val result = onboardingRepository.saveDemographics(demographicsRequest)

                result.onSuccess { response ->
                    // Check if there are missing fields
                    if (!response.missing.isNullOrEmpty()) {
                        // API indicates more fields are needed
                        // Rebuild the steps to include the missing fields
                        setMissingFields(response.missing)

                        // DON'T call proceedToNextStep() here!
                        // setMissingFields() already reset currentStepIndex to 0
                        // The pager will automatically show the first missing step
                        // If only 1 step missing (e.g., terms_acceptance), it will show that

                        _uiState.value = OnboardingUiState.Idle
                    } else if (response.activated == true) {
                        // No missing fields and activated = true → onboarding complete
                        _uiState.value = OnboardingUiState.OnboardingComplete
                    } else {
                        // Saved but not activated and no missing fields reported
                        _uiState.value = OnboardingUiState.Success("Onboarding saved successfully.")
                    }
                }.onFailure { error ->
                    _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to submit onboarding")
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Validate emergency contact: name and phone required, phone must have country code.
     */
    private fun validateEmergencyContact(name: String, phone: String): String? {
        return when {
            name.isBlank() -> "Emergency contact name is required"
            phone.isBlank() -> "Emergency contact phone is required"
            !phone.startsWith("+") -> "Phone number must include country code (e.g., +1, +44, +91)"
            else -> null
        }
    }

    /**
     * Normalize phone number to E.164 format.
     * For now, simple formatting. In production, use Google's libphonenumber.
     */
    private fun normalizePhoneNumber(phone: String): String {
        // Remove all non-digit characters except leading +
        val cleaned = phone.replace(Regex("[^\\d+]"), "")
        // If it doesn't start with +, assume US and add +1
        return if (cleaned.startsWith("+")) {
            cleaned
        } else {
            "+1${cleaned.takeLast(10)}"
        }
    }

    /**
     * Validate date of birth: not in future, within 120 years.
     */
    private fun validateDOB(dateStr: String): String? {
        if (dateStr.isBlank()) {
            return "Date of birth is required"
        }

        return try {
            val dob = dateFormatter.parse(dateStr) ?: return "Invalid date format"
            val today = Calendar.getInstance()
            val dobCal = Calendar.getInstance().apply { time = dob }

            when {
                dobCal > today -> "Date of birth cannot be in the future"
                today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR) > 120 -> "Date of birth is too old (max 120 years)"
                else -> null
            }
        } catch (e: Exception) {
            "Invalid date format. Use YYYY-MM-DD"
        }
    }

    /**
     * Validate username: 3-30 characters.
     */
    private fun validateUsername(username: String): Boolean {
        return username.length in 3..30
    }

    /**
     * Factory for creating OnboardingViewModel instances.
     */
    companion object {
        fun createFactory(
            onboardingRepository: OnboardingRepository,
            defaultFestivalId: String = "297d5837-a7b6-49a4-873b-4e3b17b60657"
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(onboardingRepository, defaultFestivalId) as T
                }
            }
        }
    }
}
