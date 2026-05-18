package com.faster.festival.core.crypto

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Pinch SOS spec (`Pinch_SOS_Frontend_Implementation_Guide.md`) requires the
 * `x-device-body-sha256` header to be the lowercase hex SHA-256 of the EXACT
 * JSON byte string that goes over the wire. Any deviation breaks the
 * server-side signature check.
 *
 * Test vectors are well-known SHA-256s of canonical inputs.
 */
class BodyHashGeneratorTest {

    @Test
    fun `empty string sha256`() {
        // Known: SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            BodyHashGenerator.sha256Hex("")
        )
    }

    @Test
    fun `abc sha256`() {
        // Known: SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            BodyHashGenerator.sha256Hex("abc")
        )
    }

    @Test
    fun `output is lowercase hex`() {
        val out = BodyHashGenerator.sha256Hex("Hello FASTER")
        assertEquals(64, out.length, "SHA-256 hex must be 64 chars")
        assertEquals(out.lowercase(), out, "Hex output must be lowercase per spec")
        out.forEach { c ->
            assert(c in '0'..'9' || c in 'a'..'f') { "Non-hex char in output: $c" }
        }
    }

    @Test
    fun `bytes overload matches string overload`() {
        val s = """{"client_trigger_id":"mobile-ui-x","nonce":"n"}"""
        assertEquals(
            BodyHashGenerator.sha256Hex(s),
            BodyHashGenerator.sha256Hex(s.toByteArray(Charsets.UTF_8))
        )
    }

    @Test
    fun `field reorder produces different hash`() {
        // Spec invariant: any post-hash mutation invalidates the signature.
        // Reordering fields changes the byte string → different hash.
        val a = """{"a":1,"b":2}"""
        val b = """{"b":2,"a":1}"""
        assert(BodyHashGenerator.sha256Hex(a) != BodyHashGenerator.sha256Hex(b))
    }
}
