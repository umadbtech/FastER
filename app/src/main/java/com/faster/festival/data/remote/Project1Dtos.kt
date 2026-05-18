package com.faster.festival.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the Project 1 wristband-CRUD + SOS-history surface. Field shapes
 * match `Wristband-Backend-API.md` §5 1:1 except endpoints are addressed as
 * Supabase Edge Functions (`functions/v1/<name>`) per project convention.
 */

// ─── 5.1 wristband-pair ────────────────────────────────────────────────────

@Serializable
data class WristbandPairRequest(
    @SerialName("wristband_id") val wristbandId: String,
    @SerialName("unicast_address") val unicastAddress: Int,
    @SerialName("group_address") val groupAddress: Int = 0xC000,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("firmware_version") val firmwareVersion: String? = null,
    @SerialName("paired_at") val pairedAt: String
)

@Serializable
data class WristbandPairing(
    @SerialName("pairing_id") val pairingId: String,
    @SerialName("wristband_id") val wristbandId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("unicast_address") val unicastAddress: Int,
    @SerialName("group_address") val groupAddress: Int,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("firmware_version") val firmwareVersion: String? = null,
    @SerialName("paired_at") val pairedAt: String,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("battery_pct") val batteryPct: Int? = null,
    @SerialName("device_state") val deviceState: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

// ─── 5.3 wristband-unpair ──────────────────────────────────────────────────

@Serializable
data class WristbandUnpairRequest(
    val reason: String = "user_initiated"
)

@Serializable
data class WristbandUnpairResponse(
    @SerialName("pairing_id") val pairingId: String? = null,
    @SerialName("wristband_id") val wristbandId: String,
    @SerialName("unpaired_at") val unpairedAt: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    val reason: String? = null
)

// ─── 5.4 wristband-heartbeat (telemetry batch fits here too) ────────────────

@Serializable
data class TelemetrySnapshot(
    @SerialName("seq_num") val seqNum: Int,
    @SerialName("accel_x_g") val accelX: Float,
    @SerialName("accel_y_g") val accelY: Float,
    @SerialName("accel_z_g") val accelZ: Float,
    @SerialName("peak_mag_g") val peakMag: Float,
    val motion: Boolean,
    @SerialName("battery_pct") val batteryPct: Int,
    @SerialName("device_state") val deviceState: String,
    @SerialName("received_at") val receivedAt: String
)

@Serializable
data class WristbandHeartbeatRequest(
    @SerialName("last_seen_at") val lastSeenAt: String,
    @SerialName("battery_pct") val batteryPct: Int? = null,
    @SerialName("device_state") val deviceState: String? = null,
    @SerialName("rssi_dbm") val rssiDbm: Int? = null,
    val telemetry: TelemetrySnapshot? = null
)

@Serializable
data class WristbandTelemetryBatchRequest(
    @SerialName("wristband_id") val wristbandId: String,
    @SerialName("snapshots") val snapshots: List<TelemetrySnapshot>
)

@Serializable
data class WristbandTelemetryBatchResponse(
    val ok: Boolean = true,
    val accepted: Int = 0,
    val rejected: Int = 0
)

// ─── 5.5 wristband-sos-record (audit POST when 0x11 lands) ─────────────────

@Serializable
data class SosPhoneLocation(
    val lat: Double,
    val lng: Double,
    @SerialName("accuracy_m") val accuracyMeters: Int? = null,
    @SerialName("captured_at") val capturedAt: String
)

@Serializable
data class WristbandSosRecordRequest(
    @SerialName("event_id") val eventId: Long,
    val state: String,
    @SerialName("retry_count") val retryCount: Int,
    @SerialName("battery_pct") val batteryPct: Int,
    @SerialName("device_uptime_ms") val deviceUptimeMs: Long,
    @SerialName("received_at") val receivedAt: String,
    @SerialName("client_trigger_id") val clientTriggerId: String,
    @SerialName("phone_location") val phoneLocation: SosPhoneLocation? = null
)

@Serializable
data class WristbandSosRecordResponse(
    @SerialName("sos_id") val sosId: String? = null,
    @SerialName("pairing_id") val pairingId: String? = null,
    @SerialName("wristband_id") val wristbandId: String? = null,
    @SerialName("event_id") val eventId: Long? = null,
    val state: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("dispatch_console_url") val dispatchConsoleUrl: String? = null
)

// ─── 5.7 wristband-sos-history (optional) ───────────────────────────────────

@Serializable
data class SosHistoryItem(
    @SerialName("sos_id") val sosId: String,
    @SerialName("event_id") val eventId: Long,
    val state: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("battery_pct") val batteryPct: Int? = null,
    @SerialName("false_alarm") val falseAlarm: Boolean? = null,
    @SerialName("responder_dispatched_at") val responderDispatchedAt: String? = null
)

@Serializable
data class SosHistoryResponse(
    val items: List<SosHistoryItem> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null
)
