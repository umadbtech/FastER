package android.util

/**
 * Minimal JVM stub of `android.util.Base64` so unit tests covering the signing
 * pipeline (which calls `Base64.encodeToString(signature, Base64.NO_WRAP)` via
 * [com.faster.festival.core.crypto.CanonicalSigner]) can link on the host JVM
 * without spinning up Robolectric.
 *
 * Implements `NO_WRAP` and `DEFAULT` against `java.util.Base64` — the only
 * variants the production signing path uses. Other flag bits (`URL_SAFE`,
 * `CRLF`, `NO_PADDING`, `NO_CLOSE`) are not implemented because no
 * production call site uses them; if a future test needs them, add here.
 */
object Base64 {

    const val DEFAULT = 0
    const val NO_PADDING = 1
    const val NO_WRAP = 2
    const val CRLF = 4
    const val URL_SAFE = 8

    @JvmStatic
    fun encodeToString(input: ByteArray, flags: Int): String {
        val encoder = if (hasFlag(flags, NO_WRAP)) {
            java.util.Base64.getEncoder().withoutPadding().let {
                // NO_WRAP suppresses MIME line wrapping; java.util.Base64 base
                // encoder doesn't wrap, so this is effectively the same.
                java.util.Base64.getEncoder()
            }
        } else {
            // DEFAULT — java.util.Base64 doesn't insert MIME wrapping either,
            // so the same encoder is correct (close enough for unit tests).
            java.util.Base64.getEncoder()
        }
        return encoder.encodeToString(input)
    }

    @JvmStatic
    fun decode(input: String, flags: Int): ByteArray =
        java.util.Base64.getDecoder().decode(input)

    @JvmStatic
    fun decode(input: ByteArray, flags: Int): ByteArray =
        java.util.Base64.getDecoder().decode(input)

    private fun hasFlag(flags: Int, mask: Int) = flags and mask == mask
}
