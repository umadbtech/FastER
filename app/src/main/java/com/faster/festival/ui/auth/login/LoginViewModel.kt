package com.faster.festival.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.AuthRepository
import com.faster.festival.data.repository.AuthRepositoryContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI state
sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

data class LoginFormState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isFormValid: Boolean = false
)

class LoginViewModel(private val authRepository: AuthRepositoryContract) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    init {
        // Pre-fill email if saved in session
        viewModelScope.launch {
            try {
                authRepository.getSavedEmail()?.let { saved ->
                    // Update form state with saved email and run validation
                    onEmailChange(saved)
                }
            } catch (_: Exception) {
                // ignore read errors; no-op
            }
        }
    }

    fun onEmailChange(email: String) {
        val error = if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) null else "Invalid email format"
        _formState.update { it.copy(email = email, emailError = error) }
        validateForm()
    }

    fun onPasswordChange(password: String) {
        val error = if (password.length >= 6) null else "Password must be at least 6 characters"
        _formState.update { it.copy(password = password, passwordError = error) }
        validateForm()
    }

    private fun validateForm() {
        _formState.update {
            it.copy(
                isFormValid =
                    it.email.isNotBlank() && it.emailError == null &&
                            it.password.isNotBlank() && it.passwordError == null
            )
        }
    }

    fun login(onSuccess: () -> Unit) {
        val state = _formState.value
        if (!state.isFormValid) return

        viewModelScope.launch {
            if (_uiState.value is LoginUiState.Loading) return@launch
            _uiState.value = LoginUiState.Loading
            val result = authRepository.login(email = state.email, password = state.password)
            result
                .onSuccess {
                    _uiState.value = LoginUiState.Success
                    onSuccess()
                }
                .onFailure {
                    _uiState.value = LoginUiState.Error(it.message ?: "Login failed")
                }
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(repository) as T
        }
    }
}
