package com.faster.festival.wristband.data.ble

import com.faster.festival.wristband.domain.model.DeviceEvent
import com.faster.festival.wristband.domain.model.DeviceEventCode
import com.faster.festival.wristband.domain.model.DeviceState
import com.faster.festival.wristband.domain.model.DeviceStatus
import com.faster.festival.wristband.domain.model.SosCancel
import com.faster.festival.wristband.domain.model.SosEvent
import com.faster.festival.wristband.domain.model.SosState
import com.faster.festival.wristband.domain.model.Telemetry
import timber.log.Timber

/**
 * Canonical parser for inbound FastER vendor-model messages.
 *
 * One entry point — [parse]. Each sub-command branch reads through a
 * [LittleEndianReader] so per-field bounds are enforced uniformly; any
 * underrun lands as a [VendorMessage.MalformedMessage] rather than crashing
 * the mesh stack. No raw `ByteBuffer` plumbing lives outside the reader.
 *
 * This object is the single source of truth for byte → message decoding.
 * [VendorPayloadCodec.decode] now delegates here and adapts the result back
 * to its legacy `Inbound` shape for callers that haven't migrated yet.
 *
 * Validation policy:
 *  • Sub-command byte unknown ⇒ [VendorMessage.UnknownVendorMessage] (forward-compat).
 *  • Payload too short for sub-cmd ⇒ [VendorMessage.MalformedMessage] with size info.
 *  • Field-level enum miss (e.g. unknown `device_state` byte) is tolerated —
 *    the typed enum's `fromRaw` returns `Unknown`. We do NOT downgrade the
 *    whole packet to Malformed for an unknown enum value; new firmware
 *    states would otherwise blackhole at the mobile parser.
 */
object VendorMessageParser {

    /** Telemetry accel scale per firmware spec (LSB per g). */
    private const val ACCEL_LSB_PER_G = MeshConstants.ACCEL_LSB_PER_G

    fun parse(
        raw: ByteArray,
        nowMs: Long = System.currentTimeMillis()
    ): VendorMessage {
        if (raw.isEmpty()) {
            return VendorMessage.MalformedMessage(
                subCmd = -1,
                reason = "empty payload",
                raw = raw,
                receivedAtMs = nowMs
            )
        }
        val sub = raw[0]
        val subInt = sub.toInt() and 0xFF
        return try {
            when (sub) {
                MeshConstants.SUB_TELEMETRY -> parseTelemetry(raw, nowMs)
                MeshConstants.SUB_SOS -> parseSos(raw, nowMs)
                MeshConstants.SUB_SOS_CANCEL -> parseSosCancel(raw, nowMs)
                MeshConstants.SUB_STATUS -> parseStatus(raw, nowMs)
                MeshConstants.SUB_DEVICE_EVENT -> parseDeviceEvent(raw, nowMs)
                else -> {
                    Timber.tag(TAG).d(
                        "Unknown sub-cmd 0x%02X size=%d", subInt, raw.size
                    )
                    VendorMessage.UnknownVendorMessage(
                        subCmd = subInt,
                        raw = raw,
                        receivedAtMs = nowMs
                    )
                }
            }
        } catch (mpe: MalformedPacketException) {
            Timber.tag(TAG).w(
                "Malformed sub-cmd 0x%02X — %s | size=%d hex=%s",
                subInt, mpe.message, raw.size, raw.toShortHex()
            )
            VendorMessage.MalformedMessage(
                subCmd = subInt,
                reason = mpe.message ?: "malformed",
                raw = raw,
                receivedAtMs = nowMs
            )
        }
    }

    // ─── 0x10 telemetry ────────────────────────────────────────────────────
    // Layout (LE):
    //   [0]    sub-cmd                u8
    //   [1-2]  seq                    u16
    //   [3-4]  accel_x_raw            s16  (LSB = 1/ACCEL_LSB_PER_G g)
    //   [5-6]  accel_y_raw            s16
    //   [7-8]  accel_z_raw            s16
    //   [9-10] peak_mag_raw           u16
    //   [11]   motion_flag            u8   (1 = motion)
    //   [12]   battery_pct            u8
    //   [13]   device_state           u8
    // Minimum 14 bytes.
    private fun parseTelemetry(raw: ByteArray, nowMs: Long): VendorMessage {
        if (raw.size < 14) {
            return VendorMessage.MalformedMessage(
                subCmd = 0x10,
                reason = "telemetry needs ≥14 bytes (got ${raw.size})",
                raw = raw,
                receivedAtMs = nowMs
            )
        }
        val r = LittleEndianReader(raw).seek(1)
        val seq = r.u16("seq")
        val ax = r.s16("accel_x") / ACCEL_LSB_PER_G
        val ay = r.s16("accel_y") / ACCEL_LSB_PER_G
        val az = r.s16("accel_z") / ACCEL_LSB_PER_G
        val peak = r.u16("peak_mag") / ACCEL_LSB_PER_G
        val motion = r.u8("motion") == 1
        val battery = r.u8("battery").coerceIn(0, 100)
        val state = DeviceState.fromRaw(r.u8("device_state"))
        return VendorMessage.TelemetryMessage(
            Telemetry(seq, ax, ay, az, peak, motion, battery, state, nowMs)
        )
    }

    // ─── 0x11 SOS ──────────────────────────────────────────────────────────
    // [0]      sub-cmd
    // [1-4]    event_id     u32
    // [5]      state        u8 (SosState raw)
    // [6-9]    device_uptime u32 (ms or s — fw spec is "uptime/timestamp")
    // [10]     retry_count  u8
    // [11]     battery_pct  u8
    // Minimum 12 bytes.
    private fun parseSos(raw: ByteArray, nowMs: Long): VendorMessage {
        if (raw.size < 12) {
            return VendorMessage.MalformedMessage(
                subCmd = 0x11,
                reason = "SOS needs ≥12 bytes (got ${raw.size})",
                raw = raw,
                receivedAtMs = nowMs
            )
        }
        val r = LittleEndianReader(raw).seek(1)
        val eventId = r.u32("event_id")
        val state = SosState.fromRaw(r.u8("sos_state"))
        val uptime = r.u32("device_uptime")
        val retry = r.u8("retry_count")
        val battery = r.u8("battery").coerceIn(0, 100)
        return VendorMessage.SosEmergencyMessage(
            SosEvent(eventId, state, uptime, retry, battery, nowMs)
        )
    }

    // ─── 0x12 SOS cancel ──────────────────────────────────────────────────
    // [0]      sub-cmd
    // [1-4]    event_id     u32
    // [5]      reason       u8
    // Minimum 6 bytes.
    private fun parseSosCancel(raw: ByteArray, nowMs: Long): VendorMessage {
        if (raw.size < 6) {
            return VendorMessage.MalformedMessage(
                subCmd = 0x12,
                reason = "SOS cancel needs ≥6 bytes (got ${raw.size})",
                raw = raw,
                receivedAtMs = nowMs
            )
        }
        val r = LittleEndianReader(raw).seek(1)
        val eventId = r.u32("event_id")
        val reason = r.u8("reason")
        return VendorMessage.SosCancelMessage(SosCancel(eventId, reason, nowMs))
    }

    // ─── 0x13 Device status (PROVISIONAL — firmware spec §45 marks STUB) ───
    // [0]       sub-cmd
    // [1-2]     seq           u16
    // [3]       battery_pct   u8
    // [4]       device_state  u8 (DeviceState raw)
    // [5]       fw_major      u8
    // [6]       fw_minor      u8
    // [7]       fw_patch      u8
    // [8-11]    uptime_ms     u32
    // [12]      flags         u8  (bit0=nfc_present, bit1=charging)  — optional
    //
    // Minimum [MeshConstants.STATUS_MIN_BYTES] = 12 (flags optional, defaults to 0).
    private fun parseStatus(raw: ByteArray, nowMs: Long): VendorMessage {
        if (raw.size < MeshConstants.STATUS_MIN_BYTES) {
            return VendorMessage.MalformedMessage(
                subCmd = 0x13,
                reason = "status needs ≥${MeshConstants.STATUS_MIN_BYTES} bytes (got ${raw.size})",
                raw = raw,
                receivedAtMs = nowMs
            )
        }
        val r = LittleEndianReader(raw).seek(1)
        val seq = r.u16("seq")
        val battery = r.u8("battery").coerceIn(0, 100)
        val state = DeviceState.fromRaw(r.u8("device_state"))
        val major = r.u8("fw_major")
        val minor = r.u8("fw_minor")
        val patch = r.u8("fw_patch")
        val uptime = r.u32("uptime_ms")
        val flags = if (r.remaining >= 1) r.u8("flags") else 0
        return VendorMessage.DeviceStatusMessage(
            DeviceStatus(
                seqNum = seq,
                batteryPct = battery,
                deviceState = state,
                firmwareVersion = "$major.$minor.$patch",
                uptimeMs = uptime,
                flags = flags,
                receivedAtMs = nowMs
            )
        )
    }

    // ─── 0x14 Device event ─────────────────────────────────────────────────
    // [0]      sub-cmd
    // [1]      timestamp_or_padding (firmware spec §7.5 — single info byte)
    // [2]      event_code   u8 (DeviceEventCode raw)
    // Minimum 3 bytes.
    private fun parseDeviceEvent(raw: ByteArray, nowMs: Long): VendorMessage {
        if (raw.size < 3) {
            return VendorMessage.MalformedMessage(
                subCmd = 0x14,
                reason = "device event needs ≥3 bytes (got ${raw.size})",
                raw = raw,
                receivedAtMs = nowMs
            )
        }
        val r = LittleEndianReader(raw).seek(1)
        // Spec sketch reads ONE byte at offset 1 then the event code at
        // offset 2; we mirror that exactly. The middle byte is captured
        // by the reader cursor advance but isn't surfaced — keep parity
        // with the prior codec.
        @Suppress("UNUSED_VARIABLE")
        val ignored = r.u8("reserved")
        val code = DeviceEventCode.fromRaw(r.u8("event_code"))
        return VendorMessage.DeviceEventMessage(DeviceEvent(code, nowMs))
    }

    private fun ByteArray.toShortHex(maxBytes: Int = 16): String =
        joinToString(separator = "") { "%02X".format(it) }
            .take(maxBytes * 2)

    private const val TAG = "VendorParser"
}
