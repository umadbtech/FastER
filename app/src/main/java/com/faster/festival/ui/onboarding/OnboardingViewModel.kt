package com.faster.festival.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.model.SaveDemographicsRequest
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

    // Screen 4: Wristband
    val wristbandCode: String = "",
    val wristbandError: String? = null,

    // Current screen (0-3)
    val currentScreen: Int = 0,

    // Username (saved before screen 1)
    val username: String = "",
    val usernameError: String? = null
)

/**
 * ViewModel for managing onboarding flow state and API interactions.
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(OnboardingFormState())
    val formState: StateFlow<OnboardingFormState> = _formState.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Initialize onboarding flow.
     */
    fun initializeOnboarding() {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            val result = onboardingRepository.ensureOnboarding()
            result.onSuccess {
                _uiState.value = OnboardingUiState.Idle
            }.onFailure { error ->
                _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to initialize onboarding")
            }
        }
    }

    /**
     * Save username and proceed to Screen 1 (DOB).
     */
    fun saveUsername(username: String) {
        if (!validateUsername(username)) {
            _formState.update { it.copy(usernameError = "Username must be 3-30 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            val result = onboardingRepository.saveUsername(username)
            result.onSuccess { response ->
                _formState.update { it.copy(username = username, usernameError = null, currentScreen = 0) }
                _uiState.value = OnboardingUiState.Idle
            }.onFailure { error ->
                _uiState.value = OnboardingUiState.Error(error.message ?: "Failed to save username")
            }
        }
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

        _formState.update { it.copy(currentScreen = 1) }
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
        _formState.update { it.copy(currentScreen = 2) }
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
        _formState.update { it.copy(currentScreen = 3) }
    }

    /**
     * Update wristband code.
     */
    fun updateWristbandCode(code: String) {
        _formState.update { it.copy(wristbandCode = code, wristbandError = null) }
    }

    /**
     * Submit entire onboarding flow.
     */
    fun submitOnboarding() {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading

            val current = _formState.value

            // Validate all fields
            val dobError = validateDOB(current.dateOfBirth)
            if (dobError != null) {
                _uiState.value = OnboardingUiState.Error(dobError)
                return@launch
            }

            try {
                // Save demographics
                val demographicsRequest = SaveDemographicsRequest(
                    dob = current.dateOfBirth,
                    race_ethnicity = current.selectedRaceEthnicity.ifEmpty { null },
                    race_ethnicity_text = current.raceEthnicityText.ifBlank { null },
                    gender_identity = current.selectedGenderIdentity.ifBlank { null },
                    gender_identity_text = current.genderIdentityText.ifBlank { null },
                    wristband_code = current.wristbandCode.ifBlank { null }
                )

                val result = onboardingRepository.saveDemographics(demographicsRequest)

                result.onSuccess { response ->
                    if (response.activated == true && (response.missing?.isEmpty() == true || response.missing == null)) {
                        _uiState.value = OnboardingUiState.OnboardingComplete
                    } else {
                        _uiState.value = OnboardingUiState.Success("Onboarding saved. Please complete any remaining steps.")
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
     * Go back one screen.
     */
    fun goBack() {
        _formState.update { state ->
            if (state.currentScreen > 0) {
                state.copy(currentScreen = state.currentScreen - 1)
            } else {
                state
            }
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
        fun createFactory(onboardingRepository: OnboardingRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(onboardingRepository) as T
                }
            }
        }
    }
}
