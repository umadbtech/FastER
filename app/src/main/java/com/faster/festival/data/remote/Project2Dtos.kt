package com.faster.festival.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the Project 2 surface (signed dispatch + raw telemetry ingest).
 *
 * Signed payloads (pinch-ingest / pinch-update-location) live on the existing
 * [com.faster.festival.data.sos.remote.SosApiService] — they're owned by the
 * SOS pipeline, which holds the Ed25519 signer. [Project2ApiService] does
 * NOT re-declare those endpoints; [PinchIngestRepository] is the thin facade
 * other callers use to dispatch one without depending on the broader SOS
 * surface.
 *
 * The dispatch-status DTO mirrors [com.faster.festival.data.sos.remote.SosIngestResponse]
 * but lives here so non-SOS callers (e.g. an automation harness) can talk to
 * pinch-ingest without importing the SOS package.
 */

@Serializable
data class PinchIngestDispatchResult(
    val ok: Boolean = true,
    @SerialName("client_trigger_id") val clientTriggerId: String,
    @SerialName("alert_id") val alertId: String? = null,
    val status: String? = null,
    @SerialName("user_status") val userStatus: String? = null
)
