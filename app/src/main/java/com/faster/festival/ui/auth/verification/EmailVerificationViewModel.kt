package com.faster.festival.ui.auth.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.model.VerificationPayload
import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.realtime.channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface VerificationUiState {
    data object Idle : VerificationUiState
    data object Listening : VerificationUiState
    data object Verified : VerificationUiState
    data class Error(val message: String) : VerificationUiState
}

/**
 * ViewModel that listens to Supabase Realtime email verification events.
 *
 * Subscribes to the "global_verifications" channel and listens for "verified" broadcast events.
 * When a verification event is received and the user_id matches the current user,
 * the state is updated to Verified.
 *
 * Channel cleanup is handled in onCleared() to prevent memory leaks.
 */
class EmailVerificationViewModel(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: EncryptedSessionManager
) : ViewModel() {

    private val _verificationState = MutableStateFlow<VerificationUiState>(VerificationUiState.Idle)
    val verificationState: StateFlow<VerificationUiState> = _verificationState.asStateFlow()

    private var channel: io.github.jan_tennert.supabase.realtime.RealtimeChannel? = null

    init {
        startListeningForVerification()
    }

    private fun startListeningForVerification() {
        viewModelScope.launch {
            try {
                _verificationState.value = VerificationUiState.Listening

                // Get current user ID
                val currentUserId = sessionManager.getUserID()
                if (currentUserId.isNullOrEmpty()) {
                    _verificationState.value = VerificationUiState.Error(
                        "No user ID found. Please sign up again."
                    )
                    return@launch
                }

                // Create and subscribe to the realtime channel
                channel = supabaseClient.channel("global_verifications") {
                    // Listen for "verified" broadcast events
                    onBroadcast("verified") { message ->
                        handleVerificationEvent(message, currentUserId)
                    }
                }

                // Subscribe to the channel
                channel?.subscribe()

            } catch (e: Exception) {
                _verificationState.value = VerificationUiState.Error(
                    "Connection failed: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    private fun handleVerificationEvent(
        message: io.github.jan_tennert.supabase.realtime.Message,
        currentUserId: String
    ) {
        viewModelScope.launch {
            try {
                // Extract payload from the message
                val payload = message.payload
                val jsonPayload = payload.jsonObject

                // Parse user_id, email, and timestamp from the payload
                val verifiedUserId = jsonPayload["user_id"]?.jsonPrimitive?.content
                val email = jsonPayload["email"]?.jsonPrimitive?.content
                val timestamp = jsonPayload["timestamp"]?.jsonPrimitive?.content

                if (verifiedUserId != null) {
                    // Compare received user_id with current user ID
                    if (verifiedUserId == currentUserId) {
                        // Email verified successfully for this user
                        sessionManager.setEmailConfirmed(true)
                        _verificationState.value = VerificationUiState.Verified
                    }
                    // If user_id doesn't match, ignore the event (not for this user)
                }
            } catch (e: Exception) {
                _verificationState.value = VerificationUiState.Error(
                    "Failed to process verification: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Unsubscribe from the channel and clean up resources.
     * Called automatically in onCleared() to prevent memory leaks.
     */
    private suspend fun unsubscribeFromChannel() {
        try {
            channel?.unsubscribe()
            channel = null
        } catch (e: Exception) {
            // Log error but don't propagate during cleanup
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up channel subscription when ViewModel is destroyed
        viewModelScope.launch {
            unsubscribeFromChannel()
        }
    }

    class Factory(
        private val supabaseClient: SupabaseClient,
        private val sessionManager: EncryptedSessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EmailVerificationViewModel::class.java)) {
                return EmailVerificationViewModel(supabaseClient, sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
