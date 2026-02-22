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

sealed interface PhoneLoginUiState {
    object Idle : PhoneLoginUiState
    object Loading : PhoneLoginUiState
    data class Sent(val phone: String) : PhoneLoginUiState
    data class Error(val message: String) : PhoneLoginUiState
}

data class PhoneLoginFormState(
    val phone: String = "",
    val phoneError: String? = null,
    val isSubmitEnabled: Boolean = false,
    val resendSecondsLeft: Int = 0
)

class PhoneLoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<PhoneLoginUiState>(PhoneLoginUiState.Idle)
    val uiState: StateFlow<PhoneLoginUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(PhoneLoginFormState())
    val formState: StateFlow<PhoneLoginFormState> = _formState.asStateFlow()

    private var timerJob: Job? = null

    fun onPhoneChange(phone: String) {
        val normalized = phone.trim()
        val error = if (normalized.startsWith("+") && normalized.length >= 8) null else "Invalid phone format"
        _formState.update { it.copy(phone = normalized, phoneError = error, isSubmitEnabled = error == null && normalized.isNotBlank()) }
    }

    fun sendOtp(createUser: Boolean = true) {
        if (_uiState.value is PhoneLoginUiState.Loading) return
        val phone = _formState.value.phone
        if (phone.isBlank() || _formState.value.phoneError != null) return

        viewModelScope.launch {
            _uiState.value = PhoneLoginUiState.Loading
            val result = repository.sendPhoneOtpWithErrorMapping(phone = phone, createUser = createUser)
            result.onSuccess {
                _uiState.value = PhoneLoginUiState.Sent(phone)
                startResendTimer()
            }.onFailure { ex ->
                // Map known exceptions to the exact toast/snackbar messages requested
                val msg = when (ex) {
                    is com.faster.festival.data.repository.SmsSendFailedException -> "SMS failed to send. Check signal or try email."
                    is com.faster.festival.data.repository.RateLimitException -> "Too many attempts. Wait 60s."
                    is com.faster.festival.data.repository.PhoneValidationException -> "Invalid number format."
                    else -> ex.message ?: "Failed to send OTP"
                }
                _uiState.value = PhoneLoginUiState.Error(msg)
            }
        }
    }

    private fun startResendTimer(seconds: Int = 60) {
        timerJob?.cancel()
        _formState.update { it.copy(resendSecondsLeft = seconds, isSubmitEnabled = false) }
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _formState.update { it.copy(resendSecondsLeft = remaining) }
            }
            _formState.update { it.copy(isSubmitEnabled = true) }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _formState.update { it.copy(resendSecondsLeft = 0, isSubmitEnabled = true) }
    }

    fun resetState() {
        _uiState.value = PhoneLoginUiState.Idle
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PhoneLoginViewModel::class.java)) {
                return PhoneLoginViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
