package com.faster.festival.ui.auth.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.repository.AuthRepository
import com.faster.festival.data.repository.SmsSendFailedException
import com.faster.festival.data.repository.RateLimitException
import com.faster.festival.data.repository.PhoneValidationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ErrorSeverity { CRITICAL, WARNING, INFO }

enum class ErrorType { TOAST, SNACKBAR, DIALOG }

data class SmsErrorState(
    val message: String,
    val severity: ErrorSeverity = ErrorSeverity.INFO,
    val type: ErrorType = ErrorType.SNACKBAR,
    val action: String? = null,
    val canRetry: Boolean = false,
    val retrySeconds: Int = 0,
    val showEmailFallback: Boolean = false
)

class SmsVerificationViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _otp = MutableStateFlow("")
    val otp: StateFlow<String> = _otp.asStateFlow()

    private val _isOtpComplete = MutableStateFlow(false)
    val isOtpComplete: StateFlow<Boolean> = _isOtpComplete.asStateFlow()

    private val _errorState = MutableStateFlow<SmsErrorState?>(null)
    val errorState: StateFlow<SmsErrorState?> = _errorState.asStateFlow()

    private val _resendSeconds = MutableStateFlow(60)
    val resendSeconds: StateFlow<Int> = _resendSeconds.asStateFlow()

    private var countdownJob: Job? = null
    private var retryAttempts = 0
    private val maxAttempts = 2 // total attempts including initial (so 1 retry)

    fun setOtp(value: String) {
        _otp.value = value.filter { it.isDigit() }.take(6)
        _isOtpComplete.value = _otp.value.length == 6
    }

    fun clearError() {
        _errorState.value = null
    }

    private fun setErrorStateForException(e: Throwable) {
        when (e) {
            is SmsSendFailedException -> {
                // critical
                val canFallback = retryAttempts >= (maxAttempts - 1)
                val initialRetrySeconds = if (!canFallback) 3 else 0
                _errorState.value = SmsErrorState(
                    message = e.message ?: "SMS failed to send. Check signal or try email.",
                    severity = ErrorSeverity.CRITICAL,
                    type = ErrorType.SNACKBAR,
                    action = if (!canFallback) "Retry" else "Email Signup",
                    canRetry = !canFallback,
                    retrySeconds = initialRetrySeconds,
                    showEmailFallback = canFallback
                )

                // If we have a retrySeconds > 0, start a transient countdown on the error state so UI can render a live timer
                if (initialRetrySeconds > 0) {
                    viewModelScope.launch {
                        var rem = initialRetrySeconds
                        while (rem > 0) {
                            delay(1000)
                            rem -= 1
                            val current = _errorState.value
                            if (current != null) _errorState.value = current.copy(retrySeconds = rem)
                        }
                    }
                }
            }
            is RateLimitException -> {
                _errorState.value = SmsErrorState(
                    message = e.message ?: "Too many attempts. Wait 60s.",
                    severity = ErrorSeverity.WARNING,
                    type = ErrorType.SNACKBAR,
                    action = "Retry",
                    canRetry = false,
                    retrySeconds = e.retryAfterSeconds ?: 60,
                    showEmailFallback = false
                )
            }
            is PhoneValidationException -> {
                _errorState.value = SmsErrorState(
                    message = e.message ?: "Invalid number format.",
                    severity = ErrorSeverity.INFO,
                    type = ErrorType.SNACKBAR,
                    action = "Edit",
                    canRetry = false,
                    retrySeconds = 0,
                    showEmailFallback = false
                )
            }
            else -> {
                _errorState.value = SmsErrorState(
                    message = e.message ?: "Verification unavailable. Try email signup.",
                    severity = ErrorSeverity.INFO,
                    type = ErrorType.SNACKBAR,
                    action = "Email Signup",
                    canRetry = false,
                    retrySeconds = 0,
                    showEmailFallback = true
                )
            }
        }
    }

    suspend fun verifyOtp(phone: String, token: String): Result<Unit> {
        _errorState.value = null
        try {
            val result = repository.verifyPhoneOtp(phone = phone, token = token)
            return if (result.isSuccess) {
                _errorState.value = null
                Result.success(Unit)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Verification failed"
                _errorState.value = SmsErrorState(message = msg, severity = ErrorSeverity.INFO, type = ErrorType.SNACKBAR)
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            setErrorStateForException(e)
            return Result.failure(e)
        }
    }

    fun startCountdown(seconds: Int = 60) {
        countdownJob?.cancel()
        _resendSeconds.value = seconds
        countdownJob = viewModelScope.launch {
            var rem = seconds
            while (rem > 0) {
                delay(1000)
                rem -= 1
                _resendSeconds.value = rem
            }
        }
    }

    fun startResendWithRetry(phone: String, createUser: Boolean = true) {
        // Reset flags
        retryAttempts = 0
        sendWithAutoRetry(phone, createUser)
    }

    private fun sendWithAutoRetry(phone: String, createUser: Boolean) {
        viewModelScope.launch {
            retryAttempts = 0
            while (retryAttempts < maxAttempts) {
                val res = repository.sendPhoneOtpWithErrorMapping(phone = phone, createUser = createUser)
                if (res.isSuccess) {
                    // success -> start countdown and clear errors
                    startCountdown()
                    _errorState.value = null
                    return@launch
                } else {
                    val ex = res.exceptionOrNull()
                    retryAttempts += 1
                    if (ex is SmsSendFailedException) {
                        setErrorStateForException(ex)
                        if (retryAttempts < maxAttempts) {
                            // wait specified retrySeconds on the error state (already set) then retry
                            val wait = _errorState.value?.retrySeconds ?: 3
                            delay((wait * 1000).toLong())
                            continue
                        } else {
                            // after max attempts, show fallback (error state already set)
                            return@launch
                        }
                    } else if (ex is RateLimitException) {
                        setErrorStateForException(ex)
                        // start cooldown according to retrySeconds
                        val wait = ex.retryAfterSeconds ?: 60
                        startCountdown(wait)
                        return@launch
                    } else {
                        setErrorStateForException(ex ?: Exception("Unknown error"))
                        return@launch
                    }
                }
            }
        }
    }

    fun resendOtp(phone: String) {
        // allow user-triggered resend — use mapping function and show errors
        viewModelScope.launch {
            val res = repository.sendPhoneOtpWithErrorMapping(phone = phone)
            if (res.isSuccess) {
                startCountdown()
                _errorState.value = null
            } else {
                setErrorStateForException(res.exceptionOrNull() ?: Exception("Failed to resend"))
            }
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SmsVerificationViewModel::class.java)) {
                return SmsVerificationViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
