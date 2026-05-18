package com.faster.festival.core.crypto

/**
 * Couples canonical JSON serialization + SHA-256 body hashing + Ed25519
 * signing into ONE atomic call.
 *
 * The Pinch SOS spec has one critical invariant:
 *
 *   > **Signing MUST occur AFTER final JSON serialization.**
 *
 * It is impossible to violate that invariant by construction when callers
 * go through this manager — there is no way to obtain a [SignedRequest]
 * other than by handing the manager a typed payload and letting it produce
 * the rawJson + hash + signature as a single atomic unit. Pre-existing code
 * paths that hand-rolled the three steps in sequence
 * ([com.faster.festival.data.sos.SosRepositoryImpl] before the refactor) are
 * migrated to call this manager instead.
 *
 * **Project 2 only.** The three headers the manager produces —
 * `x-device-signature`, `x-device-signature-alg`, `x-device-body-sha256` —
 * are accepted ONLY by Project 2 signed endpoints (`pinch-ingest`,
 * `pinch-update-location`). Project 1 endpoints don't see them.
 *
 * Reuses the existing [CanonicalSigner] (canonical-string format), the
 * existing [BodyHashGenerator] (SHA-256 hex), the existing [CanonicalJson]
 * (deterministic JSON), and the existing
 * [com.faster.festival.core.security.Ed25519KeyManager] (signing key) —
 * no parallel implementations.
 */
class DeviceSignatureManager(
    private val canonicalSigner: CanonicalSigner
) {

    /**
     * The four pieces a Project 2 signed request needs.
     *
     * `rawJson` is the EXACT byte string (UTF-8) to put in the HTTP body —
     * Retrofit must NOT re-serialize from a DTO, because that re-serialization
     * would not byte-match the bytes we hashed.
     */
    data class SignedRequest(
        val rawJson: String,
        val bodySha256Hex: String,
        val signatureBase64: String,
        val signatureAlg: String
    )

    /**
     * Build a [SignedRequest] for `POST /pinch-ingest`. See class kdoc for the
     * serialize-then-hash-then-sign invariant.
     */
    inline fun <reified T> signPinchIngest(
        deviceId: String,
        nonce: String,
        timestamp: String,
        payload: T
    ): SignedRequest = signFor(
        path = "/pinch-ingest",
        deviceId = deviceId,
        nonce = nonce,
        timestamp = timestamp,
        payload = payload
    )

    /** Sibling for `POST /pinch-update-location`. */
    inline fun <reified T> signPinchUpdateLocation(
        deviceId: String,
        nonce: String,
        timestamp: String,
        payload: T
    ): SignedRequest = signFor(
        path = "/pinch-update-location",
        deviceId = deviceId,
        nonce = nonce,
        timestamp = timestamp,
        payload = payload
    )

    @PublishedApi
    internal inline fun <reified T> signFor(
        path: String,
        deviceId: String,
        nonce: String,
        timestamp: String,
        payload: T
    ): SignedRequest {
        // Step 1: serialize ONCE — these bytes are what go over HTTP.
        val rawJson = CanonicalJson.serialize(payload)
        // Step 2: hash those exact bytes.
        val bodySha256Hex = BodyHashGenerator.sha256Hex(rawJson)
        // Step 3: sign the canonical string built AROUND that hash.
        val signed = signCanonical(path, deviceId, nonce, timestamp, bodySha256Hex)
        return SignedRequest(
            rawJson = rawJson,
            bodySha256Hex = bodySha256Hex,
            signatureBase64 = signed.signatureBase64,
            signatureAlg = SIGNATURE_ALG
        )
    }

    @PublishedApi
    internal fun signCanonical(
        path: String,
        deviceId: String,
        nonce: String,
        timestamp: String,
        bodySha256Hex: String
    ): CanonicalSigner.Canonical = when (path) {
        "/pinch-ingest" -> canonicalSigner.signPinchIngest(
            deviceId, nonce, timestamp, bodySha256Hex
        )
        "/pinch-update-location" -> canonicalSigner.signPinchUpdateLocation(
            deviceId, nonce, timestamp, bodySha256Hex
        )
        else -> error("Unsupported path for DeviceSignatureManager: $path")
    }

    companion object {
        const val SIGNATURE_ALG = "ed25519"
        const val HEADER_SIGNATURE = "x-device-signature"
        const val HEADER_SIGNATURE_ALG = "x-device-signature-alg"
        const val HEADER_BODY_SHA256 = "x-device-body-sha256"
    }
}
