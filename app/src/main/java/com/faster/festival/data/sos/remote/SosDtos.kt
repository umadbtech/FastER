package com.faster.festival.data.sos.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Device registration ───────────────────────────────────────────────────

@Serializable
data class RegisterDeviceRequest(
    val platform: String = "android",
    @SerialName("app_id") val appId: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("device_public_key") val devicePublicKey: String,
    @SerialName("key_algorithm") val keyAlgorithm: String = "ed25519"
)

@Serializable
data class RegisterDeviceResponse(
    val device: DeviceRow
)

@Serializable
data class DeviceRow(
    @SerialName("device_id") val deviceId: String,
    val status: String? = null
)

// ─── Attestation ───────────────────────────────────────────────────────────

@Serializable
data class VerifyAttestationRequest(
    @SerialName("device_id") val deviceId: String,
    val platform: String = "android",
    @SerialName("app_id") val appId: String,
    val provider: String,
    @SerialName("attestation_token") val attestationToken: String,
    @SerialName("device_public_key") val devicePublicKey: String,
    @SerialName("key_algorithm") val keyAlgorithm: String = "ed25519"
)

@Serializable
data class VerifyAttestationResponse(
    val ok: Boolean? = null,
    val device: DeviceRow? = null,
    val status: String? = null
)

// ─── pinch-ingest ──────────────────────────────────────────────────────────

@Serializable
data class SosTriggerRequest(
    @SerialName("client_trigger_id") val clientTriggerId: String,
    @SerialName("festival_id") val festivalId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("trigger_source") val triggerSource: String = "mobile_ui",
    val nonce: String,
    val timestamp: String,
    val location: SosLocation,
    val wristband: WristbandInfo,
    @SerialName("device_context") val deviceContext: DeviceContext
)

@Serializable
data class SosLocation(
    val latitude: Double,
    val longitude: Double,
    @SerialName("accuracy_meters") val accuracyMeters: Int? = null
)

@Serializable
data class WristbandInfo(
    @SerialName("wristband_id") val wristbandId: String? = null,
    @SerialName("connection_state") val connectionState: String,
    @SerialName("battery_percent") val batteryPercent: Int? = null
)

@Serializable
data class DeviceContext(
    val platform: String = "android",
    @SerialName("app_version") val appVersion: String,
    @SerialName("sent_at") val sentAt: String
)

/**
 * `pinch-ingest` 2xx response. New contract is NESTED:
 *
 * ```
 * { "ok": true, "deduplicated": false,
 *   "alert": { "id", "client_trigger_id", "status", "user_status",
 *              "ui_status", "aws_incident_id", "aws_status", "received_at" },
 *   "tracking_session": { "id", "status", "expires_at" },
 *   "aws_outbox": { "id", "status" } }
 * ```
 *
 * Back-compat convenience getters ([alertId] / [clientTriggerId] / [status])
 * pass through to [alert] so existing call sites keep working, and
 * [trackingSessionId] / [uiStatus] expose the new fields the live screen needs.
 */
@Serializable
data class SosIngestResponse(
    val ok: Boolean? = null,
    val deduplicated: Boolean? = null,
    val alert: IngestAlert? = null,
    @SerialName("tracking_session") val trackingSession: TrackingSessionInfo? = null
) {
    val alertId: String? get() = alert?.id
    val clientTriggerId: String? get() = alert?.clientTriggerId
    val status: String? get() = alert?.userStatus
    val uiStatus: String? get() = alert?.uiStatus
    val trackingSessionId: String? get() = trackingSession?.id
}

@Serializable
data class IngestAlert(
    val id: String? = null,
    @SerialName("client_trigger_id") val clientTriggerId: String? = null,
    val status: String? = null,
    @SerialName("user_status") val userStatus: String? = null,
    @SerialName("ui_status") val uiStatus: String? = null,
    @SerialName("aws_incident_id") val awsIncidentId: String? = null,
    @SerialName("aws_status") val awsStatus: String? = null,
    @SerialName("received_at") val receivedAt: String? = null
)

@Serializable
data class TrackingSessionInfo(
    val id: String? = null,
    val status: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
)

// ─── pinch-update-location ─────────────────────────────────────────────────

/**
 * Body for `pinch-update-location` — periodic GPS push while an SOS is in
 * flight. Same Ed25519 signing envelope as `pinch-ingest` (the canonical
 * string just uses `/pinch-update-location` as the path).
 *
 * `tracking_session_id` is mandatory — handed to us by `pinch-ingest`
 * (`tracking_session.id`). We don't fire updates until that value is non-null.
 * Per the new contract the body carries NO `client_trigger_id` / `device_context`.
 */
@Serializable
data class LocationUpdateRequest(
    @SerialName("tracking_session_id") val trackingSessionId: String,
    @SerialName("device_id") val deviceId: String,
    val nonce: String,
    val timestamp: String,
    val location: SosLocation
)

@Serializable
data class LocationUpdateResponse(
    val ok: Boolean? = null,
    @SerialName("tracking_session_id") val trackingSessionId: String? = null,
    @SerialName("captured_at") val capturedAt: String? = null,
    @SerialName("latest_location") val latestLocation: LatestLocation? = null
)

@Serializable
data class LatestLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("accuracy_meters") val accuracyMeters: Int? = null,
    @SerialName("captured_at") val capturedAt: String? = null
)

// ─── pinch-cancel ──────────────────────────────────────────────────────────

/** Unsigned cancel request — `apikey` + Bearer only, no `x-device-signature`. */
@Serializable
data class CancelRequest(
    @SerialName("alert_id") val alertId: String,
    @SerialName("client_request_id") val clientRequestId: String,
    val reason: String
)

@Serializable
data class CancelResponse(
    val ok: Boolean? = null,
    @SerialName("cancel_requested") val cancelRequested: Boolean? = null,
    val alert: IngestAlert? = null
) {
    val uiStatus: String? get() = alert?.uiStatus
}

// ─── pinch-alert-details ───────────────────────────────────────────────────

/**
 * Echo response for `pinch-alert-details`. The request body is built per-kind
 * as a `JsonObject` (only the present keys) and signed — see
 * [com.faster.festival.data.sos.SosRepositoryImpl.sendAlertDetails].
 */
@Serializable
data class AlertDetailsResponse(
    val ok: Boolean? = null,
    val deduplicated: Boolean? = null,
    @SerialName("attendee_details") val attendeeDetails: kotlinx.serialization.json.JsonObject? = null
)

// ─── pinch-alert-status ────────────────────────────────────────────────────

/**
 * Real backend shape for `pinch-alert-status`:
 *
 * ```
 * {
 *   "recommended_step": "PROVIDE_DETAILS",
 *   "tracking_session_id": null,
 *   "polling": {
 *     "continue": true,
 *     "foreground_interval_seconds": 3,
 *     "background_interval_seconds": 15
 *   },
 *   "user_status": "DISPATCH_CONFIRMED",
 *   "aws_status": "CONFIRMED",
 *   "assignment": null
 * }
 * ```
 *
 * The response is FLAT — no `alert` wrapper. Server tells us its own polling
 * cadence ([PollingConfig]) and whether to keep polling ([PollingConfig.shouldContinue]).
 * Responder identity and ETA come back in [SosAssignment] once dispatch
 * confirms — `null` until then.
 *
 * Convenience read-only properties [responderName] / [etaMinutes] / [alertId]
 * pass through to [assignment] so existing call sites that read those fields
 * directly off the alert continue to work.
 */
@Serializable
data class SosStatusResponse(
    val ok: Boolean? = null,
    val view: String? = null,
    @SerialName("alert_id") val alertIdField: String? = null,
    @SerialName("user_status") val userStatus: String? = null,
    @SerialName("ui_status") val uiStatus: String? = null,
    @SerialName("recommended_step") val recommendedStep: String? = null,
    val cancellation: SosCancellation? = null,
    val polling: PollingConfig? = null,
    val responder: SosResponder? = null,
    val eta: SosEta? = null,
    // ── legacy fields kept for back-compat with the prior flat contract ──
    @SerialName("tracking_session_id") val trackingSessionId: String? = null,
    @SerialName("aws_status") val awsStatus: String? = null,
    val assignment: SosAssignment? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    /** No responder NAME in the new contract — only status/message. */
    val responderName: String? get() = assignment?.responderName
    val responderMessage: String? get() = responder?.message ?: assignment?.status
    val responderStatus: String? get() = responder?.status

    /** Human ETA banner straight from the backend ("Arrives between 12:31-12:36"). */
    val etaLabel: String? get() = eta?.label

    /** Minutes — derived from `eta.seconds` (rounded up), falling back to legacy `assignment.eta_minutes`. */
    val etaMinutes: Int? get() = eta?.seconds?.let { (it + 59) / 60 } ?: assignment?.etaMinutes

    val alertId: String? get() = alertIdField ?: assignment?.alertId
}

@Serializable
data class SosResponder(
    val status: String? = null,
    @SerialName("last_event_at") val lastEventAt: String? = null,
    val message: String? = null
)

@Serializable
data class SosEta(
    val label: String? = null,
    val seconds: Int? = null,
    @SerialName("min_seconds") val minSeconds: Int? = null,
    @SerialName("max_seconds") val maxSeconds: Int? = null,
    @SerialName("occurred_at") val occurredAt: String? = null
)

@Serializable
data class SosCancellation(
    val status: String? = null,
    val reason: String? = null,
    @SerialName("requested_at") val requestedAt: String? = null
)

/**
 * Server-driven polling cadence. Mobile client honors
 * `foreground_interval_seconds` when present and falls back to the
 * spec-default 1.5 s / 4 s cadence only if the field is absent.
 *
 * `continue=false` ends the polling loop immediately, in addition to
 * any terminal `user_status` ([com.faster.festival.domain.sos.SosUserStatus.isTerminal]).
 */
@Serializable
data class PollingConfig(
    @SerialName("continue") val shouldContinue: Boolean = true,
    @SerialName("foreground_interval_seconds") val foregroundIntervalSeconds: Int? = null,
    @SerialName("background_interval_seconds") val backgroundIntervalSeconds: Int? = null
)

@Serializable
data class SosAssignment(
    @SerialName("alert_id") val alertId: String? = null,
    @SerialName("responder_name") val responderName: String? = null,
    @SerialName("responder_id") val responderId: String? = null,
    @SerialName("eta_minutes") val etaMinutes: Int? = null,
    @SerialName("status") val status: String? = null
)

/**
 * Back-compat alias — pre-refactor code referenced `SosAlert` everywhere.
 * The flat [SosStatusResponse] is now the wire shape AND the domain model.
 */
typealias SosAlert = SosStatusResponse

// ─── pinch-alert-history ───────────────────────────────────────────────────

/**
 * `pinch-alert-history` response — the authoritative list of this user's past
 * SOS alerts (keyed off the bearer JWT). Cursor-paginated: pass
 * [nextCursor] back as `cursor` to fetch the next page.
 *
 * ```
 * { "ok": true,
 *   "alerts": [ { …PinchAlertHistoryItem… } ],
 *   "next_cursor": null, "limit": 20 }
 * ```
 */
@Serializable
data class PinchAlertHistoryResponse(
    val ok: Boolean? = null,
    val alerts: List<PinchAlertHistoryItem> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    val limit: Int? = null
)

/**
 * One historical SOS alert. `ui_status` / `user_status` are the authoritative
 * terminal states the list renders (`COMPLETED`, `CANCELLED`, `REJECTED`, …);
 * [latestLocation] reuses the same shape as `pinch-update-location`.
 */
@Serializable
data class PinchAlertHistoryItem(
    val id: String? = null,
    @SerialName("client_trigger_id") val clientTriggerId: String? = null,
    @SerialName("festival_id") val festivalId: String? = null,
    @SerialName("trigger_source") val triggerSource: String? = null,
    @SerialName("wristband_id") val wristbandId: String? = null,
    val priority: String? = null,
    val status: String? = null,
    @SerialName("user_status") val userStatus: String? = null,
    @SerialName("ui_status") val uiStatus: String? = null,
    @SerialName("is_terminal") val isTerminal: Boolean? = null,
    @SerialName("received_at") val receivedAt: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("latest_location") val latestLocation: LatestLocation? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
