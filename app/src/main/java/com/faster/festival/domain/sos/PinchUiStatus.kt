package com.faster.festival.domain.sos

/**
 * Backend-driven `ui_status` from `pinch-ingest` / `pinch-alert-status` /
 * `pinch-cancel`. This — NOT the raw `user_status` enum ([SosUserStatus]) — is
 * what the new Pinch SOS UI renders from, per the Pinch SOS Frontend Endpoint
 * contract.
 *
 * Adding a value here without backend support will silently fall to [Unknown].
 */
enum class PinchUiStatus(val raw: String) {
    /** Alert accepted by backend; awaiting dispatch review. */
    AlertReceived("ALERT_RECEIVED"),

    /** Dispatch is reviewing / assigning a responder. */
    DispatchReviewing("DISPATCH_REVIEWING"),

    /** Responder is en route — active tracking. */
    HelpOnTheWay("HELP_ON_THE_WAY"),

    /** Responder reached the scene. */
    HelpArrived("HELP_ARRIVED"),

    /** Emergency resolved/closed (terminal). */
    Completed("COMPLETED"),

    /** User cancel was POSTed; backend hasn't confirmed yet. */
    CancelRequested("CANCEL_REQUESTED"),

    /** Dispatch refused the cancel — the SOS stays active. */
    CancelDenied("CANCEL_DENIED"),

    /** Cancel confirmed (terminal). */
    Cancelled("CANCELLED"),

    /** False alarm / invalid trigger / denied (terminal). */
    Rejected("REJECTED"),

    /** Unmapped/absent ui_status — UI shows a neutral "updating" state. */
    Unknown("UNKNOWN");

    /** Polling + the live screen stop here. */
    val isTerminal: Boolean
        get() = this == Completed || this == Cancelled || this == Rejected

    /**
     * Three-step progress stage the live screen renders:
     * 0 = SWIPED, 1 = ARRIVED, 2 = RESOLVED. Cancel/terminal-failure states
     * keep the indicator at SWIPED so the timeline never shows false progress.
     */
    val stage: Int
        get() = when (this) {
            AlertReceived, DispatchReviewing, HelpOnTheWay,
            CancelRequested, CancelDenied -> 0
            HelpArrived -> 1
            Completed -> 2
            Cancelled, Rejected, Unknown -> 0
        }

    /** True while a cancel request is pending confirmation. */
    val isCancelPending: Boolean get() = this == CancelRequested

    /** Default headline; the live screen prefers the backend `responder.message` / `eta.label` when present. */
    val headline: String
        get() = when (this) {
            AlertReceived -> "Alert received"
            DispatchReviewing -> "Dispatch reviewing your alert"
            HelpOnTheWay -> "Help is On the Way"
            HelpArrived -> "Help has arrived"
            Completed -> "Emergency resolved"
            CancelRequested -> "Cancelling…"
            CancelDenied -> "Cancel not approved"
            Cancelled -> "Help alert cancelled"
            Rejected -> "Alert closed"
            Unknown -> "Updating…"
        }

    companion object {
        fun fromRaw(raw: String?): PinchUiStatus =
            entries.firstOrNull { it.raw == raw } ?: Unknown
    }
}
