package com.faster.festival.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.util.Log
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.GenderIdentity
import com.faster.festival.data.model.SaveDemographicsRequest
import com.faster.festival.data.model.SaveEmergencyContactRequest
import com.faster.festival.data.model.SaveProfileNameRequest
import com.faster.festival.data.repository.OnboardingRepository
import com.faster.festival.utils.DeviceContact
import com.faster.festival.utils.DeviceContactsHelper
import com.faster.festival.utils.PhoneNumberUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * UI state for the 6-step onboarding flow.
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.PROFILE_DETAILS,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false,

    // Step 1: Profile Details
    val dateOfBirth: String = "",
    val genderIdentity: String = "",

    // Step 2: Emergency Contact
    val emergencyName: String = "",
    val emergencyPhone: String = "",
    val emergencyRelationship: String = "",
    val deviceContactsEnabled: Boolean = false,
    val contactSuggestions: List<DeviceContact> = emptyList(),

    // Step 3: Confirm Account Details
    val legalName: String = "",
    val phoneNumber: String = "",
    val email: String = "",

    // Step 4: Username
    val username: String = "",

    // Step 5: Accept Terms
    val termsAccepted: Boolean = false,

    // Step 6: Wristband
    val wristbandCode: String = "",

    // Validation errors
    val dateOfBirthError: String? = null,
    val emergencyNameError: String? = null,
    val emergencyPhoneError: String? = null,
    val legalNameError: String? = null,
    val phoneNumberError: String? = null,
    val usernameError: String? = null
)

/**
 * ViewModel for the 6-step onboarding flow.
 *
 * Steps: PROFILE_DETAILS -> EMERGENCY_CONTACT -> CONFIRM_DETAILS
 *        -> USERNAME -> ACCEPT_TERMS -> WRISTBAND
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val sessionManager: EncryptedSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _festivalId = MutableStateFlow("")
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val steps = OnboardingStep.entries.toList()

    init {
        // Load email from session
        val email = sessionManager.getUserEmail() ?: ""
        _uiState.update { it.copy(email = email) }
        initializeOnboarding()
    }

    private fun initializeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = onboardingRepository.ensureOnboarding()
            result.onSuccess { festivalId ->
                _festivalId.value = festivalId
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to initialize onboarding"
                    )
                }
            }
        }
    }

    // ============================================================
    // Field update methods
    // ============================================================

    fun updateDateOfBirth(value: String) {
        _uiState.update { it.copy(dateOfBirth = value, dateOfBirthError = null) }
    }

    fun updateGenderIdentity(value: String) {
        _uiState.update { it.copy(genderIdentity = value) }
    }

    fun updateEmergencyName(value: String) {
        _uiState.update { it.copy(emergencyName = value, emergencyNameError = null) }
    }

    /**
     * Called as the user types in the name field.
     * If device contacts are enabled, searches for matching contacts.
     */
    fun updateEmergencyNameWithSearch(value: String, context: Context) {
        _uiState.update { it.copy(emergencyName = value, emergencyNameError = null) }
        if (_uiState.value.deviceContactsEnabled) {
            searchDeviceContacts(value, context)
        }
    }

    fun updateEmergencyPhone(value: String) {
        _uiState.update { it.copy(emergencyPhone = value, emergencyPhoneError = null) }
    }

    fun updateEmergencyRelationship(value: String) {
        _uiState.update { it.copy(emergencyRelationship = value) }
    }

    /**
     * Mark device contacts as enabled (after permission is granted).
     */
    fun enableDeviceContacts() {
        _uiState.update { it.copy(deviceContactsEnabled = true) }
    }

    /**
     * Mark device contacts as disabled (toggle off or permission denied).
     */
    fun disableDeviceContacts() {
        _uiState.update { it.copy(deviceContactsEnabled = false, contactSuggestions = emptyList()) }
    }

    /**
     * Search device contacts by partial name match.
     */
    private fun searchDeviceContacts(query: String, context: Context) {
        val results = DeviceContactsHelper.searchContacts(context, query)
        _uiState.update { it.copy(contactSuggestions = results) }
    }

    /**
     * Select a device contact — auto-fill name and phone.
     */
    fun selectDeviceContact(contact: DeviceContact) {
        _uiState.update {
            it.copy(
                emergencyName = contact.name,
                emergencyPhone = contact.normalizedPhone,
                emergencyNameError = null,
                emergencyPhoneError = null,
                contactSuggestions = emptyList()
            )
        }
    }

    /**
     * Dismiss contact suggestions without selecting.
     */
    fun dismissContactSuggestions() {
        _uiState.update { it.copy(contactSuggestions = emptyList()) }
    }

    fun updateLegalName(value: String) {
        _uiState.update { it.copy(legalName = value, legalNameError = null) }
    }

    fun updatePhoneNumber(value: String) {
        _uiState.update { it.copy(phoneNumber = value, phoneNumberError = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, usernameError = null) }
    }

    fun updateTermsAccepted(value: Boolean) {
        _uiState.update { it.copy(termsAccepted = value) }
    }

    fun updateWristbandCode(value: String) {
        _uiState.update { it.copy(wristbandCode = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ============================================================
    // Navigation
    // ============================================================

    fun previousStep() {
        val current = _uiState.value.currentStep
        val currentIndex = steps.indexOf(current)
        if (currentIndex > 0) {
            _uiState.update { it.copy(currentStep = steps[currentIndex - 1], error = null) }
        }
    }

    val currentStepIndex: Int
        get() = steps.indexOf(_uiState.value.currentStep)

    val totalSteps: Int
        get() = steps.size

    val isFirstStep: Boolean
        get() = currentStepIndex == 0

    // ============================================================
    // Save methods
    // ============================================================

    /**
     * Step 1: Validate and save demographics (DOB + gender identity).
     * On success, advances to EMERGENCY_CONTACT.
     */
    fun saveProfileDetails() {
        val state = _uiState.value

        val dobError = validateDateOfBirth(state.dateOfBirth)
        if (dobError != null) {
            _uiState.update { it.copy(dateOfBirthError = dobError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val genderApiValue = state.genderIdentity.ifBlank { null }
                ?.let { GenderIdentity.toApiValue(it) ?: it.lowercase().replace(" ", "_") }
            val request = SaveDemographicsRequest(
                dob = state.dateOfBirth,
                gender_identity = genderApiValue
            )
            val result = onboardingRepository.saveDemographics(request)

            result.onSuccess {
                Log.d("OnboardingVM", "saveDemographics succeeded, advancing to EMERGENCY_CONTACT")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = OnboardingStep.EMERGENCY_CONTACT,
                        error = null
                    )
                }
            }.onFailure { error ->
                Log.e("OnboardingVM", "saveDemographics failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to save profile details"
                    )
                }
            }
        }
    }

    /**
     * Step 2: Validate and save emergency contact.
     * On success, advances to CONFIRM_DETAILS.
     */
    fun saveEmergencyContact() {
        val state = _uiState.value

        if (state.emergencyName.isBlank()) {
            _uiState.update { it.copy(emergencyNameError = "Contact name is required") }
            return
        }

        if (state.emergencyPhone.isBlank()) {
            _uiState.update { it.copy(emergencyPhoneError = "Phone number is required") }
            return
        }

        val normalizedPhone = PhoneNumberUtils.normalizeToE164(state.emergencyPhone)
        if (!PhoneNumberUtils.isValidE164(normalizedPhone)) {
            _uiState.update {
                it.copy(emergencyPhoneError = "Invalid phone number. Include country code (e.g. +1)")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val request = SaveEmergencyContactRequest(
                festival_id = _festivalId.value.ifBlank { null },
                external_name = state.emergencyName,
                external_phone_e164 = normalizedPhone,
                relationship = state.emergencyRelationship.ifBlank { null },
                is_primary = true
            )

            val result = onboardingRepository.saveEmergencyContact(request)

            result.onSuccess {
                Log.d("OnboardingVM", "saveEmergencyContact succeeded, advancing to CONFIRM_DETAILS")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = OnboardingStep.CONFIRM_DETAILS,
                        error = null
                    )
                }
            }.onFailure { error ->
                Log.e("OnboardingVM", "saveEmergencyContact failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to save emergency contact"
                    )
                }
            }
        }
    }

    /**
     * Step 3: Save legal name and advance to USERNAME.
     */
    fun createAccount() {
        val state = _uiState.value

        if (state.legalName.isBlank()) {
            _uiState.update { it.copy(legalNameError = "Full legal name is required") }
            return
        }

        if (state.phoneNumber.isBlank()) {
            _uiState.update { it.copy(phoneNumberError = "Phone number is required") }
            return
        }

        // Split legal name into first/last
        val nameParts = state.legalName.trim().split(" ", limit = 2)
        val firstName = nameParts[0]
        val lastName = if (nameParts.size > 1) nameParts[1] else ""

        if (lastName.isBlank()) {
            _uiState.update { it.copy(legalNameError = "Please enter your first and last name") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val nameResult = onboardingRepository.saveProfileName(
                SaveProfileNameRequest(
                    legalFirstName = firstName,
                    legalLastName = lastName
                )
            )
            nameResult.onSuccess {
                Log.d("OnboardingVM", "saveProfileName succeeded, advancing to USERNAME")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = OnboardingStep.USERNAME,
                        error = null
                    )
                }
            }.onFailure { error ->
                Log.e("OnboardingVM", "saveProfileName failed: ${error.message}")
                _uiState.update {
                    it.copy(isLoading = false, error = error.message ?: "Failed to save name")
                }
            }
        }
    }

    /**
     * Step 4: Validate and save username.
     * On success, advances to ACCEPT_TERMS.
     */
    fun saveUsername() {
        val state = _uiState.value
        val username = state.username.trim()

        if (username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Username is required") }
            return
        }
        if (username.length < 3) {
            _uiState.update { it.copy(usernameError = "Username must be at least 3 characters") }
            return
        }
        if (username.length > 30) {
            _uiState.update { it.copy(usernameError = "Username must be 30 characters or less") }
            return
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            _uiState.update { it.copy(usernameError = "Only letters, numbers, underscores, and hyphens allowed") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = onboardingRepository.saveUsername(username)

            result.onSuccess {
                Log.d("OnboardingVM", "saveUsername succeeded, advancing to ACCEPT_TERMS")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = OnboardingStep.ACCEPT_TERMS,
                        error = null
                    )
                }
            }.onFailure { error ->
                Log.e("OnboardingVM", "saveUsername failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to save username"
                    )
                }
            }
        }
    }

    /**
     * Step 5: Accept terms and advance to WRISTBAND.
     */
    fun acceptTermsAndContinue() {
        val state = _uiState.value

        if (!state.termsAccepted) {
            _uiState.update { it.copy(error = "You must accept the terms to continue") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = onboardingRepository.acceptTerms()

            result.onSuccess {
                Log.d("OnboardingVM", "acceptTerms succeeded, advancing to WRISTBAND")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = OnboardingStep.WRISTBAND,
                        error = null
                    )
                }
            }.onFailure { error ->
                Log.e("OnboardingVM", "acceptTerms failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to accept terms"
                    )
                }
            }
        }
    }

    /**
     * Step 6: Pair wristband (optional). On success or skip, marks onboarding complete.
     */
    fun saveWristband() {
        val code = _uiState.value.wristbandCode.trim()

        if (code.isBlank()) {
            // Skip — mark complete
            sessionManager.setOnboardingJustCompleted(true)
            _uiState.update { it.copy(isComplete = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = onboardingRepository.saveWristband(code)

            result.onSuccess {
                Log.d("OnboardingVM", "saveWristband succeeded, onboarding complete")
                sessionManager.setOnboardingJustCompleted(true)
                _uiState.update {
                    it.copy(isLoading = false, isComplete = true, error = null)
                }
            }.onFailure { error ->
                Log.e("OnboardingVM", "saveWristband failed: ${error.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to pair wristband"
                    )
                }
            }
        }
    }

    fun skipWristband() {
        sessionManager.setOnboardingJustCompleted(true)
        _uiState.update { it.copy(isComplete = true) }
    }

    // ============================================================
    // Validation helpers
    // ============================================================

    private fun validateDateOfBirth(dateStr: String): String? {
        if (dateStr.isBlank()) {
            return "Date of birth is required"
        }
        return try {
            val dob = dateFormatter.parse(dateStr) ?: return "Invalid date format"
            val today = Calendar.getInstance()
            val dobCal = Calendar.getInstance().apply { time = dob }
            when {
                dobCal.after(today) -> "Date of birth cannot be in the future"
                today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR) > 120 ->
                    "Please enter a valid date of birth"
                else -> null
            }
        } catch (e: Exception) {
            "Invalid date format. Use YYYY-MM-DD"
        }
    }

    // ============================================================
    // Factory
    // ============================================================

    companion object {
        fun createFactory(
            onboardingRepository: OnboardingRepository,
            sessionManager: EncryptedSessionManager
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(onboardingRepository, sessionManager) as T
                }
            }
        }
    }
}
