package com.faster.festival.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.AppConfig
import com.faster.festival.data.model.SaveDemographicsRequest
import com.faster.festival.data.model.SaveEmergencyContactRequest
import com.faster.festival.data.repository.OnboardingRepository
import com.faster.festival.data.util.RetryHelper
import com.faster.festival.ui.util.ErrorMapper
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
 *
 * ✅ Improvements:
 * - Uses AppConfig for centralized configuration
 * - Uses ErrorMapper for consistent error handling
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val defaultFestivalId: String = AppConfig.DEFAULT_FESTIVAL_ID  // ✅ Use AppConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(OnboardingFormState())
    val formState: StateFlow<OnboardingFormState> = _formState.asStateFlow()

    // Mutable state for festival_id retrieved from RPC
    private val _festivalId = MutableStateFlow(defaultFestivalId)
    val festivalId: StateFlow<String> = _festivalId.asStateFlow()

    // Terms and Conditions text
    private val _termsAndConditionsText = MutableStateFlow<String?>(null)
    val termsAndConditionsText: StateFlow<String?> = _termsAndConditionsText.asStateFlow()

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
     * Load Terms and Conditions text.
     * In a real app, this could load from an API or local assets.
     */
    fun loadTermsAndConditions() {
        // Default T&C text - in production, this would come from API or assets
        val termsText = """
            TERMS AND CONDITIONS
            
            Last Updated: March 2026
            
            1. ACCEPTANCE OF TERMS
            By registering for this application and accessing the festival, you agree to be bound by these Terms and Conditions. If you do not agree to abide by the above, please do not use this service.
            
            2. USE LICENSE
            Permission is granted to temporarily download one copy of the materials (information or software) on the FastER Festival app for personal, non-commercial transitory viewing only. This is the grant of a license, not a transfer of title, and under this license you may not:
            • Modifying or copying the materials
            • Using the materials for any commercial purpose or for any public display
            • Attempting to decompile or reverse engineer any software contained on the app
            • Transferring the materials to another person or "mirroring" the materials on any other server
            • Removing any copyright or other proprietary notations from the materials
            • Transferring the materials to another person or "mirroring" the materials on any other server
            
            3. DISCLAIMER
            The materials on the FastER Festival app are provided on an 'as is' basis. The FastER Festival makes no warranties, expressed or implied, and hereby disclaims and negates all other warranties including, without limitation, implied warranties or conditions of merchantability, fitness for a particular purpose, or non-infringement of intellectual property or other violation of rights.
            
            4. LIMITATIONS
            In no event shall the FastER Festival or its suppliers be liable for any damages (including, without limitation, damages for loss of data or profit, or due to business interruption) arising out of the use or inability to use the materials on the FastER Festival app, even if the FastER Festival or an authorized representative has been notified orally or in writing of the possibility of such damage.
            
            5. ACCURACY OF MATERIALS
            The materials appearing on the FastER Festival app could include technical, typographical, or photographic errors. The FastER Festival does not warrant that any of the materials on the app are accurate, complete, or current. The FastER Festival may make changes to the materials contained on the app at any time without notice.
            
            6. LINKS
            The FastER Festival has not reviewed all of the sites linked to its website and is not responsible for the contents of any such linked site. The inclusion of any link does not imply endorsement by the FastER Festival of the site. Use of any such linked website is at the user's own risk.
            
            7. MODIFICATIONS
            The FastER Festival may revise these terms and conditions for its website at any time without notice. By using this website, you are agreeing to be bound by the then current version of these terms and conditions.
            
            8. GOVERNING LAW
            These terms and conditions are governed by and construed in accordance with the laws of the jurisdiction where the festival is held, and you irrevocably submit to the exclusive jurisdiction of the courts in that location.
            
            9. PRIVACY
            Your use of the app is also governed by our Privacy Policy, which you acknowledge that you have read and understood.
            
            10. CONTACT INFORMATION
            If you have any questions about these Terms and Conditions, please contact us at support@faster-festival.com
            
            By clicking "Accept," you acknowledge that you have read these Terms and Conditions and agree to be bound by them.
        """.trimIndent()

        _termsAndConditionsText.value = termsText
    }

    /**
     * Get the current step.
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
            try {
                // ✅ Use RetryHelper to handle 500/network errors with retry
                val result = RetryHelper.retryOnError {
                    onboardingRepository.saveUsername(username).getOrThrow()
                }

                // Clear error
                _formState.update { it.copy(usernameError = null) }

                // Check if onboarding is complete
                if (result.activated == true) {
                    // All steps complete!
                    _uiState.value = OnboardingUiState.OnboardingComplete
                } else {
                    // Simply proceed to next step in the current ordered list
                    proceedToNextStep()
                    _uiState.value = OnboardingUiState.Idle
                }
            } catch (error: Exception) {
                // ✅ Use ErrorMapper for consistent error messages
                _uiState.value = OnboardingUiState.Error(ErrorMapper.mapThrowableToMessage(error))
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
     * Proceed from Screen 3 to Screen 4 - Save demographics data.
     */
    fun proceedFromGenderIdentity() {
        saveDemographicsToBackend()
    }

    /**
     * Save demographics (DOB, race/ethnicity, gender) to backend.
     */
    private fun saveDemographicsToBackend() {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading

            val current = _formState.value

            try {
                // Create demographics request with all collected data
                val request = SaveDemographicsRequest(
                    dob = current.dateOfBirth,
                    race_ethnicity = current.selectedRaceEthnicity,
                    race_ethnicity_text = current.raceEthnicityText.ifBlank { null },
                    gender_identity = current.selectedGenderIdentity,
                    gender_identity_text = current.genderIdentityText.ifBlank { null }
                )

                val result = onboardingRepository.saveDemographics(request)

                result.onSuccess { response ->
                    // Check if onboarding is complete
                    if (response.activated == true) {
                        _uiState.value = OnboardingUiState.OnboardingComplete
                    } else {
                        // Simply proceed to next step in the current ordered list
                        proceedToNextStep()
                        _uiState.value = OnboardingUiState.Idle
                    }
                }.onFailure { error ->
                    _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to save demographics")
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Unknown error saving demographics")
            }
        }
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
                    // Check if onboarding is complete
                    if (response.activated == true) {
                        // All steps complete!
                        _uiState.value = OnboardingUiState.OnboardingComplete
                    } else {
                        // Simply proceed to next step in the current ordered list
                        proceedToNextStep()
                        _uiState.value = OnboardingUiState.Idle
                    }
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
        val wristbandCode = _formState.value.wristbandCode

        if (wristbandCode.isEmpty()) {
            // Skip wristband - it's optional
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
                    // All steps complete!
                    _uiState.value = OnboardingUiState.OnboardingComplete
                } else {
                    // Simply proceed to next step in the current ordered list
                    proceedToNextStep()
                    _uiState.value = OnboardingUiState.Idle
                }
            }.onFailure { error ->
                _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to save wristband")
            }
        }
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

        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            val result = onboardingRepository.acceptTerms()
            result.onSuccess { response ->
                // Onboarding should be complete after accepting terms
                if (response.activated == true) {
                    _uiState.value = OnboardingUiState.OnboardingComplete
                } else {
                    // Still more steps - proceed to next
                    proceedToNextStep()
                    _uiState.value = OnboardingUiState.Idle
                }
            }.onFailure { error ->
                _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to accept terms")
            }
        }
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

            // Validate terms acceptance (required for submission)
            if (!current.termsAccepted) {
                _uiState.value = OnboardingUiState.Error("You must accept the terms to proceed")
                return@launch
            }

            try {
                // Save demographics (final submission)
                // Note: wristband_code is OPTIONAL - can be null if user skipped pairing
                val demographicsRequest = SaveDemographicsRequest(
                    dob = current.dateOfBirth.ifBlank { null },
                    race_ethnicity = current.selectedRaceEthnicity.ifEmpty { null },
                    race_ethnicity_text = current.raceEthnicityText.ifBlank { null },
                    gender_identity = current.selectedGenderIdentity.ifBlank { null },
                    gender_identity_text = current.genderIdentityText.ifBlank { null },
                    wristband_code = current.wristbandCode.ifBlank { null },  // ✅ OPTIONAL - null allowed
                    terms_acceptance = if (current.termsAccepted) true else null
                )

                val result = onboardingRepository.saveDemographics(demographicsRequest)

                result.onSuccess { response ->
                    // After saving demographics, also call accept-terms endpoint
                    // ✅ This ensures terms_acceptance is properly recorded
                    val acceptTermsResult = onboardingRepository.acceptTerms()

                    acceptTermsResult.onSuccess { termsResponse ->
                        // Check if onboarding is complete
                        if (termsResponse.activated == true) {
                            // No missing fields and activated = true → onboarding complete
                            // Load profile summary to populate user data
                            val profileResult = onboardingRepository.getProfileSummary()
                            profileResult.onSuccess { profile ->
                                // Profile loaded successfully - stored in local state if needed
                                _uiState.value = OnboardingUiState.OnboardingComplete
                            }.onFailure { profileError ->
                                // Continue even if profile load fails
                                _uiState.value = OnboardingUiState.OnboardingComplete
                            }
                        } else if (!termsResponse.missing.isNullOrEmpty()) {
                            // API indicates more fields are needed
                            setMissingFields(termsResponse.missing)
                            _uiState.value = OnboardingUiState.Idle
                        } else {
                            // Saved but not activated
                            _uiState.value = OnboardingUiState.Success("Onboarding saved successfully.")
                        }
                    }.onFailure { acceptTermsError ->
                        _uiState.value = OnboardingUiState.Error(acceptTermsError.message ?: "Failed to accept terms")
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
