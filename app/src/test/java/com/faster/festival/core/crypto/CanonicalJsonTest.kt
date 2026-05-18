package com.faster.festival.core.crypto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the serialization invariants the SOS signature pipeline depends on.
 *
 * If any of these tests starts failing, the bytes we hash will no longer
 * byte-match the bytes that go over the wire, and the server will reject
 * every signed request with `signature_mismatch`. None of these tests are
 * about "nice JSON" — they are about server compatibility.
 */
class CanonicalJsonTest {

    @Serializable
    private data class Sample(
        @SerialName("a_first") val aFirst: String,
        @SerialName("b_second") val bSecond: Int,
        @SerialName("c_optional") val cOptional: String? = null,
        @SerialName("d_default") val dDefault: String = "default-value"
    )

    @Test
    fun `serialization is byte-deterministic`() {
        val sample = Sample(aFirst = "x", bSecond = 7, cOptional = "y")
        val first = CanonicalJson.serialize(sample)
        val second = CanonicalJson.serialize(sample)
        val third = CanonicalJson.serialize(sample)
        assertEquals(first, second)
        assertEquals(second, third)
    }

    @Test
    fun `serialization preserves field declaration order`() {
        // The spec requires the wire body to be deterministic; kotlinx.serialization
        // honors declaration order by default. If anyone flips a Json{} flag that
        // changes this, signing will silently break.
        val out = CanonicalJson.serialize(Sample(aFirst = "x", bSecond = 1))
        val idxA = out.indexOf("\"a_first\"")
        val idxB = out.indexOf("\"b_second\"")
        val idxC = out.indexOf("\"c_optional\"")
        val idxD = out.indexOf("\"d_default\"")
        assertTrue(idxA in 0..idxB, "a must precede b: $out")
        assertTrue(idxB in 0..idxC, "b must precede c: $out")
        assertTrue(idxC in 0..idxD, "c must precede d: $out")
    }

    @Test
    fun `null-valued optional fields are emitted explicitly`() {
        // explicitNulls=true — required because the spec example for a
        // mobile-only SOS sends `wristband.wristband_id: null`. If
        // explicitNulls flips to false, the body shape silently changes
        // and the SHA-256 mismatches.
        val out = CanonicalJson.serialize(Sample(aFirst = "x", bSecond = 1, cOptional = null))
        assertTrue("\"c_optional\":null" in out,
            "explicitNulls must emit null fields: $out")
    }

    @Test
    fun `default-valued fields are emitted`() {
        // encodeDefaults=true — keeps `trigger_source: "mobile_ui"` etc. in
        // every payload regardless of whether the caller set it explicitly.
        val out = CanonicalJson.serialize(Sample(aFirst = "x", bSecond = 1))
        assertTrue("\"d_default\":\"default-value\"" in out,
            "encodeDefaults must include defaulted fields: $out")
    }

    @Test
    fun `output has no pretty-print whitespace`() {
        val out = CanonicalJson.serialize(Sample(aFirst = "x", bSecond = 1))
        // No '\n', no spaces around colons or commas — the most compact form.
        assertTrue('\n' !in out, "Found newline in: $out")
        assertTrue("\": " !in out, "Found space after colon in: $out")
        assertTrue(", " !in out, "Found space after comma in: $out")
    }

    @Test
    fun `equal inputs produce equal UTF-8 byte arrays`() {
        // The hash is computed over the UTF-8 bytes of the serialized string;
        // verifying byte-level equality is the strongest possible determinism check.
        val s = Sample(aFirst = "café", bSecond = 7, cOptional = "δ")
        val bytesA = CanonicalJson.serialize(s).toByteArray(Charsets.UTF_8)
        val bytesB = CanonicalJson.serialize(s).toByteArray(Charsets.UTF_8)
        assertTrue(bytesA.contentEquals(bytesB))
    }
}
