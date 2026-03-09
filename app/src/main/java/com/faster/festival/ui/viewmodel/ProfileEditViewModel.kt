package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.models.ProfileSummary
import com.faster.festival.data.repository.ProfileRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State for Profile Edit operations */
sealed class ProfileEditUiState {
    data object Idle : ProfileEditUiState()
    data object Loading : ProfileEditUiState()
    data class Success(val message: String, val updatedProfile: ProfileSummary? = null) :
            ProfileEditUiState()
    data class Error(val message: String, val retryAction: (() -> Unit)? = null) :
            ProfileEditUiState()
}

/** Form state for profile editing */
data class ProfileEditFormState(
        val firstName: String = "",
        val lastName: String = "",
        val firstNameError: String? = null,
        val lastNameError: String? = null,
        val isFormValid: Boolean = false,
        val contactName: String = "",
        val contactPhone: String = "",
        val contactRelationship: String = "",
        val contactNameError: String? = null,
        val contactPhoneError: String? = null,
        val dateOfBirth: String = "",
        val genderIdentity: String = "",
        val raceEthnicity: String = "" // For simplicity, single string input for race/ethnicity
)

/**
 * ViewModel for Profile Edit Screen Handles saving legal name, demographics, uploading avatar,
 * managing emergency contacts
 */
class ProfileEditViewModel(
        private val profileRepository: ProfileRepository,
        private val sessionManager: EncryptedSessionManager
) : ViewModel() {

    private val _editState = MutableStateFlow<ProfileEditUiState>(ProfileEditUiState.Idle)
    val editState: StateFlow<ProfileEditUiState> = _editState.asStateFlow()

    private val _formState = MutableStateFlow(ProfileEditFormState())
    val formState: StateFlow<ProfileEditFormState> = _formState.asStateFlow()

    private var lastRetryAction: (() -> Unit)? = null

    /** Update first name field */
    fun updateFirstName(value: String) {
        _formState.update { it.copy(firstName = value, firstNameError = null) }
        validateForm()
    }

    /** Update last name field */
    fun updateLastName(value: String) {
        _formState.update { it.copy(lastName = value, lastNameError = null) }
        validateForm()
    }

    /** Validate form fields */
    private fun validateForm() {
        val state = _formState.value
        var isValid = true
        var firstNameErr: String? = null
        var lastNameErr: String? = null

        if (state.firstName.isBlank()) {
            firstNameErr = "First name is required"
            isValid = false
        } else if (state.firstName.length < 2) {
            firstNameErr = "First name must be at least 2 characters"
            isValid = false
        }

        if (state.lastName.isBlank()) {
            lastNameErr = "Last name is required"
            isValid = false
        } else if (state.lastName.length < 2) {
            lastNameErr = "Last name must be at least 2 characters"
            isValid = false
        }

        _formState.update {
            it.copy(
                    firstNameError = firstNameErr,
                    lastNameError = lastNameErr,
                    isFormValid = isValid
            )
        }
    }

    /** Save legal name to API */
    fun saveLegalName() {
        val state = _formState.value
        if (!state.isFormValid) {
            validateForm()
            return
        }

        viewModelScope.launch {
            _editState.value = ProfileEditUiState.Loading
            val token = sessionManager.getAccessToken()

            if (token.isNullOrBlank()) {
                _editState.value =
                        ProfileEditUiState.Error(
                                "Session expired. Please log in again.",
                                { saveLegalName() }
                        )
                return@launch
            }

            profileRepository.saveLegalName(state.firstName, state.lastName, token).collect { result
                ->
                _editState.value =
                        result.fold(
                                onSuccess = { profile ->
                                    ProfileEditUiState.Success("Name saved successfully", profile)
                                },
                                onFailure = { error ->
                                    ProfileEditUiState.Error(
                                            error.message ?: "Failed to save name",
                                            { saveLegalName() }
                                    )
                                }
                        )
            }
        }
    }

    /** Upload avatar image */
    fun uploadAvatar(imageFile: File) {
        if (!imageFile.exists()) {
            _editState.value =
                    ProfileEditUiState.Error("File not found", { uploadAvatar(imageFile) })
            return
        }

        viewModelScope.launch {
            _editState.value = ProfileEditUiState.Loading
            val token = sessionManager.getAccessToken()

            if (token.isNullOrBlank()) {
                _editState.value =
                        ProfileEditUiState.Error(
                                "Session expired. Please log in again.",
                                { uploadAvatar(imageFile) }
                        )
                return@launch
            }

            profileRepository.uploadAvatar(imageFile, token).collect { result ->
                _editState.value =
                        result.fold(
                                onSuccess = { _url ->
                                    ProfileEditUiState.Success("Avatar uploaded successfully")
                                },
                                onFailure = { error ->
                                    // ✅ Parse error message for user-friendly output
                                    val userMessage = parseAvatarUploadError(error)
                                    ProfileEditUiState.Error(
                                            userMessage,
                                            { uploadAvatar(imageFile) }
                                    )
                                }
                        )
            }
        }
    }

    /**
     * ✅ Parse avatar upload error and return human-readable message
     * Handles: File too large, invalid format, server errors, etc.
     */
    private fun parseAvatarUploadError(error: Throwable): String {
        val errorMessage = error.message ?: "Unknown error"

        return when {
            errorMessage.contains("413", ignoreCase = true) ||
            errorMessage.contains("too large", ignoreCase = true) ||
            errorMessage.contains("file exceeds", ignoreCase = true) ->
                "Image is too large. Please compress to under 5MB and try again."

            errorMessage.contains("415", ignoreCase = true) ||
            errorMessage.contains("unsupported", ignoreCase = true) ||
            errorMessage.contains("invalid format", ignoreCase = true) ->
                "Unsupported image format. Please use JPG, PNG, or WebP."

            errorMessage.contains("401", ignoreCase = true) ||
            errorMessage.contains("unauthorized", ignoreCase = true) ->
                "Authorization failed. Please log in again."

            errorMessage.contains("500", ignoreCase = true) ||
            errorMessage.contains("server error", ignoreCase = true) ->
                "Server error. Please try again later."

            errorMessage.contains("network", ignoreCase = true) ||
            errorMessage.contains("connection", ignoreCase = true) ->
                "Network error. Please check your connection and try again."

            else -> errorMessage
        }
    }
    /** Get signed avatar URL */
    fun getAvatarUrl(): String? {
        var result: String? = null
        viewModelScope.launch {
            val token = sessionManager.getAccessToken()
            if (token != null) {
                profileRepository.getAvatarUrl(token).collect { urlResult ->
                    result = urlResult.getOrNull()
                }
            }
        }
        return result
    }

    /** Update date of birth */
    fun updateDateOfBirth(value: String) {
        _formState.update { it.copy(dateOfBirth = value) }
    }

    /** Update gender identity */
    fun updateGenderIdentity(value: String) {
        _formState.update { it.copy(genderIdentity = value) }
    }

    /** Update race/ethnicity */
    fun updateRaceEthnicity(value: String) {
        _formState.update { it.copy(raceEthnicity = value) }
    }

    /** Save demographics */
    fun saveDemographics() {
        viewModelScope.launch {
            _editState.value = ProfileEditUiState.Loading
            val token = sessionManager.getAccessToken()

            if (token.isNullOrBlank()) {
                _editState.value =
                        ProfileEditUiState.Error(
                                "Session expired. Please log in again.",
                                { saveDemographics() }
                        )
                return@launch
            }

            val state = _formState.value
            val raceList =
                    if (state.raceEthnicity.isNotBlank()) listOf(state.raceEthnicity) else null

            profileRepository.saveDemographics(
                            dateOfBirth = state.dateOfBirth.ifBlank { null },
                            genderIdentity = state.genderIdentity.ifBlank { null },
                            raceEthnicity = raceList,
                            accessToken = token
                    )
                    .collect { result ->
                        _editState.value =
                                result.fold(
                                        onSuccess = {
                                            ProfileEditUiState.Success(
                                                    "Demographics saved successfully",
                                                    null
                                            )
                                        },
                                        onFailure = { error ->
                                            ProfileEditUiState.Error(
                                                    error.message ?: "Failed to save demographics",
                                                    { saveDemographics() }
                                            )
                                        }
                                )
                    }
        }
    }

    /** Update emergency contact name */
    fun updateContactName(value: String) {
        _formState.update { it.copy(contactName = value, contactNameError = null) }
    }

    /** Update emergency contact phone */
    fun updateContactPhone(value: String) {
        _formState.update { it.copy(contactPhone = value, contactPhoneError = null) }
    }

    /** Update emergency contact relationship */
    fun updateContactRelationship(value: String) {
        _formState.update { it.copy(contactRelationship = value) }
    }

    /** Validate emergency contact fields */
    private fun validateContactForm(): Boolean {
        val state = _formState.value
        var isValid = true
        var nameErr: String? = null
        var phoneErr: String? = null

        if (state.contactName.isBlank()) {
            nameErr = "Contact name is required"
            isValid = false
        }

        if (state.contactPhone.isBlank()) {
            phoneErr = "Contact phone is required"
            isValid = false
        } else if (!state.contactPhone.startsWith("+")) {
            phoneErr = "Phone must include country code (e.g., +1234567890)"
            isValid = false
        }

        _formState.update { it.copy(contactNameError = nameErr, contactPhoneError = phoneErr) }

        return isValid
    }

    /** Save emergency contact */
    fun saveEmergencyContact() {
        if (!validateContactForm()) return

        viewModelScope.launch {
            _editState.value = ProfileEditUiState.Loading
            val token = sessionManager.getAccessToken()

            if (token.isNullOrBlank()) {
                _editState.value =
                        ProfileEditUiState.Error(
                                "Session expired. Please log in again.",
                                { saveEmergencyContact() }
                        )
                return@launch
            }

            val state = _formState.value
            profileRepository.saveEmergencyContact(
                            name = state.contactName,
                            phone = state.contactPhone,
                            relationship = state.contactRelationship.ifBlank { null },
                            accessToken = token
                    )
                    .collect { result ->
                        _editState.value =
                                result.fold(
                                        onSuccess = { profile ->
                                            // Clear form
                                            _formState.update {
                                                it.copy(
                                                        contactName = "",
                                                        contactPhone = "",
                                                        contactRelationship = ""
                                                )
                                            }
                                            ProfileEditUiState.Success(
                                                    "Emergency contact saved successfully",
                                                    profile
                                            )
                                        },
                                        onFailure = { error ->
                                            ProfileEditUiState.Error(
                                                    error.message
                                                            ?: "Failed to save emergency contact",
                                                    { saveEmergencyContact() }
                                            )
                                        }
                                )
                    }
        }
    }

    /** Delete emergency contact */
    fun deleteEmergencyContact(contactId: String) {
        viewModelScope.launch {
            _editState.value = ProfileEditUiState.Loading
            val token = sessionManager.getAccessToken()

            if (token.isNullOrBlank()) {
                _editState.value =
                        ProfileEditUiState.Error(
                                "Session expired. Please log in again.",
                                { deleteEmergencyContact(contactId) }
                        )
                return@launch
            }

            profileRepository.deleteEmergencyContact(contactId, token).collect { result ->
                _editState.value =
                        result.fold(
                                onSuccess = { profile ->
                                    ProfileEditUiState.Success("Emergency contact deleted", profile)
                                },
                                onFailure = { error ->
                                    ProfileEditUiState.Error(
                                            error.message ?: "Failed to delete emergency contact",
                                            { deleteEmergencyContact(contactId) }
                                    )
                                }
                        )
            }
        }
    }

    /**
     * ✅ Compress image to max 5MB for API constraints
     * Uses android.graphics.Bitmap for compression
     * Returns compressed file or null if compression fails
     */
    fun compressImageToMaxSize(
        context: android.content.Context,
        imageFile: File,
        maxSizeBytes: Long = 5 * 1024 * 1024 // 5MB
    ): File? {
        return try {
            if (!imageFile.exists()) return null

            // Decode bitmap
            val originalBitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: return null

            // Check if compression needed
            if (originalBitmap.byteCount <= maxSizeBytes) {
                originalBitmap.recycle()
                return imageFile // Already small enough
            }

            // Compress iteratively
            var quality = 90
            val outputFile = File(context.cacheDir, "avatar_compressed_${System.currentTimeMillis()}.jpg")
            var fileSize: Long

            do {
                outputFile.delete()
                outputFile.outputStream().use { out ->
                    originalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
                }
                fileSize = outputFile.length()

                if (fileSize > maxSizeBytes && quality > 20) {
                    quality -= 10
                } else {
                    break
                }
            } while (fileSize > maxSizeBytes && quality > 20)

            // Scale down if still too large
            if (fileSize > maxSizeBytes) {
                val scaleFactor = 0.8
                val newWidth = (originalBitmap.width * scaleFactor).toInt()
                val newHeight = (originalBitmap.height * scaleFactor).toInt()

                val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                    originalBitmap,
                    newWidth,
                    newHeight,
                    true
                )

                quality = 85
                do {
                    outputFile.delete()
                    outputFile.outputStream().use { out ->
                        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
                    }
                    fileSize = outputFile.length()

                    if (fileSize > maxSizeBytes && quality > 20) {
                        quality -= 10
                    } else {
                        break
                    }
                } while (fileSize > maxSizeBytes && quality > 20)

                scaledBitmap.recycle()
            }

            originalBitmap.recycle()
            android.util.Log.d("ProfileEditViewModel", "Image compressed to ${outputFile.length()} bytes")
            outputFile
        } catch (e: Exception) {
            android.util.Log.e("ProfileEditViewModel", "Compression failed", e)
            null
        }
    }

    /** Factory for creating instances */
    class Factory(
            private val profileRepository: ProfileRepository,
            private val sessionManager: EncryptedSessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileEditViewModel::class.java)) {
                return ProfileEditViewModel(profileRepository, sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    /**
     * ✅ Set error state manually
     * Used for non-API errors like file conversion failures
     *
     * @param message Error message to display
     */
    fun setError(message: String) {
        _editState.value = ProfileEditUiState.Error(message, null)
    }
}
