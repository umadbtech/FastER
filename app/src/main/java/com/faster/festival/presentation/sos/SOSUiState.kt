package com.faster.festival.presentation.sos

import com.faster.festival.domain.sos.SosUserStatus

/**
 * UI state for the SOS button + overlay. Branches collapse the multi-step
 * trusted-device + trigger pipeline into something the Compose layer can
 * render with a single `when`.
 */
sealed class SOSUiState {

    /** First paint — bootstrap not yet attempted. */
    object Idle : SOSUiState()

    /** Setup is running silently in the background. SOS button stays disabled. */
    object SetupInProgress : SOSUiState()

    /**
     * User has no active membership / not an attendee — SOS is unavailable.
     * The screen MUST show a clear message and route to onboarding.
     */
    object OnboardingRequired : SOSUiState()

    /**
     * Trusted device verified and ready — SOS button is enabled. No active
     * incident.
     */
    object Ready : SOSUiState()

    /** Setup hit a terminal failure — surface to the user with retry. */
    data class SetupFailed(val message: String) : SOSUiState()

    /** Active SOS — `pinch-ingest` round-trip in flight or polling. */
    data class Sending(val clientTriggerId: String) : SOSUiState()

    /** Polling has heard back — render progress + status text. */
    data class InProgress(
        val clientTriggerId: String,
        val status: SosUserStatus,
        val responderName: String?,
        val etaMinutes: Int?
    ) : SOSUiState()

    /** Terminal state from server. */
    data class Resolved(val status: SosUserStatus) : SOSUiState()

    /** `pinch-ingest` failed even after retry — surface to the user. */
    data class TriggerFailed(
        val clientTriggerId: String,
        val message: String
    ) : SOSUiState()
}
