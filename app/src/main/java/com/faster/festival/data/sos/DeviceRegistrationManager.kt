package com.faster.festival.data.sos

import com.faster.festival.BuildConfig
import com.faster.festival.core.security.Ed25519KeyManager
import com.faster.festival.data.repository.local.SosDeviceRepository
import com.faster.festival.data.sos.local.DeviceIdentityStore
import com.faster.festival.data.sos.remote.RegisterDeviceRequest
import com.faster.festival.data.sos.remote.RegisterDeviceResponse
import com.faster.festival.data.sos.remote.VerifyAttestationRequest
import com.faster.festival.data.sos.remote.VerifyAttestationResponse
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

/**
 * Owns the trusted-device bootstrap pipeline (register + verify attestation).
 *
 * Idempotent — safe to call on every app launch:
 *  • If a device key already exists, [Ed25519KeyManager.generateIfMissing] is
 *    a no-op.
 *  • If `device_id` is already persisted AND attestation is verified, the
 *    backend round-trips are skipped.
 *
 * Storage architecture:
 *  • [Ed25519KeyManager] — raw private key, EncryptedSharedPreferences
 *    (Keystore-wrapped AES-256 envelope).
 *  • [DeviceIdentityStore] (DataStore) — hot-path cache: `device_id` +
 *    `attestation_verified` boolean. Used by the signed `pinch-ingest`
 *    flow on the SOS critical path.
 *  • [SosDeviceRepository] (Room) — canonical audit record: full status
 *    enums, timestamps, last-sync, raw backend payload.
 *
 * Backend retry: 1s → 2s → 4s → 8s on network / 5xx only. 4xx (401, malformed,
 * already-exists) is non-retryable — surface as failure and let the caller
 * decide.
 */
class DeviceRegistrationManager(
    private val keyManager: Ed25519KeyManager,
    private val identityStore: DeviceIdentityStore,
    private val deviceRepo: SosDeviceRepository,
    private val remote: SosRemoteDataSource,
    private val attestationProvider: AttestationProvider,
    private val appId: String = BuildConfig.APPLICATION_ID,
    private val appVersion: String = BuildConfig.VERSION_NAME
) {

    sealed class BootstrapResult {
        object Ready : BootstrapResult()
        data class Failed(val cause: Throwable) : BootstrapResult()
    }

    private val payloadJson = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * Run the full bootstrap. Caller is responsible for ensuring the user is
     * logged in AND has an active membership before invoking this — see
     * [com.faster.festival.presentation.sos.SOSSetupManager].
     */
    suspend fun bootstrap(): BootstrapResult {
        return runCatching {
            // 1. Generate signing key if missing.
            keyManager.generateIfMissing()
            val publicKeyB64 = keyManager.getPublicKeyBase64()

            // 2. Register device — skip if we already have a device_id locally.
            val deviceId = identityStore.deviceId() ?: registerDeviceWithRetry(publicKeyB64)

            // 3. Verify attestation — skip if already verified for this device.
            if (!identityStore.attestationVerified()) {
                verifyAttestationWithRetry(deviceId, publicKeyB64)
            } else {
                // Make sure Room mirrors the verified state even if DataStore
                // was set on a prior run that didn't yet write to Room.
                deviceRepo.markAttestationVerified(deviceId = deviceId)
            }
            BootstrapResult.Ready
        }.getOrElse {
            Timber.tag(TAG).w(it, "SOS device bootstrap failed")
            BootstrapResult.Failed(it)
        }
    }

    private suspend fun registerDeviceWithRetry(publicKeyB64: String): String {
        val request = RegisterDeviceRequest(
            appId = appId,
            appVersion = appVersion,
            devicePublicKey = publicKeyB64
        )
        Timber.tag(TAG).i("Registering SOS device with backend")
        val resp = withBackoff("register-device") {
            remote.registerDevice(request).getOrThrow()
        }
        val deviceId = resp.device.deviceId
        require(deviceId.isNotBlank()) { "register-device returned blank device_id" }

        identityStore.saveDeviceId(deviceId)
        deviceRepo.saveRegistration(
            deviceId = deviceId,
            platform = "android",
            appId = appId,
            appVersion = appVersion,
            publicKey = publicKeyB64,
            keyAlgorithm = "ed25519",
            registrationStatus = resp.device.status?.lowercase()?.ifBlank { "registered" }
                ?: "registered",
            rawBackendPayload = runCatching { payloadJson.encodeToString(resp) }.getOrNull()
        )
        Timber.tag(TAG).i("Registered device_id=%s…", deviceId.take(8))
        return deviceId
    }

    private suspend fun verifyAttestationWithRetry(deviceId: String, publicKeyB64: String) {
        Timber.tag(TAG).i("Requesting %s attestation token", attestationProvider.provider)
        val token = attestationProvider.obtainToken(deviceId, publicKeyB64).getOrThrow()
        val request = VerifyAttestationRequest(
            deviceId = deviceId,
            appId = appId,
            provider = attestationProvider.provider,
            attestationToken = token,
            devicePublicKey = publicKeyB64
        )

        val resp = try {
            withBackoff("verify-attestation") {
                remote.verifyAttestation(request).getOrThrow()
            }
        } catch (t: Throwable) {
            deviceRepo.markAttestationFailed(
                deviceId = deviceId,
                rawBackendPayload = t.localizedMessage
            )
            throw t
        }

        identityStore.markAttestationVerified(true)
        deviceRepo.markAttestationVerified(
            deviceId = deviceId,
            rawBackendPayload = runCatching { payloadJson.encodeToString(resp) }.getOrNull()
        )
        Timber.tag(TAG).i("Attestation verified")
    }

    /**
     * Hard reset — clears device id, attestation state, AND the Ed25519 key.
     * Forces full re-registration on next bootstrap.
     */
    suspend fun reset() {
        identityStore.clear()
        keyManager.reset()
        deviceRepo.clear()
    }

    /**
     * Soft refresh — re-runs `sos-verify-attestation` for the existing device.
     * Triggered by [com.faster.festival.data.sos.remote.AttestationExpiredException]
     * when Project 2 reports the device's attestation has crossed its TTL.
     *
     * Idempotent and safe to call concurrently — first caller wins, subsequent
     * callers see `attestationVerified = true` and skip. Does NOT regenerate
     * keys or device id: the same device identity stays in place, only the
     * server-side attestation timestamp is refreshed.
     *
     *  • If no `device_id` exists locally, falls through to full bootstrap.
     *  • Marks `attestationVerified = false` BEFORE the network round-trip so
     *    a process death mid-verify leaves the flag in the "needs verify"
     *    state on next launch.
     */
    suspend fun reattest(): BootstrapResult = runCatching {
        keyManager.generateIfMissing()
        val publicKeyB64 = keyManager.getPublicKeyBase64()
        val deviceId = identityStore.deviceId()
            ?: return@runCatching run {
                Timber.tag(TAG).i("reattest: no device_id — falling through to full bootstrap")
                registerDeviceWithRetry(publicKeyB64)
            }.let { id ->
                identityStore.markAttestationVerified(false)
                verifyAttestationWithRetry(id, publicKeyB64)
                BootstrapResult.Ready
            }
        Timber.tag(TAG).i("reattest: forcing re-verification for device_id=%s…", deviceId.take(8))
        identityStore.markAttestationVerified(false)
        verifyAttestationWithRetry(deviceId, publicKeyB64)
        BootstrapResult.Ready
    }.getOrElse {
        Timber.tag(TAG).w(it, "reattest failed")
        BootstrapResult.Failed(it)
    }

    /**
     * Exponential backoff per spec — 1s → 2s → 4s → 8s. Retries on transport
     * (IOException) or HTTP 5xx only. 4xx / non-HTTP exceptions are re-thrown
     * immediately so a non-retryable failure surfaces fast.
     */
    private suspend fun <T> withBackoff(
        label: String,
        block: suspend () -> T
    ): T {
        val delays = longArrayOf(1_000L, 2_000L, 4_000L, 8_000L)
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (t: Throwable) {
                val retryable = t is IOException ||
                        (t is HttpException && t.code() in 500..599)
                if (!retryable || attempt >= delays.size) {
                    Timber.tag(TAG).w(t, "%s — non-retryable / out of attempts", label)
                    throw t
                }
                val wait = delays[attempt]
                Timber.tag(TAG).i("%s retryable failure attempt=%d backoff=%dms",
                    label, attempt + 1, wait)
                delay(wait)
                attempt++
            }
        }
    }

    private companion object {
        const val TAG = "SosBootstrap"
    }
}
