package com.faster.festival.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import java.security.SecureRandom

/**
 * Trusted-device Ed25519 key manager for signed SOS requests.
 *
 *  • Keys are generated on-device and never leave the phone — only the public
 *    key is exposed via [getPublicKeyBase64], formatted as base64-encoded raw
 *    32 bytes (per Pinch SOS Frontend Implementation Guide).
 *  • Private key bytes are persisted under [EncryptedSharedPreferences] backed
 *    by [MasterKey] (matches the existing `EncryptedSessionManager` pattern;
 *    no custom secure-element work needed for `minSdk = 24`).
 *  • Android Keystore would be ideal but added Ed25519 only on API 33+; this
 *    manager keeps cross-version parity and we re-evaluate when minSdk ≥ 33.
 *
 * Thread-safety: backed by a single SharedPreferences instance; reads/writes
 * are atomic at the preference-key level. Signing reconstructs the parameters
 * on each call — the in-memory key material lives only for the signing
 * operation and is dereferenced afterwards.
 */
class Ed25519KeyManager(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Generate a new Ed25519 key pair on first run. No-op if a key already
     * exists — explicitly designed to be idempotent so callers can run it
     * unconditionally during SOS bootstrap.
     */
    fun generateIfMissing() {
        if (hasKey()) return
        val privateKey = Ed25519PrivateKeyParameters(SecureRandom())
        val privateBytes = privateKey.encoded
        val publicBytes = privateKey.generatePublicKey().encoded
        prefs.edit()
            .putString(KEY_PRIVATE_B64, Base64.encodeToString(privateBytes, Base64.NO_WRAP))
            .putString(KEY_PUBLIC_B64, Base64.encodeToString(publicBytes, Base64.NO_WRAP))
            .apply()
    }

    fun hasKey(): Boolean =
        prefs.contains(KEY_PRIVATE_B64) && prefs.contains(KEY_PUBLIC_B64)

    /**
     * Base64 raw-32-byte Ed25519 public key (NO_WRAP, no padding lines).
     * The exact format the SOS backend expects in `device_public_key` body
     * fields for `sos-register-device` and `sos-verify-attestation`.
     */
    fun getPublicKeyBase64(): String {
        val pub = prefs.getString(KEY_PUBLIC_B64, null)
            ?: error("Ed25519 key not generated; call generateIfMissing() first")
        return pub
    }

    /**
     * Sign [message] (typically the canonical string) with the device private
     * key. Returns the raw 64-byte Ed25519 signature.
     */
    fun sign(message: ByteArray): ByteArray {
        val privBytes = prefs.getString(KEY_PRIVATE_B64, null)
            ?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: error("Ed25519 key not generated; call generateIfMissing() first")
        val privateKey = Ed25519PrivateKeyParameters(privBytes, 0)
        val signer = org.bouncycastle.crypto.signers.Ed25519Signer()
        signer.init(true, privateKey)
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    /**
     * Erase the device key. Use during a "reset trusted device" flow — the
     * caller MUST also re-run register + verify-attestation after wiping.
     */
    fun reset() {
        prefs.edit()
            .remove(KEY_PRIVATE_B64)
            .remove(KEY_PUBLIC_B64)
            .apply()
    }

    private companion object {
        const val PREF_FILE = "faster_sos_device_keys"
        const val KEY_PRIVATE_B64 = "ed25519_private_b64"
        const val KEY_PUBLIC_B64 = "ed25519_public_b64"
    }
}
