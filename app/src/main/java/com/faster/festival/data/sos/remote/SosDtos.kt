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

@Serializable
data class SosIngestResponse(
    val ok: Boolean? = null,
    @SerialName("client_trigger_id") val clientTriggerId: String? = null,
    @SerialName("alert_id") val alertId: String? = null,
    val status: String? = null
)

// ─── pinch-update-location ─────────────────────────────────────────────────

/**
 * Body for `pinch-update-location` — periodic GPS push while an SOS is in
 * flight. Same Ed25519 signing envelope as `pinch-ingest` (the canonical
 * string just uses `/pinch-update-location` as the path).
 *
 * `tracking_session_id` is mandatory — dispatch hands it to us via
 * `pinch-alert-status` once they accept the alert. We don't fire updates
 * until that value is non-null.
 */
@Serializable
data class LocationUpdateRequest(
    @SerialName("client_trigger_id") val clientTriggerId: String,
    @SerialName("tracking_session_id") val trackingSessionId: String,
    @SerialName("device_id") val deviceId: String,
    val nonce: String,
    val timestamp: String,
    val location: SosLocation,
    @SerialName("device_context") val deviceContext: DeviceContext
)

@Serializable
data class LocationUpdateResponse(
    val ok: Boolean? = null,
    val status: String? = null
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
    @SerialName("recommended_step") val recommendedStep: String? = null,
    @SerialName("tracking_session_id") val trackingSessionId: String? = null,
    val polling: PollingConfig? = null,
    @SerialName("user_status") val userStatus: String? = null,
    @SerialName("aws_status") val awsStatus: String? = null,
    val assignment: SosAssignment? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val responderName: String? get() = assignment?.responderName
    val etaMinutes: Int? get() = assignment?.etaMinutes
    val alertId: String? get() = assignment?.alertId
}

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
