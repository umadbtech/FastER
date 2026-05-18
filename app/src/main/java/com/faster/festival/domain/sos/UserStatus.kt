package com.faster.festival.domain.sos

/**
 * Server-side `user_status` enum from `pinch-alert-status`. Mirrors backend
 * exactly — adding a new value here without backend support will silently fall
 * to [Unknown].
 */
enum class SosUserStatus(val raw: String) {
    SosReceived("SOS_RECEIVED"),
    DispatchReceived("DISPATCH_RECEIVED"),
    DispatchConfirmed("DISPATCH_CONFIRMED"),
    ResponderAssigned("RESPONDER_ASSIGNED"),
    ResponderAccepted("RESPONDER_ACCEPTED"),
    ResponderEnRoute("RESPONDER_EN_ROUTE"),
    ResponderOnScene("RESPONDER_ON_SCENE"),
    Resolved("RESOLVED"),
    Closed("CLOSED"),
    Rejected("REJECTED"),
    CancelledByUser("CANCELLED_BY_USER"),
    Unknown("UNKNOWN");

    /** Polling stops on terminal states. */
    val isTerminal: Boolean
        get() = this == Resolved || this == Closed ||
                this == Rejected || this == CancelledByUser

    val displayText: String
        get() = when (this) {
            SosReceived -> "SOS received. Contacting dispatch…"
            DispatchReceived -> "Dispatch received your SOS."
            DispatchConfirmed -> "Dispatch confirmed your SOS."
            ResponderAssigned -> "A responder has been assigned."
            ResponderAccepted -> "Responder accepted the assignment."
            ResponderEnRoute -> "Responder is on the way."
            ResponderOnScene -> "Responder is on scene."
            Resolved -> "SOS resolved."
            Closed -> "SOS closed."
            Rejected -> "SOS was rejected."
            CancelledByUser -> "SOS was cancelled."
            Unknown -> "Updating…"
        }

    /** 0..7 — drives a progress bar in the SOS UI. */
    val progressIndex: Int
        get() = when (this) {
            SosReceived -> 0
            DispatchReceived -> 1
            DispatchConfirmed -> 2
            ResponderAssigned -> 3
            ResponderAccepted -> 4
            ResponderEnRoute -> 5
            ResponderOnScene -> 6
            Resolved, Closed -> 7
            Rejected, CancelledByUser, Unknown -> 0
        }

    companion object {
        fun fromRaw(raw: String?): SosUserStatus =
            values().firstOrNull { it.raw == raw } ?: Unknown
    }
}
