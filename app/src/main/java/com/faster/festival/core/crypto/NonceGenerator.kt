package com.faster.festival.core.crypto

import java.util.UUID

/**
 * Generates a fresh nonce for every signed SOS HTTP request — including each
 * retry within a single SOS tap (per Pinch SOS spec).
 *
 *  • `client_trigger_id` is generated ONCE per SOS tap (see [newClientTriggerId])
 *    and reused across retries.
 *  • `nonce` is regenerated on every HTTP request, including retries.
 */
object NonceGenerator {

    /** Fresh per-request nonce (UUID v4). */
    fun newNonce(): String = UUID.randomUUID().toString()

    /** Per-tap client_trigger_id, prefixed `mobile-ui-` per spec. */
    fun newClientTriggerId(): String = "mobile-ui-${UUID.randomUUID()}"

    /**
     * Idempotency key for ONE `pinch-alert-details` submit (phone / medical /
     * incident / location). The SAME id MUST be reused if that exact submit is
     * retried, so the backend can dedup — callers generate it once per user
     * action and hold it for the retry window. [kind] is a short tag
     * ("phone" / "medical" / "incident" / "location") echoed in logs.
     */
    fun newClientUpdateId(kind: String): String = "$kind-${UUID.randomUUID()}"

    /** Idempotency key for ONE `pinch-cancel` request, reused across retries. */
    fun newCancelRequestId(): String = "cancel-${UUID.randomUUID()}"
}
