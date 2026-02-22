package com.faster.festival.ui.auth.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI state for OTP screen
data class OtpUiState(
    val email: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    val resendCooldown: Int = 0,
    val error: String? = null
)

sealed interface VerificationEvent {
    object ShowSuccessDialog : VerificationEvent
    data class ShowToast(val message: String) : VerificationEvent
    object ResendStarted : VerificationEvent
}

class OtpViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // Expose a single source of truth UI state
    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    // For backward compatibility with other code, also expose smaller flows
    private val _events = MutableSharedFlow<VerificationEvent>()
    val events = _events.asSharedFlow()

    private var cooldownJob: Job? = null

    private fun updateState(update: OtpUiState.() -> OtpUiState) {
        _uiState.value = _uiState.value.update()
    }

    fun setCode(code: String) {
        if (code.length <= 6) updateState { copy(otp = code, error = null) }
    }

    // Allow setting/storing the email so resend can reuse it
    fun setEmail(email: String) {
        updateState { copy(email = email) }
    }

    fun startTimer(seconds: Int = 30) {
        cooldownJob?.cancel()
        updateState { copy(resendCooldown = seconds, isResending = false) }
        cooldownJob = viewModelScope.launch {
            while (_uiState.value.resendCooldown > 0) {
                delay(1000)
                updateState { copy(resendCooldown = maxOf(0, resendCooldown - 1)) }
            }
        }
    }

    /**
     * Send OTP via backend; if showFeedback is false this will be silent (no toast or visible error).
     * successMessage controls the toast text when showFeedback is true. Defaults keep previous behavior.
     */
    fun sendOtp(email: String, successMessage: String? = "OTP sent", showFeedback: Boolean = true) {
        viewModelScope.launch {
            val current = uiState.value
            if (current.isResending || current.resendCooldown > 0) return@launch
            // Keep isResending state so UI can disable controls; don't set visible error here unless showFeedback
            updateState { copy(isResending = true, error = if (showFeedback) null else error) }
            if (showFeedback) _events.emit(VerificationEvent.ResendStarted)
            try {
                val result = authRepository.sendOtp(email)
                if (result.isSuccess) {
                    // reset otp and start a 30s cooldown
                    updateState { copy(otp = "", error = null) }
                    startTimer(30)
                    // Emit a success toast only when feedback is enabled
                    if (showFeedback && !successMessage.isNullOrBlank()) _events.emit(VerificationEvent.ShowToast(successMessage))
                } else {
                    val msg = mapErrorToFriendlyMessage(result.exceptionOrNull()?.message)
                    if (showFeedback) {
                        updateState { copy(error = msg) }
                        _events.emit(VerificationEvent.ShowToast(msg))
                    } else {
                        // Silent failure: don't surface to UI; could log instead
                    }
                }
            } catch (e: Exception) {
                val msg = "Network error. Please check your connection."
                if (showFeedback) {
                    updateState { copy(error = msg) }
                    _events.emit(VerificationEvent.ShowToast(msg))
                }
            } finally {
                updateState { copy(isResending = false) }
            }
        }
    }

    fun resendOtp(email: String) {
        if (_uiState.value.resendCooldown > 0) return
        // Use a different success message for explicit resends
        sendOtp(email, successMessage = "OTP sent again", showFeedback = true)
    }

    /**
     * New: Resend by calling the Supabase /auth/v1/signup endpoint again, reusing the same
     * SignupRequest shape. This matches your desired server-side flow where calling signup
     * again will resend the verification code to the same email.
     */
    fun resendUsingSignup(fullName: String? = null) {
        val email = _uiState.value.email
        if (email.isBlank() || _uiState.value.resendCooldown > 0 || _uiState.value.isResending) return

        viewModelScope.launch {
            updateState { copy(isResending = true, error = null) }
            _events.emit(VerificationEvent.ResendStarted)

            try {
                val result = authRepository.resendSignup(email = email, fullName = fullName)
                if (result.isSuccess) {
                    // Reset OTP input and start cooldown
                    updateState { copy(otp = "", error = null) }
                    startTimer(30)
                    _events.emit(VerificationEvent.ShowToast("Verification email resent"))
                } else {
                    val raw = result.exceptionOrNull()?.message
                    val friendly = mapErrorToFriendlyMessage(raw)
                    updateState { copy(error = friendly) }
                    _events.emit(VerificationEvent.ShowToast(friendly))
                }
            } catch (e: Exception) {
                val msg = "Network error. Please check your connection."
                updateState { copy(error = msg) }
                _events.emit(VerificationEvent.ShowToast(msg))
            } finally {
                updateState { copy(isResending = false) }
            }
        }
    }

    fun verifyOtp(email: String) {
        val code = _uiState.value.otp
        if (code.length != 6) {
            val msg = "Enter a 6-digit code"
            updateState { copy(error = msg) }
            viewModelScope.launch { _events.emit(VerificationEvent.ShowToast(msg)) }
            return
        }

        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            updateState { copy(isLoading = true, error = null) }

            try {
                val result = authRepository.verifyOtp(email, code)
                if (result.isSuccess) {
                    val authResponse = result.getOrNull()
                    if (authResponse == null) {
                        val msg = "Empty verification response"
                        updateState { copy(error = msg, isLoading = false) }
                        _events.emit(VerificationEvent.ShowToast(msg))
                    } else {
                        val access = authResponse.accessToken
                        val refresh = authResponse.refreshToken
                        val user = authResponse.user

                        if (access.isNullOrEmpty() || refresh.isNullOrEmpty() || user == null) {
                            val msg = "Invalid verification response"
                            updateState { copy(error = msg, isLoading = false) }
                            _events.emit(VerificationEvent.ShowToast(msg))
                        } else {
                            val emailVerifiedFlag = user.userMetadata?.get("email_verified")
                            val isEmailVerified = emailVerifiedFlag?.toBoolean() ?: false

                            if (isEmailVerified) {
                                // persist session
                                try {
                                    authRepository.persistSession(authResponse)
                                } catch (e: Exception) {
                                    // non-fatal
                                    _events.emit(VerificationEvent.ShowToast("Failed to persist session"))
                                }

                                updateState { copy(isLoading = false, error = null) }
                                _events.emit(VerificationEvent.ShowSuccessDialog)
                            } else {
                                val msg = "Email not verified yet. Please check your email."
                                updateState { copy(error = msg, isLoading = false) }
                                _events.emit(VerificationEvent.ShowToast(msg))
                            }
                        }
                    }
                } else {
                    val raw = result.exceptionOrNull()?.message
                    val friendly = mapErrorToFriendlyMessage(raw)
                    // If expired, clear the input to encourage resend
                    if (friendly.contains("expired", ignoreCase = true)) {
                        updateState { copy(otp = "", error = friendly, isLoading = false) }
                    } else {
                        updateState { copy(error = friendly, isLoading = false) }
                    }
                    _events.emit(VerificationEvent.ShowToast(friendly))
                }
            } catch (e: Exception) {
                val msg = "Network error. Please check your connection."
                updateState { copy(error = msg, isLoading = false) }
                _events.emit(VerificationEvent.ShowToast(msg))
            }
        }
    }

    private fun mapErrorToFriendlyMessage(raw: String?): String {
        if (raw.isNullOrEmpty()) return "Verification failed. Please try again."
        val lower = raw.lowercase()
        return when {
            "otp_expired" in lower || "expired" in lower || "token has expired" in lower -> "Code expired. Please request a new one."
            "invalid" in lower || "invalid code" in lower || "wrong" in lower -> "Invalid code. Try again."
            "rate" in lower || "limit" in lower || "too many requests" in lower -> "Too many requests. Try again later."
            "network" in lower || "timeout" in lower -> "Network error. Please check your connection."
            else -> raw
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OtpViewModel(authRepository) as T
        }
    }
}
