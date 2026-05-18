package com.faster.festival.data.sos

import com.faster.festival.BuildConfig

/**
 * Pluggable attestation provider — extracts the part of the trusted-device
 * setup that varies between staging (test attestation) and production
 * (Play Integrity / App Attest).
 */
interface AttestationProvider {
    val provider: String

    /**
     * Returns the raw attestation token to ship to `sos-verify-attestation`.
     * The token is bound to the device public key server-side; production
     * implementations should derive a challenge from
     * `hash(user_id + device_id + device_public_key + issued_at)`.
     */
    suspend fun obtainToken(deviceId: String, devicePublicKeyB64: String): Result<String>
}

/**
 * Staging-only — the backend accepts `provider="test"` + `attestation_token`
 * starting with `test:` when `SOS_ALLOW_TEST_ATTESTATION=true` is set.
 *
 * Guards itself against accidental release-build use: throws unless the
 * server-side flag is on. We can only inspect the build-time mirror flag, but
 * if the env value is `false` we refuse.
 */
class TestAttestationProvider : AttestationProvider {
    override val provider: String = "test"

    override suspend fun obtainToken(
        deviceId: String,
        devicePublicKeyB64: String
    ): Result<String> {
        if (!BuildConfig.SOS_ALLOW_TEST_ATTESTATION) {
            return Result.failure(
                IllegalStateException(
                    "Test attestation is disabled (SOS_ALLOW_TEST_ATTESTATION=false). " +
                            "Use a real Play Integrity provider in production."
                )
            )
        }
        // Spec: token format `test:<anything>`. We tag with a stable per-device
        // suffix so backend-side debug logs can distinguish APK/staging
        // installs without exposing user-identifying data.
        val tag = deviceId.take(8).ifEmpty { "android-device" }
        return Result.success("test:$tag")
    }
}

/**
 * Production placeholder — Phase B work. When ready:
 *  • Add `com.google.android.play:integrity:1.x` dependency.
 *  • In `obtainToken`, request a Play Integrity token bound to a server-issued
 *    challenge derived from `hash(user_id + device_id + device_public_key + issued_at)`.
 *  • Send `provider = "play_integrity"` so the backend routes verification to
 *    Google's attestation API.
 */
class FuturePlayIntegrityProvider : AttestationProvider {
    override val provider: String = "play_integrity"

    override suspend fun obtainToken(
        deviceId: String,
        devicePublicKeyB64: String
    ): Result<String> = Result.failure(
        NotImplementedError(
            "Play Integrity provider not yet wired. See AttestationProvider.kt for steps."
        )
    )
}
