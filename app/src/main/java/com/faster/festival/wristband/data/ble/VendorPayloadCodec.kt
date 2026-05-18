package com.faster.festival.wristband.data.ble

import com.faster.festival.wristband.domain.model.DeviceEvent
import com.faster.festival.wristband.domain.model.DeviceStatus
import com.faster.festival.wristband.domain.model.SosCancel
import com.faster.festival.wristband.domain.model.SosEvent
import com.faster.festival.wristband.domain.model.Telemetry

/**
 * Legacy adapter over the canonical [VendorMessageParser] / [VendorMessageEncoder].
 *
 * Pre-existing call sites (`NordicMeshManager.onMeshMessageReceived`,
 * `WristbandMeshRepositoryImpl`, `FakeMeshManager`) consume the
 * [Inbound] hierarchy and the `encodeSosXxx` helpers exposed here.
 * Rather than rewrite every consumer, this object stays in place but
 * delegates every byte operation to the canonical parser/encoder so
 * there is exactly ONE implementation of LE decoding and outbound
 * packing in the codebase.
 *
 * New code SHOULD prefer [VendorMessageParser.parse] + the typed
 * [VendorMessage] hierarchy and [VendorMessageEncoder] directly.
 */
object VendorPayloadCodec {

    sealed class Inbound {
        data class TelemetryMsg(val telemetry: Telemetry) : Inbound()
        data class SosMsg(val event: SosEvent) : Inbound()
        data class SosCancelMsg(val cancel: SosCancel) : Inbound()
        /** 0x13 device-status snapshot (added with vendor-model gap-fill). */
        data class StatusMsg(val status: DeviceStatus) : Inbound()
        data class DeviceEventMsg(val event: DeviceEvent) : Inbound()
        data class Unknown(val subCmd: Int, val raw: ByteArray) : Inbound()
    }

    fun decode(raw: ByteArray, nowMs: Long = System.currentTimeMillis()): Inbound =
        VendorMessageParser.parse(raw, nowMs).toLegacyInbound(raw)

    fun encodeSosAck(eventId: Long, helpDispatched: Boolean): ByteArray =
        VendorMessageEncoder.sosAck(eventId, helpDispatched)

    fun encodeSosResponder(eventId: Long, etaMinutes: Int): ByteArray =
        VendorMessageEncoder.sosResponder(eventId, etaMinutes)

    fun encodeSosResolved(eventId: Long, falseAlarm: Boolean): ByteArray =
        VendorMessageEncoder.sosResolved(eventId, falseAlarm)

    /** 0x23 NFC result — see [VendorMessageEncoder.nfcResult]. */
    fun encodeNfcResult(
        scanId: Long,
        result: NfcResultCode,
        tagUid: ByteArray = ByteArray(0)
    ): ByteArray = VendorMessageEncoder.nfcResult(scanId, result, tagUid)

    fun encodeConfig(key: Int, value: Int): ByteArray =
        VendorMessageEncoder.config(key, value)

    /**
     * Bridge between the new sealed [VendorMessage] hierarchy and the legacy
     * [Inbound] one. Malformed / underrun packets collapse to [Inbound.Unknown]
     * so existing consumers (which already filter `Unknown` out at the repo
     * boundary) continue to work unchanged.
     */
    private fun VendorMessage.toLegacyInbound(raw: ByteArray): Inbound = when (this) {
        is VendorMessage.TelemetryMessage -> Inbound.TelemetryMsg(telemetry)
        is VendorMessage.SosEmergencyMessage -> Inbound.SosMsg(event)
        is VendorMessage.SosCancelMessage -> Inbound.SosCancelMsg(cancel)
        is VendorMessage.DeviceStatusMessage -> Inbound.StatusMsg(status)
        is VendorMessage.DeviceEventMessage -> Inbound.DeviceEventMsg(event)
        is VendorMessage.MalformedMessage -> Inbound.Unknown(subCmd, raw)
        is VendorMessage.UnknownVendorMessage -> Inbound.Unknown(subCmd, raw)
    }
}
