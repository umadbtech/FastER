package com.faster.festival.ui.auth.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.model.AuthResponse
import com.faster.festival.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ResetUiState {
    object Idle : ResetUiState
    object Loading : ResetUiState
    object Success : ResetUiState
    data class Error(val message: String) : ResetUiState
}

data class ResetFormState(
    val otp: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val otpError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isFormValid: Boolean = false
)

class ResetPasswordViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<ResetUiState>(ResetUiState.Idle)
    val uiState: StateFlow<ResetUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(ResetFormState())
    val formState: StateFlow<ResetFormState> = _formState.asStateFlow()

    fun onOtpChange(otp: String) {
        val cleaned = otp.filter { it.isDigit() }.take(6)
        _formState.update { it.copy(otp = cleaned, otpError = if (cleaned.length == 6) null else "Enter 6-digit code") }
        validate()
    }

    fun onPasswordChange(password: String) {
        val error = if (password.length >= 6) null else "Password must be at least 6 characters"
        _formState.update { it.copy(password = password, passwordError = error) }
        validate()
    }

    fun onConfirmPasswordChange(confirm: String) {
        val error = if (confirm == _formState.value.password) null else "Passwords do not match"
        _formState.update { it.copy(confirmPassword = confirm, confirmPasswordError = error) }
        validate()
    }

    private fun validate() {
        val s = _formState.value
        val valid = s.otp.length == 6 && s.password.length >= 6 && s.password == s.confirmPassword
        _formState.update { it.copy(isFormValid = valid) }
    }

    suspend fun verifyOtpAndSetPassword(email: String, token: String) {
        if (_uiState.value is ResetUiState.Loading) return
        _uiState.value = ResetUiState.Loading
        // verify token first
        val otp = _formState.value.otp
        val password = _formState.value.password
        try {
            val verifyResult = repository.verifyRecoveryOtp(email = email, token = otp.ifBlank { token })
            verifyResult.onSuccess { authResp: AuthResponse ->
                // If verify returns tokens, use its access token to update password
                val access = authResp.accessToken ?: ""
                val updateResult = repository.updatePassword(accessToken = access, newPassword = password)
                updateResult.onSuccess {
                    _uiState.value = ResetUiState.Success
                }.onFailure {
                    _uiState.value = ResetUiState.Error(it.message ?: "Failed to update password")
                }
            }.onFailure {
                _uiState.value = ResetUiState.Error(it.message ?: "OTP verification failed")
            }
        } catch (e: Exception) {
            _uiState.value = ResetUiState.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    fun resetState() {
        _uiState.value = ResetUiState.Idle
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ResetPasswordViewModel::class.java)) {
                return ResetPasswordViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
