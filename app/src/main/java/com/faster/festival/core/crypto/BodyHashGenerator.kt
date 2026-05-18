package com.faster.festival.core.crypto

import java.security.MessageDigest

/**
 * SHA-256 hex digester used by the trusted-device signing pipeline.
 *
 * **Critical invariant (per spec):** the input must be the EXACT JSON byte
 * string that will be sent over HTTP — no pretty-printing, no field reorder,
 * no rebuild, no mutation after hashing. Any post-hash mutation will fail
 * server-side signature verification.
 */
object BodyHashGenerator {

    /** Lowercase hex SHA-256 of [rawJson]. */
    fun sha256Hex(rawJson: String): String =
        sha256Hex(rawJson.toByteArray(Charsets.UTF_8))

    /** Lowercase hex SHA-256 of [bytes]. */
    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.toHex()
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4])
            sb.append(HEX[v and 0x0F])
        }
        return sb.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
