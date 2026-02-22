package com.faster.festival.ui.auth.forgot

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

sealed interface ForgotUiState {
    object Idle : ForgotUiState
    object Loading : ForgotUiState
    object Sent : ForgotUiState
    data class Error(val message: String) : ForgotUiState
}

data class ForgotFormState(
    val email: String = "",
    val emailError: String? = null,
    val isSubmitEnabled: Boolean = false,
    val resendSecondsLeft: Int = 0
)

class ForgotPasswordViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<ForgotUiState>(ForgotUiState.Idle)
    val uiState: StateFlow<ForgotUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(ForgotFormState())
    val formState: StateFlow<ForgotFormState> = _formState.asStateFlow()

    private var timerJob: Job? = null

    fun onEmailChange(email: String) {
        val error = if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) null else "Invalid email format"
        _formState.update { it.copy(email = email, emailError = error, isSubmitEnabled = error == null && email.isNotBlank()) }
    }

    fun sendResetEmail(redirectTo: String? = null) {
        if (_uiState.value is ForgotUiState.Loading) return
        val email = _formState.value.email
        if (email.isBlank() || _formState.value.emailError != null) return

        viewModelScope.launch {
            _uiState.value = ForgotUiState.Loading
            val result = repository.requestPasswordReset(email = email, redirectTo = redirectTo)
            result.onSuccess {
                _uiState.value = ForgotUiState.Sent
                startResendTimer()
            }.onFailure {
                _uiState.value = ForgotUiState.Error(it.message ?: "Failed to send reset email")
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
        _uiState.value = ForgotUiState.Idle
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
                return ForgotPasswordViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
