package com.faster.festival.wristband.data.ble

import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Canonical encoder for outbound FastER vendor-model packets.
 *
 * | Sub-cmd | Helper |
 * |---|---|
 * | 0x20 | [sosAck] |
 * | 0x21 | [sosResponder] |
 * | 0x22 | [sosResolved] |
 * | 0x23 | [nfcResult] |
 * | 0x30 | [config] |
 *
 * The lower-level [VendorPayloadCodec] now delegates here for every outbound
 * helper so there is exactly ONE byte-packing path. New 0x23 NFC result was
 * not present on the codec before — this object is the only place that
 * builds it.
 *
 * All multi-byte fields are LE, matching the firmware spec.
 */
object VendorMessageEncoder {

    /** 0x20 — auto-ACK every inbound 0x11 within the firmware retry window. */
    fun sosAck(eventId: Long, helpDispatched: Boolean): ByteArray =
        ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(MeshConstants.SUB_SOS_ACK)
            putInt(eventId.toInt())
            put(if (helpDispatched) 0x01 else 0x00)
        }.array().also { logOutbound(0x20, it) }

    /** 0x21 — responder dispatched; ETA clamped to a single u8 (0..255). */
    fun sosResponder(eventId: Long, etaMinutes: Int): ByteArray =
        ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(MeshConstants.SUB_SOS_RESPONDER)
            putInt(eventId.toInt())
            put(etaMinutes.coerceIn(0, 0xFF).toByte())
        }.array().also { logOutbound(0x21, it) }

    /** 0x22 — SOS resolved; flag indicates whether dispatch marked a false alarm. */
    fun sosResolved(eventId: Long, falseAlarm: Boolean): ByteArray =
        ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(MeshConstants.SUB_SOS_RESOLVED)
            putInt(eventId.toInt())
            put(if (falseAlarm) 0x01 else 0x00)
        }.array().also { logOutbound(0x22, it) }

    /**
     * 0x23 — NFC scan result (PROVISIONAL — firmware spec §45/73 marks STUB).
     *
     * Layout (LE, [MeshConstants.NFC_RESULT_BYTES] = 14 bytes total):
     * ```
     *   [0]      sub-cmd            u8
     *   [1-4]    scan_id            u32   (caller-generated, monotonic per scan)
     *   [5]      result             u8    (see NfcResultCode below)
     *   [6-13]   tag_uid            8B    (padded with 0x00; truncated to 8 if longer)
     * ```
     *
     * @param scanId monotonic per scan; firmware uses it to correlate with its
     *   own scan request id.
     * @param result one of [NfcResultCode].
     * @param tagUid up to 8 bytes; shorter UIDs are right-padded with zeros,
     *   longer UIDs are truncated. Callers should normalize beforehand if
     *   they care.
     */
    fun nfcResult(
        scanId: Long,
        result: NfcResultCode,
        tagUid: ByteArray = ByteArray(0)
    ): ByteArray {
        val uid = ByteArray(MeshConstants.NFC_TAG_UID_BYTES)
        val copyLen = minOf(tagUid.size, MeshConstants.NFC_TAG_UID_BYTES)
        if (copyLen > 0) System.arraycopy(tagUid, 0, uid, 0, copyLen)
        return ByteBuffer.allocate(MeshConstants.NFC_RESULT_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                put(MeshConstants.SUB_NFC_RESULT)
                putInt(scanId.toInt())
                put(result.raw.toByte())
                put(uid)
            }.array().also { logOutbound(0x23, it) }
    }

    /**
     * 0x30 — remote-config write (firmware-defined key/value pair).
     * Preserved here so callers don't reach back into the legacy codec.
     */
    fun config(key: Int, value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(MeshConstants.SUB_CONFIG)
            put((key and 0xFF).toByte())
            putShort((value and 0xFFFF).toShort())
        }.array().also { logOutbound(0x30, it) }

    private fun logOutbound(subCmd: Int, bytes: ByteArray) {
        // Hex dump is cheap; truncate so we never spam a 60+ byte line in
        // production. Tag matches the inbound parser for easy logcat filtering.
        Timber.tag("VendorEncoder").d(
            "→ sub-cmd 0x%02X size=%d hex=%s", subCmd, bytes.size, bytes.toShortHex()
        )
    }

    private fun ByteArray.toShortHex(maxBytes: Int = 24): String =
        joinToString(separator = "") { "%02X".format(it) }
            .take(maxBytes * 2)
}

/**
 * 0x23 result codes — PROVISIONAL. Firmware spec hasn't locked the enum yet.
 */
enum class NfcResultCode(val raw: Int) {
    Success(0x00),
    UnknownTag(0x01),
    ReadError(0x02),
    Rejected(0x03),
    HardwareUnavailable(0x04);

    companion object {
        fun fromRaw(b: Int): NfcResultCode =
            values().firstOrNull { it.raw == b } ?: ReadError
    }
}
