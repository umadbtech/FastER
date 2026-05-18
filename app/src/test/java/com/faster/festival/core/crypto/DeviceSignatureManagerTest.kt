package com.faster.festival.core.crypto

import com.faster.festival.core.security.Ed25519KeyManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.junit.Test
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * End-to-end coverage of the signing facade:
 *
 *   typed payload → CanonicalJson serialize → SHA-256 → canonical-string →
 *     Ed25519 sign → SignedRequest
 *
 * Strategy: stub [Ed25519KeyManager.sign] with a real BouncyCastle Ed25519
 * sign over the input bytes, then verify the resulting signature with
 * BouncyCastle's `Ed25519Signer.verifySignature` against the same canonical
 * string. This proves:
 *
 *   (a) `rawJson` is what gets serialized (caller hands back a payload, we
 *       get bytes we can re-hash and match).
 *   (b) `bodySha256Hex` is SHA-256 of EXACTLY those bytes.
 *   (c) The signature was produced over the canonical string that includes
 *       that body hash — no field re-ordering, no double-serialize.
 *
 * This is the test the production invariant — "Signing MUST occur AFTER
 * final JSON serialization" — relies on.
 */
class DeviceSignatureManagerTest {

    @Serializable
    private data class TestPayload(
        @SerialName("client_trigger_id") val clientTriggerId: String,
        val nonce: String,
        @SerialName("wristband_id") val wristbandId: String? = null,
        val battery: Int = 73
    )

    // ─── Deterministic test keypair ─────────────────────────────────────────
    private val seed = ByteArray(32) { it.toByte() }    // [0,1,2,...,31]
    private val privateKey = Ed25519PrivateKeyParameters(seed, 0)
    private val publicKey: Ed25519PublicKeyParameters = privateKey.generatePublicKey()

    /**
     * Real Ed25519 sign over arbitrary bytes — what production
     * [Ed25519KeyManager.sign] does, but with a deterministic key for
     * verification.
     */
    private fun bcSign(message: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, privateKey)
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    private fun newManager(): DeviceSignatureManager {
        val keyManager = mockk<Ed25519KeyManager>()
        every { keyManager.sign(any()) } answers { bcSign(firstArg()) }
        val canonical = CanonicalSigner(keyManager)
        return DeviceSignatureManager(canonical)
    }

    @Test
    fun `signPinchIngest produces rawJson whose sha256 matches the header`() {
        val payload = TestPayload(
            clientTriggerId = "mobile-ui-abc",
            nonce = "test-nonce",
            wristbandId = null,
            battery = 82
        )
        val signed = newManager().signPinchIngest(
            deviceId = "dev-test",
            nonce = "nonce-1",
            timestamp = "2026-05-09T14:32:11.482Z",
            payload = payload
        )
        // (a) rawJson is byte-deterministic and matches the canonical serializer.
        val expectedJson = CanonicalJson.serialize(payload)
        assertEquals(expectedJson, signed.rawJson)
        // (b) bodySha256Hex is SHA-256 over those exact bytes.
        val recomputed = BodyHashGenerator.sha256Hex(signed.rawJson)
        assertEquals(recomputed, signed.bodySha256Hex)
        // (c) Algorithm label is locked.
        assertEquals("ed25519", signed.signatureAlg)
    }

    @Test
    fun `signature verifies against the canonical string for pinch-ingest`() {
        val payload = TestPayload(
            clientTriggerId = "mobile-ui-xyz",
            nonce = "n",
            wristbandId = "FSTR-0003",
            battery = 50
        )
        val signed = newManager().signPinchIngest(
            deviceId = "dev-x",
            nonce = "nonce-2",
            timestamp = "2026-05-10T00:00:00Z",
            payload = payload
        )
        // Reconstruct the canonical string EXACTLY as CanonicalSigner would.
        val canonical = buildString {
            append("POST"); append('\n')
            append("/pinch-ingest"); append('\n')
            append("device_id:dev-x"); append('\n')
            append("nonce:nonce-2"); append('\n')
            append("timestamp:2026-05-10T00:00:00Z"); append('\n')
            append("body_sha256:"); append(signed.bodySha256Hex)
        }
        val signatureBytes = Base64.getDecoder().decode(signed.signatureBase64)
        val verifier = Ed25519Signer().apply {
            init(false, publicKey)
            update(canonical.toByteArray(Charsets.UTF_8), 0, canonical.toByteArray().size)
        }
        assertTrue(verifier.verifySignature(signatureBytes),
            "Ed25519 signature did NOT verify against canonical string: $canonical")
    }

    @Test
    fun `signPinchUpdateLocation uses the update-location path in the canonical string`() {
        val payload = TestPayload(clientTriggerId = "mobile-ui-q", nonce = "n2")
        val signed = newManager().signPinchUpdateLocation(
            deviceId = "dev-u",
            nonce = "nonce-3",
            timestamp = "2026-05-10T00:00:01Z",
            payload = payload
        )
        val expectedCanonical = buildString {
            append("POST"); append('\n')
            append("/pinch-update-location"); append('\n')
            append("device_id:dev-u"); append('\n')
            append("nonce:nonce-3"); append('\n')
            append("timestamp:2026-05-10T00:00:01Z"); append('\n')
            append("body_sha256:"); append(signed.bodySha256Hex)
        }
        val signatureBytes = Base64.getDecoder().decode(signed.signatureBase64)
        val verifier = Ed25519Signer().apply {
            init(false, publicKey)
            update(
                expectedCanonical.toByteArray(Charsets.UTF_8),
                0,
                expectedCanonical.toByteArray().size
            )
        }
        assertTrue(verifier.verifySignature(signatureBytes),
            "update-location signature must verify against /pinch-update-location canonical")
    }

    @Test
    fun `same inputs produce same signature (deterministic Ed25519)`() {
        val payload = TestPayload(clientTriggerId = "mobile-ui-d", nonce = "n3")
        val mgr = newManager()
        val a = mgr.signPinchIngest("d", "n-fixed", "t-fixed", payload)
        val b = mgr.signPinchIngest("d", "n-fixed", "t-fixed", payload)
        // Ed25519 is deterministic; same key + same message ⇒ same signature.
        assertEquals(a.signatureBase64, b.signatureBase64)
        assertEquals(a.bodySha256Hex, b.bodySha256Hex)
        assertEquals(a.rawJson, b.rawJson)
    }

    @Test
    fun `changing one payload byte changes the signature`() {
        val mgr = newManager()
        val a = mgr.signPinchIngest(
            "d", "n", "t",
            TestPayload(clientTriggerId = "mobile-ui-A", nonce = "n")
        )
        val b = mgr.signPinchIngest(
            "d", "n", "t",
            TestPayload(clientTriggerId = "mobile-ui-B", nonce = "n")
        )
        assertNotEquals(a.bodySha256Hex, b.bodySha256Hex,
            "Different payloads must produce different body hashes")
        assertNotEquals(a.signatureBase64, b.signatureBase64,
            "Different body hashes must produce different signatures")
    }

    @Test
    fun `header constants match the Project 2 wire contract`() {
        assertEquals("x-device-signature", DeviceSignatureManager.HEADER_SIGNATURE)
        assertEquals("x-device-signature-alg", DeviceSignatureManager.HEADER_SIGNATURE_ALG)
        assertEquals("x-device-body-sha256", DeviceSignatureManager.HEADER_BODY_SHA256)
        assertEquals("ed25519", DeviceSignatureManager.SIGNATURE_ALG)
    }
}
