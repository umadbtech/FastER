package com.faster.festival.core.crypto

import com.faster.festival.core.security.Ed25519KeyManager
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

/**
 * The Pinch SOS canonical-string contract is byte-exact:
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
 * No trailing newline. No extra whitespace. Field order is fixed. Any
 * deviation breaks server-side signature verification.
 *
 * The actual Ed25519 sign is integration-tested manually against staging —
 * here we validate the EXACT canonical-string format because that's what
 * silently breaks if someone reorders fields or drops a colon.
 */
class CanonicalSignerTest {

    private val keyManager = mockk<Ed25519KeyManager>(relaxed = true)
    private val signer = CanonicalSigner(keyManager)

    @Test
    fun `canonical string has six lines no trailing newline`() {
        val canonical = signer.buildCanonicalString(
            method = "POST",
            path = "/pinch-ingest",
            deviceId = "dev-123",
            nonce = "abc-nonce",
            timestamp = "2026-05-09T14:32:11.482Z",
            bodySha256Hex = "deadbeef00"
        )
        val expected = listOf(
            "POST",
            "/pinch-ingest",
            "device_id:dev-123",
            "nonce:abc-nonce",
            "timestamp:2026-05-09T14:32:11.482Z",
            "body_sha256:deadbeef00"
        ).joinToString("\n")  // joinToString doesn't add a trailing separator
        assertEquals(expected, canonical)
        // Defensive — explicitly check no trailing newline.
        assert(!canonical.endsWith("\n")) {
            "Canonical string must NOT end with a newline"
        }
    }

    @Test
    fun `canonical string is sensitive to colon spacing`() {
        // No spaces around colons per spec. Build it the right way; any extra
        // space would change the bytes and break the signature.
        val canonical = signer.buildCanonicalString(
            method = "POST",
            path = "/pinch-ingest",
            deviceId = "x",
            nonce = "y",
            timestamp = "t",
            bodySha256Hex = "z"
        )
        assert(canonical.contains("device_id:x")) { "device_id colon prefix missing" }
        assert(!canonical.contains("device_id : x")) { "Found illegal whitespace around colon" }
        assert(!canonical.contains("device_id: x")) { "Found illegal space after colon" }
    }

    @Test
    fun `field order matches spec`() {
        val canonical = signer.buildCanonicalString(
            method = "POST",
            path = "/pinch-ingest",
            deviceId = "a",
            nonce = "b",
            timestamp = "c",
            bodySha256Hex = "d"
        )
        val lines = canonical.split("\n")
        assertEquals("POST", lines[0])
        assertEquals("/pinch-ingest", lines[1])
        assertEquals("device_id:a", lines[2])
        assertEquals("nonce:b", lines[3])
        assertEquals("timestamp:c", lines[4])
        assertEquals("body_sha256:d", lines[5])
        assertEquals(6, lines.size, "Canonical string must be exactly 6 lines")
    }
}
