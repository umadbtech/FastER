package com.faster.festival.data.sos.remote

/**
 * Project 2 returned HTTP 403 with `{"error":"Active SOS device registration is
 * required"}` (or a semantically equivalent backend code). The locally cached
 * `device_id` is no longer recognized as an active, attested device by the
 * backend — typically because the registration was purged/expired server-side
 * or the local identity is stale relative to the current backend.
 *
 * Unlike [AttestationExpiredException] (where the device IS known and only the
 * attestation TTL lapsed), recovery here requires a FULL re-registration:
 * drop the stale local identity and re-run `sos-register-device` +
 * `sos-verify-attestation` before retrying `pinch-ingest`.
 *
 * Thrown by [SosRemoteDataSource]; [com.faster.festival.data.sos.SosRepositoryImpl]
 * branches on it to auto re-register + retry once. The original
 * `retrofit2.HttpException` is preserved as `cause`.
 */
class DeviceRegistrationRequiredException(
    val backendMessage: String,
    cause: Throwable
) : RuntimeException("SOS device registration required: $backendMessage", cause)
