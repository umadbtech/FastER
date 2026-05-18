package com.faster.festival.wristband.data.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Bounds-checked little-endian reader over a `ByteArray`. Provides the same
 * cursor model as [ByteBuffer] but enforces per-field bounds and surfaces a
 * typed [MalformedPacketException] on underrun instead of leaking a raw
 * `BufferUnderflowException` / `ArrayIndexOutOfBoundsException` up the
 * coroutine stack.
 *
 * Used by [VendorMessageParser] and the lower-level [VendorPayloadCodec] —
 * neither owns its own LE plumbing.
 *
 * Cheap: wraps the underlying array, no copy. Single-threaded by design;
 * the parser allocates a fresh reader per inbound packet.
 */
class LittleEndianReader(private val raw: ByteArray) {

    private val buf: ByteBuffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)

    val size: Int get() = raw.size
    val position: Int get() = buf.position()
    val remaining: Int get() = buf.remaining()

    fun seek(offset: Int): LittleEndianReader {
        ensure(offset, label = "seek")
        buf.position(offset)
        return this
    }

    fun u8(label: String = "u8"): Int {
        ensure(1, label)
        return buf.get().toInt() and 0xFF
    }

    fun u16(label: String = "u16"): Int {
        ensure(2, label)
        return buf.short.toInt() and 0xFFFF
    }

    fun s16(label: String = "s16"): Int {
        ensure(2, label)
        return buf.short.toInt()
    }

    fun u32(label: String = "u32"): Long {
        ensure(4, label)
        return buf.int.toLong() and 0xFFFFFFFFL
    }

    fun s32(label: String = "s32"): Int {
        ensure(4, label)
        return buf.int
    }

    /** Read [count] bytes verbatim into a freshly allocated array. */
    fun bytes(count: Int, label: String = "bytes"): ByteArray {
        ensure(count, label)
        val out = ByteArray(count)
        buf.get(out)
        return out
    }

    /** Peek the next byte without advancing the cursor. */
    fun peekU8(): Int {
        ensure(1, "peek")
        return raw[position].toInt() and 0xFF
    }

    private fun ensure(bytes: Int, label: String) {
        if (buf.remaining() < bytes) {
            throw MalformedPacketException(
                "underrun reading $label: need $bytes byte(s), have ${buf.remaining()} " +
                    "(total=${raw.size}, position=${buf.position()})"
            )
        }
    }
}

/**
 * Thrown by [LittleEndianReader] when a read would walk past the end of the
 * payload. The parser catches it and returns a `MalformedMessage` rather than
 * propagating — the BLE Mesh stack must not blow up on a single bad PDU.
 */
class MalformedPacketException(message: String) : RuntimeException(message)
