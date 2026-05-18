package com.faster.festival.wristband.data.ble

import com.faster.festival.wristband.domain.model.DeviceEvent
import com.faster.festival.wristband.domain.model.DeviceStatus
import com.faster.festival.wristband.domain.model.SosCancel
import com.faster.festival.wristband.domain.model.SosEvent
import com.faster.festival.wristband.domain.model.Telemetry

/**
 * Sealed hierarchy of inbound FastER vendor-model messages.
 *
 * `VendorMessage` is the public surface produced by [VendorMessageParser].
 * The lower-level [VendorPayloadCodec.Inbound] still exists for the inbound
 * flows that were wired before this sealed model landed (NordicMeshManager
 * etc.); the codec internally produces `VendorMessage` and bridges to
 * `Inbound` for back-compat. There is exactly ONE parser implementation —
 * see [VendorMessageParser].
 *
 * Sub-command mapping (per `Mobile-Vendor-Model-Dev.md` and the FastER
 * vendor model spec — Company 0x0030, Model 0x0001):
 *
 * | Sub-cmd | Direction | Maps to |
 * |---|---|---|
 * | 0x10 | inbound | [TelemetryMessage] |
 * | 0x11 | inbound | [SosEmergencyMessage] |
 * | 0x12 | inbound | [SosCancelMessage] |
 * | 0x13 | inbound | [DeviceStatusMessage] |
 * | 0x14 | inbound | [DeviceEventMessage] |
 * | other | inbound | [UnknownVendorMessage] |
 * | any | underrun | [MalformedMessage] |
 */
sealed interface VendorMessage {

    /** Phone-side receive timestamp for every variant. */
    val receivedAtMs: Long

    /** 0x10 — periodic telemetry from the wristband (1 Hz nominal). */
    data class TelemetryMessage(val telemetry: Telemetry) : VendorMessage {
        override val receivedAtMs: Long get() = telemetry.receivedAtMs
    }

    /** 0x11 — SOS active (firmware retries 1/2/4/8/16/30 s with same event_id). */
    data class SosEmergencyMessage(val event: SosEvent) : VendorMessage {
        override val receivedAtMs: Long get() = event.receivedAtMs
    }

    /** 0x12 — user-canceled SOS (button release inside cancel window). */
    data class SosCancelMessage(val cancel: SosCancel) : VendorMessage {
        override val receivedAtMs: Long get() = cancel.receivedAtMs
    }

    /** 0x13 — on-demand or post-boot device status snapshot. */
    data class DeviceStatusMessage(val status: DeviceStatus) : VendorMessage {
        override val receivedAtMs: Long get() = status.receivedAtMs
    }

    /** 0x14 — device lifecycle event (pair window, sleep, wake, etc.). */
    data class DeviceEventMessage(val event: DeviceEvent) : VendorMessage {
        override val receivedAtMs: Long get() = event.receivedAtMs
    }

    /**
     * Sub-command byte recognized but payload didn't pass validation
     * (too short, invalid enum, out-of-range value). The raw bytes are
     * preserved so a unit test or logcat dump can replay the failure.
     */
    data class MalformedMessage(
        val subCmd: Int,
        val reason: String,
        val raw: ByteArray,
        override val receivedAtMs: Long
    ) : VendorMessage {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MalformedMessage) return false
            return subCmd == other.subCmd &&
                reason == other.reason &&
                raw.contentEquals(other.raw) &&
                receivedAtMs == other.receivedAtMs
        }

        override fun hashCode(): Int {
            var r = subCmd
            r = 31 * r + reason.hashCode()
            r = 31 * r + raw.contentHashCode()
            r = 31 * r + receivedAtMs.hashCode()
            return r
        }
    }

    /**
     * Sub-command byte the parser doesn't recognize. Forward-compat surface
     * for firmware that ships a new opcode ahead of the mobile release.
     */
    data class UnknownVendorMessage(
        val subCmd: Int,
        val raw: ByteArray,
        override val receivedAtMs: Long
    ) : VendorMessage {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UnknownVendorMessage) return false
            return subCmd == other.subCmd &&
                raw.contentEquals(other.raw) &&
                receivedAtMs == other.receivedAtMs
        }

        override fun hashCode(): Int {
            var r = subCmd
            r = 31 * r + raw.contentHashCode()
            r = 31 * r + receivedAtMs.hashCode()
            return r
        }
    }
}
