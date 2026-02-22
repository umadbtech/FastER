package com.faster.festival.ui.auth.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Enter Phone Number screen.
 * Exposes phoneNumber (E.164 when valid), countryCode, isValid, errorMessage and actions.
 */
class EnterPhoneViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _phoneNumber = MutableStateFlow("") // normalized E.164 when valid
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _countryCode = MutableStateFlow("+1")
    val countryCode: StateFlow<String> = _countryCode.asStateFlow()

    private val _isValid = MutableStateFlow(false)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var resendJob: Job? = null

    fun setCountryCode(code: String) {
        _countryCode.value = code
        validateCurrent()
    }

    fun setRawPhone(e164: String, valid: Boolean) {
        // e164 may be empty when invalid
        _phoneNumber.value = e164
        _isValid.value = valid
        if (!valid) _errorMessage.value = null
    }

    private fun validateCurrent() {
        _isValid.update { it }
    }

    /**
     * Attempt to send OTP for the current phone number.
     * Returns true when request initiated (success/failure will update errorMessage/state flows)
     */
    fun sendOtp(onStarted: () -> Unit = {}, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val phone = _phoneNumber.value
        if (phone.isBlank()) {
            _errorMessage.value = "Enter a valid phone number"
            onResult(false, _errorMessage.value)
            return
        }

        viewModelScope.launch {
            onStarted()
            val result = repository.sendPhoneOtp(phone = phone)
            result.onSuccess {
                _errorMessage.value = null
                onResult(true, null)
            }.onFailure {
                // Map common messages
                val msg = it.message ?: "Failed to send OTP"
                _errorMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EnterPhoneViewModel::class.java)) {
                return EnterPhoneViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
