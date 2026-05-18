package com.faster.festival.data.sos.remote

/**
 * Project 2 returned HTTP 403 with `{"error":"Device attestation is expired"}`
 * (or a semantically equivalent backend code). The trusted-device attestation
 * has crossed its server-side TTL and `sos-verify-attestation` must be re-run
 * before the next `pinch-ingest` / `pinch-update-location`.
 *
 * Thrown by [SosRemoteDataSource] when it parses a 403 with an attestation
 * marker in the error body, so the upstream repository can branch
 * specifically on this case (auto-reattest + retry) instead of treating it
 * as a generic 403.
 *
 * The `cause` is preserved as the original `retrofit2.HttpException` so any
 * downstream classifier that wants the HTTP code can still reach it via
 * `cause.code()`.
 */
class AttestationExpiredException(
    val backendMessage: String,
    cause: Throwable
) : RuntimeException("Device attestation expired: $backendMessage", cause)
