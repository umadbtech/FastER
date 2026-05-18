package com.faster.festival.core.crypto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Single source of truth for the JSON serialization config used by every
 * Project 2 signed request.
 *
 * The settings are deliberately conservative — anything that changes the
 * byte string between the moment we hash and the moment OkHttp writes it on
 * the socket BREAKS server-side signature verification:
 *
 *  • `prettyPrint = false`     — no whitespace surprises
 *  • `encodeDefaults = true`   — `trigger_source` etc. ALWAYS in the body
 *  • `explicitNulls = true`    — `wristband.wristband_id: null` matches the
 *                                 spec example for mobile-only sessions
 *  • `ignoreUnknownKeys = true` — forward-compat with new server fields
 *  • field declaration order is preserved by `kotlinx.serialization` itself —
 *    no per-field reorder. Tests in `CanonicalJsonTest` lock this invariant.
 *
 * Used by [DeviceSignatureManager] for outbound signed payloads. Do NOT
 * construct a one-off `Json {}` for signing — it WILL drift and silently
 * fail signature checks on a future field add.
 */
object CanonicalJson {

    /**
     * The exact `Json` instance the signing pipeline must use. Exposed
     * `public` so internal tests can assert determinism; callers outside the
     * signing pipeline should still prefer [DeviceSignatureManager] which
     * couples serialization to hashing-and-signing in one call.
     */
    val instance: Json = Json {
        prettyPrint = false
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    /**
     * Convenience that uses reified serialization. Result is the EXACT byte
     * string (as UTF-8) that must be hashed and shipped — anything else
     * invalidates the signature.
     */
    inline fun <reified T> serialize(payload: T): String =
        instance.encodeToString(payload)
}
