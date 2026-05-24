package com.faster.festival.core.crypto

import android.util.Base64
import com.faster.festival.core.security.Ed25519KeyManager

/**
 * Builds and signs the canonical string the Project 2 SOS backend verifies.
 *
 * Canonical format (EXACT — no extra whitespace, no trailing newline):
 *
 * ```
 * POST
 * /pinch-ingest
 * device_id:<device_id>
 * nonce:<nonce>
 * timestamp:<timestamp>
 * body_sha256:<sha256_hex>
 * ```
 *
 * The canonical string proves the trusted device approved this combination of
 * `device_id` + `nonce` + `timestamp` + body hash. The Bearer JWT and
 * attestation token are NOT part of the canonical string — they ride in
 * separate headers.
 */
class CanonicalSigner(private val keyManager: Ed25519KeyManager) {

    data class Canonical(
        val canonicalString: String,
        val signatureBase64: String
    )

    /**
     * Build the canonical string for a `pinch-ingest` request and sign it.
     */
    fun signPinchIngest(
        deviceId: String,
        nonce: String,
        timestamp: String,
        bodySha256Hex: String
    ): Canonical = signForPath(
        path = "/pinch-ingest",
        deviceId = deviceId,
        nonce = nonce,
        timestamp = timestamp,
        bodySha256Hex = bodySha256Hex
    )

    /**
     * Sibling of [signPinchIngest] for the periodic location push. Same
     * canonical-string layout, different path component.
     */
    fun signPinchUpdateLocation(
        deviceId: String,
        nonce: String,
        timestamp: String,
        bodySha256Hex: String
    ): Canonical = signForPath(
        path = "/pinch-update-location",
        deviceId = deviceId,
        nonce = nonce,
        timestamp = timestamp,
        bodySha256Hex = bodySha256Hex
    )

    /**
     * Sibling for `POST /pinch-alert-details` — the signed partial-details
     * submit (phone / medical info / what-happened / manual location).
     */
    fun signPinchAlertDetails(
        deviceId: String,
        nonce: String,
        timestamp: String,
        bodySha256Hex: String
    ): Canonical = signForPath(
        path = "/pinch-alert-details",
        deviceId = deviceId,
        nonce = nonce,
        timestamp = timestamp,
        bodySha256Hex = bodySha256Hex
    )

    private fun signForPath(
        path: String,
        deviceId: String,
        nonce: String,
        timestamp: String,
        bodySha256Hex: String
    ): Canonical {
        val canonical = buildCanonicalString(
            method = "POST",
            path = path,
            deviceId = deviceId,
            nonce = nonce,
            timestamp = timestamp,
            bodySha256Hex = bodySha256Hex
        )
        val signature = keyManager.sign(canonical.toByteArray(Charsets.UTF_8))
        val b64 = Base64.encodeToString(signature, Base64.NO_WRAP)
        return Canonical(canonical, b64)
    }

    /**
     * Public for unit testing — verifies the EXACT canonical-string contract.
     */
    fun buildCanonicalString(
        method: String,
        path: String,
        deviceId: String,
        nonce: String,
        timestamp: String,
        bodySha256Hex: String
    ): String = buildString {
        append(method); append('\n')
        append(path); append('\n')
        append("device_id:").append(deviceId); append('\n')
        append("nonce:").append(nonce); append('\n')
        append("timestamp:").append(timestamp); append('\n')
        append("body_sha256:").append(bodySha256Hex)
        // No trailing newline.
    }
}
