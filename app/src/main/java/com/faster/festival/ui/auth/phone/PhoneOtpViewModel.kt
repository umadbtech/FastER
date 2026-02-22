package com.faster.festival.ui.auth.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.model.AuthResponse
import com.faster.festival.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PhoneOtpUiState {
    object Idle : PhoneOtpUiState
    object Loading : PhoneOtpUiState
    object Verified : PhoneOtpUiState
    data class Error(val message: String) : PhoneOtpUiState
}

data class PhoneOtpFormState(
    val phone: String = "",
    val otp: String = "",
    val otpError: String? = null,
    val password: String? = null,
    val isFormValid: Boolean = false,
    val resendSecondsLeft: Int = 0
)

class PhoneOtpViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<PhoneOtpUiState>(PhoneOtpUiState.Idle)
    val uiState: StateFlow<PhoneOtpUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(PhoneOtpFormState())
    val formState: StateFlow<PhoneOtpFormState> = _formState.asStateFlow()

    private var timerJob: Job? = null

    fun onOtpChange(input: String) {
        val cleaned = input.filter { it.isDigit() }.take(6)
        val err = if (cleaned.length == 6) null else "Enter 6-digit code"
        _formState.update { it.copy(otp = cleaned, otpError = err) }
        validate()
    }

    fun setPhone(phone: String) {
        _formState.update { it.copy(phone = phone) }
    }

    private fun validate() {
        val s = _formState.value
        _formState.update { it.copy(isFormValid = s.otp.length == 6) }
    }

    suspend fun verifyOtp(phone: String, token: String) {
        if (_uiState.value is PhoneOtpUiState.Loading) return
        _uiState.value = PhoneOtpUiState.Loading
        try {
            val result = repository.verifyPhoneOtp(phone = phone, token = token)
            result.onSuccess { auth: AuthResponse ->
                // Persisted by repository already
                _uiState.value = PhoneOtpUiState.Verified
            }.onFailure {
                _uiState.value = PhoneOtpUiState.Error(it.message ?: "OTP verification failed")
            }
        } catch (e: Exception) {
            _uiState.value = PhoneOtpUiState.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    /**
     * Resend OTP to phone. Launches coroutine and starts resend timer on success.
     */
    fun resendOtp(phone: String, createUser: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = PhoneOtpUiState.Loading
            val result = repository.sendPhoneOtp(phone = phone, createUser = createUser)
            result.onSuccess {
                startResendTimer()
                _uiState.value = PhoneOtpUiState.Idle
            }.onFailure {
                _uiState.value = PhoneOtpUiState.Error(it.message ?: "Failed to resend OTP")
            }
        }
    }

    fun startResendTimer(seconds: Int = 60) {
        timerJob?.cancel()
        _formState.update { it.copy(resendSecondsLeft = seconds) }
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _formState.update { it.copy(resendSecondsLeft = remaining) }
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _formState.update { it.copy(resendSecondsLeft = 0) }
    }

    fun resetState() {
        _uiState.value = PhoneOtpUiState.Idle
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PhoneOtpViewModel::class.java)) {
                return PhoneOtpViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
