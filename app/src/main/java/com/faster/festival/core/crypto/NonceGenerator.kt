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
}
