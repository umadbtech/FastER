package com.faster.festival.ui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.AuthRepository
import com.faster.festival.utils.PasswordValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SignupUiState {
    data object Idle : SignupUiState
    data object Loading : SignupUiState
    data class Success(val email: String) : SignupUiState // Pass email for verification screen
    data class Error(val message: String) : SignupUiState
}

data class SignupFormState(
        val fullName: String = "",
        val fullNameError: String? = null,
        val email: String = "",
        val emailError: String? = null,
        val password: String = "",
        val passwordError: String? = null,
        val confirmPassword: String = "",
        val confirmPasswordError: String? = null,
        val isFormValid: Boolean = false
)

class SignupViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(SignupFormState())
    val formState: StateFlow<SignupFormState> = _formState.asStateFlow()

    fun onFullNameChange(name: String) {
        val error = if (name.isNotBlank()) null else "Full Name is required"
        _formState.update { it.copy(fullName = name, fullNameError = error) }
        validateForm()
    }

    fun onEmailChange(email: String) {
        val error =
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) null
                else "Invalid email format"
        _formState.update { it.copy(email = email, emailError = error) }
        validateForm()
    }

    fun onPasswordChange(password: String) {
        val error = PasswordValidator.firstError(password)
        // Re-check confirm password match whenever password changes
        val confirm = _formState.value.confirmPassword
        val confirmError = if (confirm.isEmpty()) {
            _formState.value.confirmPasswordError
        } else {
            PasswordValidator.confirmError(password, confirm)
        }
        _formState.update {
            it.copy(
                password = password,
                passwordError = error,
                confirmPasswordError = confirmError
            )
        }
        validateForm()
    }

    fun onConfirmPasswordChange(confirm: String) {
        val error = PasswordValidator.confirmError(_formState.value.password, confirm)
        _formState.update { it.copy(confirmPassword = confirm, confirmPasswordError = error) }
        validateForm()
    }

    private fun validateForm() {
        _formState.update {
            it.copy(
                    isFormValid =
                            it.fullName.isNotBlank() &&
                                    it.fullNameError == null &&
                                    it.email.isNotBlank() &&
                                    it.emailError == null &&
                                    it.password.isNotBlank() &&
                                    it.passwordError == null &&
                                    it.confirmPassword.isNotBlank() &&
                                    it.confirmPasswordError == null
            )
        }
    }

    fun onSignupClick() {
        if (!_formState.value.isFormValid) return
        if (_uiState.value is SignupUiState.Loading) return // Prevent rapid clicks

        viewModelScope.launch {
            _uiState.value = SignupUiState.Loading
            val result =
                    authRepository.signUp(
                            fullName = _formState.value.fullName,
                            email = _formState.value.email,
                            password = _formState.value.password
                    )
            result
                    .onSuccess {
                        // Assuming successful signup sends a verification email, rely on input
                        // email
                        _uiState.value = SignupUiState.Success(_formState.value.email)
                    }
                    .onFailure {
                        _uiState.value = SignupUiState.Error(it.message ?: "Signup failed")
                    }
        }
    }

    fun resetState() {
        _uiState.value = SignupUiState.Idle
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SignupViewModel::class.java)) {
                return SignupViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
